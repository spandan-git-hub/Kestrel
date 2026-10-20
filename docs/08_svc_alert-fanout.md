# 08 — alert-fanout (MVP) · Lambda `kestrel-alert-fanout` · DynamoDB prefix `kestrel_fanout_`

Tasks **P1-25 … P1-35**, **P3-26** (email). Quiet for weeks, then thousands of sends at once → scaled independently by Lambda concurrency. Must keep working when every other service (even registry) is down.

## 1. Responsibilities
Consume `AlertEvent` from SQS; apply auto-send policy; plan recipients from a **local DynamoDB subscriber replica**; render localized messages with provenance wording; enqueue per-channel `DeliveryTask`s to SQS delivery queues; run channel worker Lambdas within channel limits; record receipts in DynamoDB; serve manual **broadcast** preview/send; expose history & queue status.
Not responsible for: deciding severity, composing CAP, storing subscriber truth.

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|---|
| `kestrel_fanout_subscriber_replica` | `subscriptionId` | — | version, status, language, minSeverity, locationIds (list), hazards (list), updatedAt |
| `kestrel_fanout_subscriber_channel` | `subscriptionId` | `channel` | status, address (sensitive: never log) |
| `kestrel_fanout_location_replica` | `locationId` | — | code, name, nameLocal (map), version |
| `kestrel_fanout_alert_dispatch` | `alertId` | `dispatchId` | stage, locationId, hazard, severity, msgType, provenance, autoSend, decidedAt, plannedAt, recipientCount, status |
| `kestrel_fanout_broadcast` | `broadcastId` | — | idempotencyKey (GSI), createdBy, locationIds, hazard, severity, provenance, languages, channels, coordinatorNote, recipientCount, estimatedSeconds, createdAt, status, triggerAlertId |
| `kestrel_fanout_delivery` | `deliveryId` | — | alertDispatchId?, broadcastId?, subscriptionId, channel, language, status, attempts, queuedAt, sentAt, errorCode; GSI on `alertDispatchId`, `broadcastId` |
| `kestrel_fanout_message_template` | `key#language` | — | body, needsReview |
| `kestrel_fanout_dedupe` | `dedupKey` (subscriptionId#locationId#hazard#severity) | — | ttl (cooldown duration) |
| `kestrel_fanout_tg_bucket` | `TG_BUCKET` | — | tokens, lastRefillAt (DynamoDB conditional update for rate limit) |
| `kestrel_fanout_outbox` | `eventId` | — | topicSuffix, payload, createdAt, publishedAt (TTL), attempts |
| `kestrel_fanout_inbox` | `eventId` | `consumerName` | processedAt, ttl |

Partition/prune `kestrel_fanout_delivery` items older than 30 days via DynamoDB TTL on `sentAt + 30d`. Bulk inserts with DynamoDB `BatchWriteItem` (≥25 items/batch, SDK handles chunking).

## 3. Replica sync
- SQS `kestrel-fanout-registry` (subscribed to SNS `kestrel-registry`): `SubscriptionChanged`, `LocationUpserted/Deleted` → Lambda `kestrel-fanout-registry-sync` → apply only if `version` > stored (DynamoDB conditional update); `DELETED` → remove rows.
- EventBridge Scheduler every 10 min triggers `kestrel-fanout-reconcile` Lambda: `GET /internal/v1/subscriptions/changes?cursor=` (circuit-breaker; skip silently when registry down).
- Metric `kestrel.fanout.replica_lag_versions`.

## 4. Alert handling pipeline
1. SQS `kestrel-fanout-alerts` (binding from `FANOUT_INPUT_STAGE` env var) triggers `kestrel-alert-fanout` Lambda. DynamoDB inbox idempotency by `eventId`.
2. **Auto-send policy**: dispatch immediately iff `autoSend=true`; else `HELD_FOR_APPROVAL` (dashboard shows it; coordinator can approve → creates broadcast from alert). `CANCEL` messages are sent as "all clear" only to subscribers who received the original alert (look up `kestrel_fanout_delivery` by `alertDispatchId`).
3. **Recipient planning**: subscriptions with `status=ACTIVE`, location ∈ `locationIds`, hazard ∈ `hazards`, `severity ≥ minSeverity`, channel ACTIVE. DynamoDB query on `kestrel_fanout_subscriber_replica` (filter on locationIds list, hazards list) in paginated batches.
4. **Dedupe**: DynamoDB conditional put on `kestrel_fanout_dedupe` (PK=`subscriptionId#locationId#hazard#severity`, TTL=cooldown 12 h; bypassed when severity escalates or msgType=CANCEL).
5. **Quiet hours** (config `FANOUT_QUIET_HOURS_START/END` default 22:00–06:00 `Asia/Kolkata`): non-SEVERE alerts planned inside quiet hours are held until window ends via EventBridge Scheduler delayed trigger; SEVERE always immediate. Heat advisories use the **early-morning window** (`FANOUT_HEAT_WINDOW` default 06:00–08:30 local).
6. **Render** message per language (§5) → `DeliveryTask{deliveryId, channel, subscriptionId, address-ref, text/title/body, url, priority}`; send in batches to SQS delivery queues. SEVERE messages sent first (submit to SQS in severity order; separate queue per channel ensures independent scaling).
7. Update `kestrel_fanout_alert_dispatch.status` in DynamoDB; record `recipientCount`; metrics TTFD/TTLD.

## 5. Message rendering (MVP: flood only; template keys generic)
Templates bundled as `messages_{en,hi,mr}.properties` (+ optional DynamoDB override in `kestrel_fanout_message_template`). **Hindi/Marathi must be drafted but flagged `# NEEDS NATIVE REVIEW` and `needsReview=true`** (`⚠ VERIFY-18`). Every message MUST contain:
1. hazard + **severity word** (icon + text);
2. location name (localized);
3. validity ("until 22 Oct, 18:00" in the location timezone);
4. **one plain-language action** (below);
5. **provenance line** (doc 03 §7) — mandatory, not removable by config;
6. link to resident status page `https://app.<domain>/s/{locationCode}` and unsubscribe hint.
Length: Telegram ≤ 700 chars; web-push title ≤ 50, body ≤ 140 chars; email full.

Default English action text (flood): WATCH/WARNING/SEVERE/CANCEL — same wording as original spec. `OFFICIAL_CONFIRMED`: replace provenance line with "Official alert from {authority} via SACHET." WhatsApp share text (coordinator tool) = same short body + provenance line + link.

## 6. Channel workers (separate Lambda per channel)
Each channel: **separate Lambda function** (`kestrel-fanout-telegram`, `kestrel-fanout-web-push`, `kestrel-fanout-email`, `kestrel-fanout-mock`) triggered by its own SQS queue. Lambda concurrency configurable (`reservedConcurrency`); SQS `batchSize` configurable.
- **TELEGRAM**: `sendMessage` via HTTP client; **DynamoDB token bucket** (`kestrel_fanout_tg_bucket` item, atomic conditional update refilling 25/s) shared across Lambda invocations — *keep below the 30 msg/s free limit* (`⚠ VERIFY-10`). On HTTP 429: honor `retry_after`, return SQS message to queue (DLQ visibility extended); severe-first ordering preserved by submission order. On 403/blocked: mark `subscriber_channel_replica.status=BLOCKED` in DynamoDB, emit `DeliveryReceipt(BLOCKED)`.
- **WEB_PUSH**: `nl.martijndwars:web-push` (verify) with VAPID keys from SSM; on 404/410 → mark BLOCKED in DynamoDB.
- **EMAIL** (P3-26): SMTP via Spring Mail; plain-text+HTML; digest for coordinators.
- **MOCK**: configurable latency (`MOCK_LATENCY_MS`) and failure rate; no external calls; enabled via env var `MOCK_ENABLED=true`.
Receipts → `DeliveryReceipt` events via DynamoDB outbox → SNS `kestrel-receipts` (batched) + DynamoDB `delivery` row updates in `BatchWriteItem` (buffer & flush every 250 ms / 25 rows).

## 7. Broadcast API (manual, one tap)
| Endpoint | Notes |
|---|---|
| `POST /api/v1/broadcasts/preview` | body `{locationIds[], hazard, severity, provenance?, coordinatorNote?, channels[], fromAlertId?}` → `{previews:[{language, channel, text}], recipientCounts:{channel→n, language→n}, totalRecipients, estimatedSeconds, whatsappShareText:{lang→text}, warnings[]}` |
| `POST /api/v1/broadcasts` | header `Idempotency-Key` **required**; re-send same key → returns original. Persists `kestrel_fanout_broadcast`, plans, enqueues to SQS; returns `202 {broadcastId}` |
| `GET /api/v1/broadcasts?page=` / `GET /api/v1/broadcasts/{id}` | history with per-channel delivery counts (DynamoDB query on `kestrel_fanout_delivery`) |
| `GET /api/v1/deliveries/summary?alertId=\|broadcastId=` | counts by status/channel + TTFD/TTLD |
| `GET /api/v1/alerts?status=HELD_FOR_APPROVAL\|…` | alert dispatches (history, held items) |
| `POST /api/v1/alerts/{alertDispatchId}/approve` | releases a held alert (COORDINATOR in scope) |
| `GET /api/v1/fanout/queue-status` | SQS queue depth per channel (`GetQueueAttributes`), DynamoDB token bucket level, replica lag |

Coordinator scope: only `locationIds` in Cognito JWT. Broadcast preview is **mandatory before send** (server stores preview hash in DynamoDB TTL 15 min; send must reference `previewId`).

## 8. Config (SSM Parameter Store / environment)
`FANOUT_INPUT_STAGE` (`DECISION` MVP) · `FANOUT_AUTO_SEND_MIN_SEVERITY` · `FANOUT_DEDUPE_COOLDOWN` · `FANOUT_QUIET_HOURS_*` · `FANOUT_HEAT_WINDOW_*` · `TELEGRAM_BOT_TOKEN`, `TELEGRAM_RATE_PER_SECOND`, `TELEGRAM_BURST` · `VAPID_*` · `MOCK_ENABLED`, `MOCK_LATENCY_MS` · `FANOUT_RECONCILE_CRON` · `TABLE_PREFIX`, `SNS_TOPIC_ARN_RECEIPTS`, `SQS_DELIVERY_TELEGRAM_URL`, `SQS_DELIVERY_WEB_PUSH_URL`, `SQS_DELIVERY_EMAIL_URL`, `SQS_DELIVERY_MOCK_URL`.

## 9. Metrics (CloudWatch)
`kestrel.fanout.deliveries.total{channel,status}`, `kestrel.fanout.queue_depth{channel}` (from SQS `ApproximateNumberOfMessages`), `kestrel.fanout.time_to_first_delivery_seconds`, `…to_last…`, `kestrel.fanout.tg_tokens_available`, `kestrel.fanout.replica_lag_versions`, `kestrel.fanout.held_alerts`.

## 10. Tests
Unit: recipient filter logic, severity-priority mapping, template rendering (all langs/severities incl. provenance line always present), quiet-hours logic, estimated seconds. LocalStack IT (DynamoDB + SQS + WireMock Telegram): DynamoDB token bucket never exceeds configured rate (assert via timestamps); 429 handling; 403 → BLOCKED; dedupe on redelivered event; idempotent broadcast; severe-before-watch ordering with mixed queue; replica version ordering; unsubscribe propagates and suppresses unsent tasks; registry down → still sends from DynamoDB replica. **Load IT (MOCK_ENABLED=true):** 50,000 MOCK subscribers → record TTFD/TTLD, assert zero duplicate deliveries and zero loss.

## 11. Task breakdown
P1-25 DynamoDB tables + replica sync/reconcile · P1-26 planner + dedupe + auto-send/held · P1-27 templates + renderer · P1-28 SQS delivery queues + batching writer · P1-29 Telegram worker Lambda + DynamoDB token bucket · P1-30 Web Push worker Lambda · P1-31 Mock sender Lambda + metrics · P1-32 broadcast preview/send + idempotency · P1-33 history/queue-status/approve APIs · P1-34 retries/DLQ/backoff/receipts · P1-35 tests green + README/Lambda handlers · P3-26 Email channel Lambda.

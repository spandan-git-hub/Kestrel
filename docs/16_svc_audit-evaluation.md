# 16 — audit-evaluation (Phase 3) · Lambda `kestrel-audit-evaluation` · DynamoDB prefix `kestrel_audit_`

Tasks **P3-20 … P3-25**. Analysis must not slow alerting → **read-only taps** on the bus, separate DB schema, never on the critical path. First candidate to merge if memory is short.

## 1. Responsibilities
Immutable audit log of every signal/decision/alert/delivery with its inputs; alert **trace** view; **backtests** (detection rate, false-alarm ratio by lead time); operational KPIs (time threshold→broadcast, delivery stats); user **feedback** ("was this useful").

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|
| `kestrel_audit_event` | `eventId` | `occurredAt` | eventType, producer, locationId, hazard, alertId, severity, provenance, payloadJson (stripped of PII), receivedAt; GSI on `alertId`; GSI on `locationId#hazard, occurredAt` |
| `kestrel_audit_observed_event` | `observedEventId` | — | locationId, hazard, onset, end, severityHint, source, note, importedAt |
| `kestrel_audit_backtest_run` | `runId` | — | params (map), startedAt, finishedAt, status |
| `kestrel_audit_backtest_result` | `runId` | `hazard#leadBucket#severityMin` | hits, misses, falseAlarms, pod, far, csi, nAlerts, nEvents |
| `kestrel_audit_feedback` | `feedbackId` | — | alertId, subscriptionHash, useful, comment, createdAt, ttl (retentionDays) |
| `kestrel_audit_outbox` | `eventId` | — | topicSuffix, payload, createdAt, publishedAt (TTL), attempts |
| `kestrel_audit_inbox` | `eventId` | `consumerName` | processedAt, ttl |

Retention: DynamoDB TTL on `kestrel_audit_event` after `AUDIT_RETENTION_DAYS` (default 365); aggregated results kept indefinitely.

## 3. Taps (SQS queues, separate Lambda trigger per tap)
SQS queues `kestrel-audit-signals` (subscribed to SNS `kestrel-signals`, all), `kestrel-audit-alerts` (SNS `kestrel-alerts`, all), `kestrel-audit-receipts` (SNS `kestrel-receipts`, all) — **separate Lambda functions** per tap (`kestrel-audit-signals-tap`, `kestrel-audit-alerts-tap`, `kestrel-audit-receipts-tap`), lenient: failures log + DLQ but never requeue forever; batch inserts with DynamoDB `BatchWriteItem`. Strip secrets/addresses (e.g. drop `address` fields from `SubscriptionChanged`) before storing.

## 4. Backtests (measured, not promised)
Input: `observed_event` rows (CSV import: `locationCode,hazard,onset,end,note`) and stored `AlertEvent(DECISION/FUSED)` history.
- **Hit**: an observed event with an alert of severity ≥ `severityMin` (default WATCH) at the location/hazard issued **before onset** (and within `maxLeadDays`, default 7).
- **Miss**: observed event with no such alert. **False alarm**: alert (≥ severityMin) with no observed event within `[validFrom, validUntil + grace]`.
- `POD = hits/(hits+misses)`; `FAR = falseAlarms/(hits+falseAlarms)`; `CSI = hits/(hits+misses+falseAlarms)`.
- **Lead buckets** (hours before onset): 0–24, 24–48, 48–72, 72–120, 120–168. Report per hazard × bucket × severityMin.
- Guard rails: show `n` for every cell; suppress rates when n < 5 ("insufficient sample"); results page carries the caveat from source docs: don't claim more than the underlying model delivers.
- **Hindcast option (`⚠`):** historical *forecasts* may be unavailable from free APIs; the default backtest uses **our own logged alerts** plus imported observed events; an optional reanalysis-based proxy (does reanalysis discharge exceed thresholds on event dates?) can be offered but **must be labelled as a threshold-validity check, not a forecast skill score**. Check Open-Meteo for historical-forecast/previous-runs data before promising anything.
- Run async (job), results via API.

## 5. KPIs
Time from first threshold crossing (first `FloodSignal` supporting the episode) → decision → first/last delivery; deliveries by channel/status; feed uptime; held-for-approval latency; false-alarm ratio over rolling window; feedback usefulness rate.

## 6. REST
`GET /api/v1/audit/events?locationId=&hazard=&type=&from=&to=` · `GET /api/v1/audit/alerts/{alertId}/trace` (ordered chain: signals → decision → fused → composed → broadcast/deliveries summary) · `POST /api/v1/audit/observed-events/import` (CSV, ADMIN) · `POST /api/v1/audit/backtests` (ADMIN) · `GET /api/v1/audit/backtests/{id}` · `GET /api/v1/audit/kpis?from=&to=` · public `POST /api/v1/feedback {alertId, useful, comment?}` (rate-limited, no PII, `subscription_hash` derived from token or null).

## 7. Tests
Tap resilience (audit DB down ⇒ other services unaffected, messages DLQ'd after bounded retries); trace ordering; backtest math on hand-built scenarios (hits/misses/false alarms, lead-bucket boundaries, n<5 suppression); CSV import validation; feedback rate limit; secret-stripping test.

## 8. Task breakdown
P3-20 DynamoDB tables + SQS tap Lambda triggers + DynamoDB batching writer · P3-21 trace/events APIs · P3-22 observed-event CSV import + backtest engine (pure domain) · P3-23 EventBridge backtest job/APIs · P3-24 KPIs + feedback · P3-25 tests/README/Lambda/SAM.

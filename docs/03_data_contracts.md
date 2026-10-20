# 03 — Data Contracts (events, messaging topology, DynamoDB ownership, REST rules)

**Changing anything here requires a Decision Log entry.** Additive changes only; bump `schemaVersion` when semantics change.

## 1. Event envelope (all events)
```json
{
  "eventId": "uuid-v4",
  "eventType": "FloodSignal",
  "schemaVersion": 1,
  "occurredAt": "2026-10-20T06:30:00Z",
  "producer": "flood-ingest",
  "correlationId": "uuid-v4",
  "payload": { }
}
```
SNS message attribute `x-event-type` and `x-schema-version` are set as string attributes; `content-type: application/json` in message body.

## 2. SNS Topics, SQS Queues

SNS topics (declared in SAM/CDK `infra/`; all standard, not FIFO — FIFO is not needed for at-least-once delivery with idempotent consumers):

| SNS Topic | Routing attribute `x-event-type` values |
|---|---|
| `kestrel-registry` | `LocationUpserted`, `LocationDeleted`, `SubscriptionChanged` |
| `kestrel-signals` | `FloodSignal`, `HeatSignal`, `AqSignal`, `OfficialAlertSignal`, `ReportAccepted`, `ReportUpdated`, `FeedStatus` |
| `kestrel-alerts` | `AlertEvent` (differentiated by `stage` field: `DECISION`, `FUSED`, `COMPOSED`; and `hazard` field) |
| `kestrel-receipts` | `DeliveryReceipt`, `BroadcastCreated` |

SQS queues (declared by the **consuming** service's SAM/CDK template; all standard, with DLQ; subscribed to the appropriate SNS topic with a filter policy):

| SQS Queue | Owner | SNS subscription filter | Notes |
|---|---|---|---|
| `kestrel-rules-flood` | rules-engine | `kestrel-signals`, `x-event-type=FloodSignal` | **separate queue + Lambda trigger** (isolation) |
| `kestrel-rules-heat` | rules-engine | `kestrel-signals`, `x-event-type=HeatSignal` | **separate queue + Lambda trigger** |
| `kestrel-rules-aq` | rules-engine | `kestrel-signals`, `x-event-type=AqSignal` | **separate queue + Lambda trigger** |
| `kestrel-fanout-alerts` | alert-fanout | `kestrel-alerts`, stage=`DECISION` (MVP), `FUSED` (P2), `COMPOSED` (P3) | binding is a **config property** (`fanout.input-stage`) |
| `kestrel-fanout-registry` | alert-fanout | `kestrel-registry`, all | replica feed |
| `kestrel-delivery-telegram` | alert-fanout | — (internal, not SNS-subscribed) | direct SQS send from fanout planner |
| `kestrel-delivery-web-push` | alert-fanout | — | direct |
| `kestrel-delivery-email` | alert-fanout | — | direct |
| `kestrel-delivery-mock` | alert-fanout | — | direct |
| `kestrel-<svc>-registry` | each ingest/report service | `kestrel-registry`, `LocationUpserted/Deleted` | e.g. `kestrel-flood-ingest-registry` |
| `kestrel-fusion-decision` | signal-fusion | `kestrel-alerts`, stage=`DECISION` | |
| `kestrel-fusion-report` | signal-fusion | `kestrel-signals`, `x-event-type=ReportAccepted/ReportUpdated` | |
| `kestrel-fusion-official` | signal-fusion | `kestrel-signals`, `x-event-type=OfficialAlertSignal` | |
| `kestrel-composer-fused` | alert-composer | `kestrel-alerts`, stage=`FUSED` | |
| `kestrel-audit-signals` | audit-evaluation | `kestrel-signals`, all | read-only taps; DLQ failures must never affect others |
| `kestrel-audit-alerts` | audit-evaluation | `kestrel-alerts`, all | |
| `kestrel-audit-receipts` | audit-evaluation | `kestrel-receipts`, all | |

Priority simulation: SQS standard queues don't natively support priority; alert-fanout uses **separate queues per channel** plus delivers SEVERE alerts first by submitting them before WATCH/WARNING to the delivery queue (`kestrel-delivery-*`). The Lambda trigger processes messages in FIFO order per batch.

## 3. Payload definitions

Conventions: `locationId` = UUID; fractions are `0..1`; instants are UTC ISO strings; `hazard` ∈ `FLOOD|HEAT|AIR_QUALITY`.

### 3.1 `FloodSignal` — SNS `kestrel-signals`, x-event-type=`FloodSignal`
```json
{
  "locationId": "uuid", "runId": "uuid",
  "fetchedAt": "2026-10-20T06:30:00Z", "dataAsOf": "2026-10-20T00:00:00Z",
  "memberCount": 50, "snapStatus": "VERIFIED",
  "thresholds": { "rp2": 812.4, "rp5": 1204.9, "rp20": 1890.1, "unit": "m3/s", "source": "GUMBEL_FIT", "modelVersion": "glofas_v4" },
  "days": [
    { "date": "2026-10-21", "min": 300.2, "median": 410.0, "max": 901.7, "share2": 0.04, "share5": 0.0, "share20": 0.0 }
  ]
}
```
`days` limited to the alert horizon (default 10 days). Shares = fraction of ensemble members strictly above the threshold.

### 3.2 `HeatSignal` — `HeatSignal`
```json
{ "locationId":"uuid","runId":"uuid","fetchedAt":"…","dataAsOf":"…","regionType":"PLAINS",
  "days":[{"date":"2026-04-22","tmax":44.1,"apparentMax":49.3,"humidityMax":38,"normalTmax":39.2,"departure":4.9}] }
```

### 3.3 `AqSignal` — `AqSignal`
```json
{ "locationId":"uuid","runId":"uuid","fetchedAt":"…","dataAsOf":"…","resolutionKm":45,
  "current":{ "pm25_24h":98.2,"pm10_24h":181.0,"no2_24h":42.0,"so2_24h":9.0,"o3_8h":35.0,"co_8h_mgm3":0.9,"validHours":24 },
  "forecastPeaks":[{"date":"2026-11-02","pm25_24h":142.0,"pm10_24h":240.0,"no2_24h":55.0,"so2_24h":12.0,"o3_8h":40.0,"co_8h_mgm3":1.1}],
  "observed": { "stationId":"…","distanceKm":8.4,"measuredAt":"…","pm25":110.0,"pm10":null,"source":"OPENAQ" } }
```
`observed` optional. CO already converted to **mg/m³** (`⚠ VERIFY-04/05`).

### 3.4 `OfficialAlertSignal` — `OfficialAlertSignal`
```json
{ "officialAlertId":"uuid","capIdentifier":"string","sender":"string","msgType":"ALERT|UPDATE|CANCEL",
  "sentAt":"…","effective":"…","expires":"…","event":"string","capSeverity":"Severe","capUrgency":"Expected","capCertainty":"Likely",
  "hazard":"FLOOD|HEAT|AIR_QUALITY|OTHER","mappedSeverity":"WARNING","locationIds":["uuid"],
  "headline":"…","instruction":"…","sourceUrl":"https://…","references":["capIdentifier"] }
```

### 3.5 `ReportAccepted` / `ReportUpdated` — `ReportAccepted` / `ReportUpdated`
```json
{ "reportId":"uuid","category":"WATER_RISING","status":"PENDING|VERIFIED|REJECTED","locationId":"uuid",
  "latitude":18.9912,"longitude":75.7601,"observedAt":"…","receivedAt":"…","waterLevel":"KNEE","note":"…(≤280)","reporterKey":"sha256-hash" }
```
Coordinates rounded to 3 decimals (~110 m). `reporterKey` is a hash — never raw IP/device ID.

### 3.6 `AlertEvent` — SNS `kestrel-alerts`
```json
{
  "alertId": "uuid", "stage": "DECISION|FUSED|COMPOSED",
  "locationId": "uuid", "hazard": "FLOOD",
  "severity": "WARNING", "previousSeverity": "WATCH", "msgType": "ALERT|UPDATE|CANCEL",
  "provenance": "MODEL_ONLY",
  "validFrom": "…", "validUntil": "…",
  "dataAsOf": "…", "sourceRunIds": ["uuid"],
  "metrics": { "share20": 0.42, "firstExceedanceDate": "2026-10-23", "threshold": "rp5" },
  "evidence": { "reportIds": [], "officialAlertIds": [] },
  "autoSend": true,
  "content": null,
  "cap": null
}
```
`content` (optional, P3): `{ "headline": {"en":"…","hi":"…","mr":"…"}, "instruction": {…}, "short": {…} }`. `cap` (optional, P3): `{ "identifier","event","urgency","severity","certainty","areaDesc" }`. Consumers **must ignore unknown fields**.
`alertId` is stable across `UPDATE`/`CANCEL` of the same alert episode.

### 3.7 Registry events — SNS `kestrel-registry`
`LocationUpserted`: `{ locationId, code, name, nameLocal:{en,hi,mr}, state, district, latitude, longitude, regionType, timezone, hazardsEnabled:[Hazard], active, version }`.
`LocationDeleted`: `{ locationId, version }`.
`SubscriptionChanged`:
```json
{ "subscriptionId":"uuid","version":17,"status":"ACTIVE|PAUSED|DELETED","language":"hi","minSeverity":"WARNING",
  "locationIds":["uuid"],"hazards":["FLOOD"],
  "channels":[{"channel":"TELEGRAM","status":"ACTIVE|PENDING|BLOCKED","address":"chat-id-or-push-json"}] }
```
Contains addresses → SNS topic is internal/trusted (not public); **never log payloads**.

### 3.8 `DeliveryReceipt` — SNS `kestrel-receipts`
`{ deliveryId, alertId?, broadcastId?, subscriptionId, channel, status: SENT|FAILED|SUPPRESSED|BLOCKED, attempt, sentAt, latencyMs, errorCode? }`

### 3.9 `BroadcastCreated` — SNS `kestrel-receipts`
`{ broadcastId, createdBy, locationIds, hazard, severity, provenance, languages, channels, recipientCount, createdAt, triggerAlertId? }`

### 3.10 `FeedStatus` — SNS `kestrel-signals`
`{ feed: "OPEN_METEO_FLOOD|OPEN_METEO_WEATHER|OPEN_METEO_AQ|OPENAQ|SACHET", state, lastSuccessAt, lastErrorAt, consecutiveFailures, dataAsOf, message }` — published on state change and every 10 min as heartbeat (EventBridge-triggered heartbeat Lambda or inline in ingest Lambda warm run).

## 4. DynamoDB ownership
One DynamoDB account, tables prefixed per service `kestrel_<service>_<entity>`. Each service's IAM role has rights **only** on its own tables. `infra/sam-template.yaml` (or `infra/cdk/`) creates all tables and IAM policies. No cross-service DynamoDB access. DynamoDB item keys:
- Use `PK` (partition key) + optional `SK` (sort key) with single-table or multi-table design per service.
- All TTL attributes named `ttl` (Unix epoch seconds).
- For event sourcing/outbox: `PK=eventId`, `SK=consumerName` on inbox table.

DynamoDB key patterns per service:
- `kestrel_flood_ingest_snapshot`: `PK=locationId`, `SK=runId`
- `kestrel_rules_alert_state`: `PK=locationId#hazard`
- `kestrel_fanout_delivery`: `PK=deliveryId`
- `kestrel_registry_subscription`: `PK=subscriptionId`
- (full patterns documented in each service doc)

DynamoDB call budget keys: `PK=BUDGET#om#day#{yyyyMMdd}`, `PK=BUDGET#om#min#{epochMin}` (atomic increment with condition check).

## 5. REST conventions
- Base paths: public/coordinator `/api/v1/<domain>/…`; internal `/internal/v1/…` (not routed by CloudFront to the public internet — blocked by CloudFront behaviour; Lambda-to-Lambda via direct invocation or VPC if needed).
- Domain path prefixes (CloudFront behaviour/origin table): `auth, locations, subscriptions, coordinators, subscribers, push` → registry · `flood` → flood-ingest · `status, rules` → rules-engine · `broadcasts, deliveries, fanout, alerts` → alert-fanout · `official` → cap-ingest · `reports` → report-intake · `fusion` → signal-fusion · `heat` → heat-ingest · `aq` → aq-ingest · `cap` → alert-composer · `audit, feedback` → audit-evaluation.
- Errors: `application/problem+json` `{type,title,status,detail,instance,traceId, errors?:[{field,message}]}`.
- Auth: Cognito Bearer JWT (coordinator/admin); `X-Subscriber-Token` (resident); public endpoints explicitly allow-listed.
- Pagination `?page=0&size=20` (max 100); sort `?sort=field,desc`.
- `ETag/If-None-Match` supported on list endpoints used by polling dashboards (cheap 304s on slow links).
- Rate limits: public write endpoints (subscribe, report, feedback) limited per IP + per token via DynamoDB counter + TTL.
- CORS: only the configured frontend origins (Amplify/Cloudflare Pages domain); credentials allowed for refresh cookie.

## 6. Severity & mapping tables (adaptations — configurable, log as D-09)

| Hazard | WATCH | WARNING | SEVERE |
|---|---|---|---|
| FLOOD | ≥30% members > 2-yr | ≥30% > 5-yr | ≥30% > 20-yr |
| HEAT | heat-wave criteria met 1 day | met 2 consecutive days | *severe* criteria met 2 consecutive days |
| AIR_QUALITY | CPCB `Poor` (201–300) | `Very Poor` (301–400) | `Severe` (401–500) |

(GloFAS's "≥30% of members exceed" signal rule is adopted; the watch/warning/severe mapping is **our adaptation**, not an external standard.)

CAP mapping (defaults; composer): WATCH→`severity=Moderate, urgency=Future, certainty=Possible`; WARNING→`Severe, Expected, Likely`; SEVERE→`Extreme, Immediate, Likely`. `OFFICIAL_CONFIRMED` raises certainty one step (Likely→Observed only if official says so).
Inbound CAP severity → ours: `Extreme→SEVERE, Severe→WARNING, Moderate→WATCH, Minor→WATCH, Unknown→WATCH`.

## 7. Provenance wording (used by fanout/composer/UI — keys, not literals)
| Provenance | English default |
|---|---|
| MODEL_ONLY | "Model forecast guidance – not an official warning. Please confirm locally." |
| MODEL_PLUS_REPORTS | "Model forecast + community reports – not an official warning. Please confirm locally." |
| OFFICIAL_CONFIRMED | "Official alert from {authority} via SACHET." |

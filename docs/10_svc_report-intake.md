# 10 — report-intake (Phase 2) · Lambda `kestrel-report-intake` · DynamoDB prefix `kestrel_report_`

Tasks **P2-06 … P2-10**. The ground-truth input no API provides. Public-facing and unpredictable → kept **away from the alert path**; scales independently.

## 1. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|
| `kestrel_report_report` | `reportId` | — | category, status, locationId, latitude, longitude, waterLevel, note, observedAt, receivedAt, reporterKey, moderatedBy, moderatedAt, rejectReason, idempotencyKey (GSI), ttl (retentionDays) |
| `kestrel_report_location_replica` | `locationId` | — | code, name, lat, lon, version |
| `kestrel_report_rate_limit` | `rateLimitKey` (IP or reporterKey) | `window` | count, ttl |
| `kestrel_report_outbox` | `eventId` | — | topicSuffix, payload, createdAt, publishedAt (TTL), attempts |
| `kestrel_report_inbox` | `eventId` | `consumerName` | processedAt, ttl |

Coordinates **rounded to 3 decimals (~110 m)** before storage. No photos, no names, no phone numbers (data minimization). Retention: DynamoDB TTL on `reportId` after `REPORT_RETENTION_DAYS` (default 90).

## 2. REST
| Endpoint | Auth | Notes |
|---|---|---|
| `POST /api/v1/reports` | public (+optional `X-Subscriber-Token`) | `{category, note?, waterLevel?, latitude, longitude, observedAt?}` + `Idempotency-Key`; returns `202 {reportId, status:"PENDING"}` |
| `GET /api/v1/reports?locationId=&status=&since=&category=` | COORDINATOR (scope) | inbox |
| `GET /api/v1/reports/{id}` | COORDINATOR | |
| `PATCH /api/v1/reports/{id}/status` | COORDINATOR | `{status:VERIFIED|REJECTED, reason?}` → publishes `report.updated` |
| `GET /api/v1/reports/summary?locationId=` | COORDINATOR | counts by category/status last 6/24 h |
| `GET /api/v1/reports/public/recent-count?locationId=` | public | count only (no content) for resident page "N reports nearby"; cached |

## 3. Validation & anti-abuse
- Coordinates within India bounds **and** within `report.maxDistanceKm` (default 25) of a monitored location; assign nearest `locationId` (haversine, replica).
- `observedAt` not in future, not older than 6 h (config).
- Note ≤ 280 chars, stripped of control chars/HTML; basic blocklist; no URLs allowed (config).
- **Reporter key** = SHA-256(salt + subscriberToken or deviceId or IP+UA). Limits (DynamoDB counters + TTL): 5 reports/hour/key, 20/hour/IP, 1 identical (category+geohash5) per 10 min; excess → `429` with `Retry-After`.
- Honeypot field + minimal proof-of-work optional (config) for bot resistance.
- `NEED_HELP` reports are flagged `priority=true` for the coordinator inbox; **never auto-broadcast**; UI shows "contact local authorities / emergency number" guidance (never promise response).
- Rejected/spam reporters: exponential cooldown per key.

## 4. Events
Publishes `report.accepted` (after validation, status PENDING) and `report.updated` (moderation) via DynamoDB outbox → SNS `kestrel-signals`. Consumes SQS `kestrel-report-intake-registry` for location replica.

## 5. Metrics
`kestrel.reports.total{category,outcome(accepted|rate_limited|invalid)}`, `kestrel.reports.pending`.

## 6. Tests
Validation matrix (bounds, distance, future time, long note, URL in note); rate limits (LocalStack DynamoDB IT); idempotency; moderation flow + event; scope enforcement; PII hygiene test (no raw IP/UA stored); load test 200 req/s burst does not impact other services (separate Lambda, DynamoDB table).

## 7. Task breakdown
P2-06 DynamoDB tables + replica + validation · P2-07 POST/idempotency + reporter key + DynamoDB rate limits · P2-08 inbox/summary/moderation APIs · P2-09 SNS events + public count · P2-10 tests (LocalStack) + README/Lambda/SAM.

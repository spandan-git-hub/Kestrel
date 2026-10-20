# 06 — flood-ingest (MVP) · Lambda `kestrel-flood-ingest` · DynamoDB prefix `kestrel_flood_`

Tasks **P1-11 … P1-18**. The critical path: must keep working when every other feed is down. Single job: pull ensemble river-discharge forecasts, compute exceedance shares versus return-period thresholds, publish `FloodSignal` to SNS. Triggered by **EventBridge Scheduler** (not a long-running process).

## 1. Verify first (⚠ VERIFY-02, VERIFY-07, VERIFY-16)
Before coding, read the live Open-Meteo Flood API docs and record in the Verification Register: exact parameter names (`ensemble`, `daily=river_discharge…`, `forecast_days`, `past_days`, `start_date/end_date`), ensemble **member naming and count** (do not hard-code 50 — count members present; docs mention 50, GloFAS examples use 51), history range (source docs say reanalysis 1984 to ~July 2022), multi-location request syntax **and how calls are counted**, free-tier limits (source docs: 600/min, 10,000/day, 300,000/month, non-commercial, attribution, no uptime guarantee), GloFAS model version used by the API (docs mention v4).

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|---|
| `kestrel_flood_monitored_point` | `locationId` | — | code, name, lat, lon, snappedLat, snappedLon, snapStatus, expectedRiverHint, active, locationVersion, updatedAt |
| `kestrel_flood_threshold` | `locationId` | `returnPeriod` (2\|5\|20) | dischargeM3s, source, modelVersion, sampleYears, computedAt |
| `kestrel_flood_forecast_run` | `locationId` | `runId` | fetchedAt, dataAsOf, payloadHash, memberCount, horizonDays, status, error |
| `kestrel_flood_forecast_day` | `runId` | `date` | qMin, qP25, qMedian, qP75, qMax, qMean, share2, share5, share20 |
| `kestrel_flood_feed_state` | `feed` | — | state, lastSuccessAt, lastErrorAt, consecutiveFailures, lastError |
| `kestrel_flood_call_budget` | `budgetKey` | — | count, ttl (TTL for minute-level; no TTL for day-level — overwrite each day) |
| `kestrel_flood_outbox` | `eventId` | — | topicSuffix, routingKey, payload, createdAt, publishedAt (TTL 7d once published), attempts |
| `kestrel_flood_inbox` | `eventId` | `consumerName` | processedAt, ttl |

Retention: keep last 30 runs per point (nightly EventBridge-triggered cleanup Lambda or TTL on `forecast_run`). Latest run per point cached in DynamoDB item `kestrel_flood_cache#latest#{locationId}` with TTL 48 h.

## 3. Location replica
Consumes `LocationUpserted/Deleted` from SQS `kestrel-flood-ingest-registry` (SNS subscription) → upsert `kestrel_flood_monitored_point` (apply only if `locationVersion` higher, using DynamoDB conditional put). Bootstrap order at Lambda cold start: (1) existing DynamoDB table, (2) `GET /internal/v1/locations` (via direct Lambda invocation), (3) `infra/seed/locations.csv`. Only locations with `FLOOD ∈ hazardsEnabled && active` are monitored.

## 4. Threshold computation (once per point; recompute on request)
Strategy `AUTO` (D-08): use `GLOFAS_CSV` if a row exists for the point **and** its model version matches the API's; otherwise **fit from Open-Meteo's own history**.

**Gumbel fit (default):**
1. Fetch historical daily `river_discharge` for the snapped coordinates (1984 → last full year available; chunk requests, respect call budget). Use **calendar-year annual maxima**, only years with ≥ 90% valid days.
2. Method of moments: `β = s·√6/π`, `μ = mean − 0.5772·β` (s = sample std-dev, n−1).
3. Return level for period T: `x_T = μ − β·ln(−ln(1 − 1/T))` for T ∈ {2, 5, 20}.
4. Store with `source=GUMBEL_FIT`, `sampleYears`, `modelVersion` from API metadata if available.
5. Reject (status `INSUFFICIENT_HISTORY`, no alerts) if < 20 valid years or if `x_2 ≤ 0` or thresholds not strictly increasing.
**GloFAS CSV import (optional):** `POST /api/v1/flood/admin/thresholds/import` CSV `code,rp2,rp5,rp20,model_version` produced offline from the Copernicus EWDS raster. Document the offline sampling procedure in the service README. If `model_version` ≠ API's, store but mark `UNUSED_VERSION_MISMATCH`.
Unit tests: known synthetic annual-max series with hand-computed `x_2, x_5, x_20` (tolerance 1e-6).

## 5. Point snapping (admin tool)
Rivers: API returns the **largest river in the 5 km cell**; the wrong river can be chosen.
- `POST /api/v1/flood/admin/points/{locationId}/snap-candidates` → tests a grid of offsets (default ±0.1° in 0.05° steps = 25 candidates, **budget-guarded**, max 25 calls) using a short history window; returns per-candidate `{lat,lon,meanAnnualMax, maxDischarge}`.
- `PUT …/snap {lat,lon,verified:true}` sets `snapped_*`, `snapStatus=VERIFIED` in DynamoDB; clears thresholds for recompute.
- **Only `VERIFIED` points generate alerts.** `UNSNAPPED/CANDIDATE` points are fetched for display but `FloodSignal.snapStatus` tells rules-engine to cap output at no alert (rules-engine enforces; dashboard shows "location not verified").

## 6. EventBridge-triggered run
- EventBridge Scheduler cron (configurable, default once daily `0 30 6 ? * * *` UTC, plus on-startup run if last success > 24 h checked at Lambda cold start).
- Lambda invocation → batch locations (config `BATCH_SIZE`, default 20) per Open-Meteo request using the multi-location syntax (⚠ VERIFY-02); request `ensemble=true`, `daily` stats + members, `forecast_days` (default 30), horizon for signal = `ALERT_HORIZON_DAYS` (default 10).
- Skip if `payloadHash` equals latest DynamoDB run record (still update `lastSuccessAt`).
- **Exceedance share** per day *d*, threshold *k*: `share_k(d) = (# members with q_i(d) > T_k) / memberCount`. Worked test (from source docs): 25 of 51 members above the 20-yr level → `0.490` (≈49%).
- Also compute min/p25/median/p75/max/mean per day for the dashboard range chart.
- Persist run + days in DynamoDB batch write, **publish `FloodSignal` per point via outbox** in the same DynamoDB `TransactWriteItems`.
- Call budget via `CallBudget` (DynamoDB atomic counter; cap 8,000/day, 500/min). Over budget → skip lowest-priority (UNVERIFIED) points first.
- HTTP client: timeout 10 s, retries 3 w/ exponential backoff + jitter, circuit breaker (open after 50% failures over 10 calls, 60 s). On failure: `FeedState` → DEGRADED (≥1 failure) / DOWN (no success for > 36 h); **keep last-good snapshot in DynamoDB**; `dataAge` grows; no signal published for failed points (rules-engine uses staleness guard).
- Publish `FeedStatus` event on state change + every 10-min heartbeat (separate EventBridge rule triggers heartbeat Lambda or inline in main run).

## 7. REST API
| Path | Role | Notes |
|---|---|---|
| `GET /api/v1/flood/points` | COORDINATOR | list: `{locationId, name, snapStatus, latestRunAt, dataAgeSeconds, maxShare2/5/20, firstExceedanceDate(≥30% per level), thresholdsSource}` |
| `GET /api/v1/flood/points/{id}/forecast` | COORDINATOR | days w/ range + shares + thresholds + `dataAsOf` + `dataAgeSeconds` |
| `GET /api/v1/flood/public/points/{id}/summary` | public | trimmed (no raw members) for resident page: level hints + dataAge — rate-limited |
| `GET /api/v1/flood/feed-status` | COORDINATOR | FeedState from DynamoDB |
| `POST /api/v1/flood/admin/run-now?locationId=` | ADMIN | triggers Lambda synchronously |
| `POST …/points/{id}/snap-candidates`, `PUT …/snap`, `POST …/thresholds/recompute`, `POST …/thresholds/import` | ADMIN | |
Responses served from DynamoDB cache item; `ETag` supported.

## 8. Config (SSM Parameter Store / environment)
`OPENMETEO_BASE_URL`, `OPENMETEO_HISTORY_BASE_URL`, `FORECAST_DAYS`, `ALERT_HORIZON_DAYS`, `BATCH_SIZE`, `TIMEOUT_MS`, `SCHEDULE_CRON`, `BUDGET_PER_DAY`, `BUDGET_PER_MINUTE`, `THRESHOLD_STRATEGY`, `MIN_YEARS`, `HISTORY_START_YEAR`, `STALE_MAX_DATA_AGE`, `RETENTION_RUNS_PER_POINT`, `TABLE_PREFIX`, `SNS_TOPIC_ARN_SIGNALS`.
Attribution text: "Weather data by Open-Meteo.com" (⚠ VERIFY-16).

## 9. Metrics (CloudWatch)
Standard feed metrics (doc 04) with `feed=OPEN_METEO_FLOOD`, plus `kestrel.flood.points_monitored{snap_status}`, `kestrel.flood.run_duration_seconds`.

## 10. Tests
Unit (pure `domain`): exceedance share, Gumbel, classification inputs, hash dedupe. WireMock IT: normal run, partial failure, 429, timeout, malformed JSON, member-count variations (50 and 51), multi-location batch parsing. LocalStack IT: DynamoDB outbox publish exactly once, replica upsert ordering by version, retention job. **Resilience IT:** upstream down → last-good retained in DynamoDB, `dataAge` increases, feed DOWN after threshold, Lambda stays healthy.

## 11. Task breakdown
P1-11 DynamoDB tables + replica + seed bootstrap · P1-12 Open-Meteo client + call budget (DynamoDB) + resilience · P1-13 threshold computation (Gumbel + CSV import) · P1-14 snap tool · P1-15 EventBridge-triggered run + exceedance + snapshots · P1-16 `FloodSignal` SNS publish + feed state/heartbeat · P1-17 read APIs + DynamoDB caching · P1-18 tests/README/Lambda handler.

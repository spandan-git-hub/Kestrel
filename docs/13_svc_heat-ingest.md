# 13 — heat-ingest (Phase 3) · Lambda `kestrel-heat-ingest` · DynamoDB prefix `kestrel_heat_`

Tasks **P3-01 … P3-05**. Pulls temperature/humidity forecasts and maintains climatological **normals**. Different cadence and logic from flood → own service. **Judgement (IMD-style criteria) lives in rules-engine** (doc 07 §3.3); this service only prepares data.

## 1. Verify first (⚠ VERIFY-03, VERIFY-06, VERIFY-16)
Source docs flagged these as **not confirmed**: Open-Meteo weather variable names. Read the live Forecast + Historical (archive) docs and record: `daily=temperature_2m_max,apparent_temperature_max` and the exact humidity variable names available **daily** vs **hourly** (source docs list `relative_humidity_2m` as daily+hourly — confirm e.g. `relative_humidity_2m_max`); `timezone` param; `forecast_days` max; archive endpoint & date range (docs: history back to 1940); multi-location syntax and call counting; free limits (shared with flood: 10,000/day total → **shared Redis budget key**). Record IMD/NDMA heat-wave criteria from the **official source** (source docs came from exam-prep sites).

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|
| `kestrel_heat_monitored_point` | `locationId` | — | code, lat, lon, regionType, active, locationVersion |
| `kestrel_heat_normal` | `locationId` | `dayOfYear` (1–366) | normalTmaxC, basePeriod, computedAt |
| `kestrel_heat_forecast_run` | `locationId` | `runId` | fetchedAt, dataAsOf, payloadHash, status, error |
| `kestrel_heat_forecast_day` | `runId` | `date` | tmax, apparentMax, humidityMax, normalTmax, departure |
| `kestrel_heat_feed_state` | `feed` | — | state, lastSuccessAt, lastErrorAt, consecutiveFailures, lastError |
| `kestrel_heat_outbox` | `eventId` | — | topicSuffix, payload, createdAt, publishedAt (TTL), attempts |
| `kestrel_heat_inbox` | `eventId` | `consumerName` | processedAt, ttl |

## 3. Normals (computed once per point; recompute on request)
- Fetch daily `temperature_2m_max` for the **30-year base period 1991–2020** (config; WMO standard climate-normal convention — confirm it matches the IMD normal you choose) from the archive API in yearly chunks (budget-guarded).
- `normal(doy)` = mean of Tmax over base years for days within a **±7-day window** around `doy` (smooths noise); Feb 29 maps to Feb 28/Mar 1 average. Store 366 rows.
- Reject point if < 25 valid years. **Unit tests** with synthetic series (known means, leap-year handling).

## 4. EventBridge-triggered run
- EventBridge Scheduler cron twice daily (config; default `0 15 4,16 ? * * *` UTC — the early run lets rules/fanout meet the **before-9-AM advisory** goal). Batched multi-location call, `forecast_days=7`, `timezone=Asia/Kolkata`.
- For each day compute `departure = tmax − normal(doy)`. Persist run/days in DynamoDB; publish `HeatSignal` (doc 03 §3.2) via outbox; payload-hash dedupe; keep last-good; FeedState `OPEN_METEO_WEATHER`.
- Shared call budget with flood (`CallBudget` DynamoDB key `BUDGET#om#*` — same DynamoDB table as flood; use shared `TABLE_PREFIX` for budget counters) so combined usage stays < caps.

## 5. REST
`GET /api/v1/heat/points` (latest tmax/departure/data age) · `GET /api/v1/heat/points/{id}/forecast` · `GET /api/v1/heat/points/{id}/normals?doy=` · `GET /api/v1/heat/feed-status` · ADMIN `POST /api/v1/heat/admin/run-now`, `POST …/normals/recompute`.

## 6. Heat action text (consumed by fanout/composer; English; translate hi/mr → NEEDS NATIVE REVIEW)
WATCH: "High temperatures are expected. Drink water often and avoid heavy outdoor work in the afternoon." · WARNING: "Heat wave conditions are likely on consecutive days. Avoid going out between 12 noon and 3 PM, drink plenty of water, and look after children, elders and outdoor workers." · SEVERE: "Severe heat wave expected. Stay indoors in the hottest hours, drink water regularly, and seek medical help if someone feels dizzy, weak or faint." Always append the provenance line (model-based advisory, **not an IMD declaration**).

## 7. Tests
Normals math + leap-year; departure calc; WireMock IT for run, failure, malformed, missing-days; shared budget enforcement across flood+heat; resilience: heat feed killed → flood alerts unaffected (part of chaos suite).

## 8. Task breakdown
P3-01 DynamoDB tables/replica/Open-Meteo client · P3-02 normals computation · P3-03 EventBridge-triggered run + SNS signal publish · P3-04 APIs + CloudWatch metrics · P3-05 tests/README/Lambda/SAM.

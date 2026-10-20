# 14 — aq-ingest (Phase 3) · Lambda `kestrel-aq-ingest` · DynamoDB prefix `kestrel_aq_`

Tasks **P3-06 … P3-10**. Pulls modelled air quality (Open-Meteo/CAMS) and optional **observed** station data (OpenAQ) for cross-checking. Computes **rolling averages**; AQI maths lives in rules-engine.

## 1. Verify first (⚠ VERIFY-04, VERIFY-05, VERIFY-08, VERIFY-16)
- Open-Meteo Air Quality: hourly `pm10, pm2_5, nitrogen_dioxide, sulphur_dioxide, ozone, carbon_monoxide`; `past_days`, `forecast_days`, `domains`; **units (CO is µg/m³ in the API; CPCB uses mg/m³ — convert ÷1000)**; outside Europe: global CAMS ~45 km, 3-hourly, updated every 12 h (source docs) → this is a **modelled estimate, not a station reading**; CAMS attribution required.
- OpenAQ **v3**: needs a free API key (header), call **server-side only**; limits per source docs ≈ 60/min & 2,000/hour (third-party listing); source docs say Indian coverage figures are from **2021** (276 stations / 145 cities) → check current coverage and endpoints (`locations` by coordinates+radius, `latest`/`measurements`).

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|
| `kestrel_aq_monitored_point` | `locationId` | — | code, lat, lon, active, locationVersion |
| `kestrel_aq_station_cache` | `locationId` | `stationId` | name, distanceKm, parameters (list), lastCheckedAt |
| `kestrel_aq_forecast_run` | `locationId` | `runId` | fetchedAt, dataAsOf, payloadHash, status, error, resolutionKm |
| `kestrel_aq_hourly_value` | `runId` | `ts` | pm10, pm25, no2, so2, o3, coUgm3; ttl (5 days) |
| `kestrel_aq_observed_value` | `locationId` | `stationId#parameter` | value, unit, measuredAt, ttl |
| `kestrel_aq_feed_state` | `feed` | — | state, lastSuccessAt, lastErrorAt, consecutiveFailures, lastError |
| `kestrel_aq_outbox` | `eventId` | — | topicSuffix, payload, createdAt, publishedAt (TTL), attempts |
| `kestrel_aq_inbox` | `eventId` | `consumerName` | processedAt, ttl |

## 3. Processing
1. Fetch hourly series with `past_days=2` (so trailing windows are complete) and `forecast_days=3` (config).
2. **Rolling windows (trailing):** 24-hour mean for PM10, PM2.5, NO₂, SO₂; 8-hour mean for O₃ and CO (CO converted to mg/m³). Window valid if ≥ `minValidHours` data points (default 16 of 24, 6 of 8; **configurable, ⚠ VERIFY-05**; note the source model is 3-hourly outside Europe so account for native resolution — count distinct native timesteps, not interpolated hours).
3. `current` = windows ending at the latest hour ≤ now; `forecastPeaks[d]` = max of rolling windows ending within day d.
4. **OpenAQ cross-check (optional, key present):** find nearest station within `AQ_STATION_MAX_DISTANCE_KM` (default 25) with recent (≤3 h) PM2.5/PM10; attach `observed`. Cache station selection 24 h in DynamoDB (TTL). Respect limits via `CallBudget` (DynamoDB counter; 60/min, 1,800/hour safety).
5. Publish `AqSignal` (doc 03 §3.3) via DynamoDB outbox → SNS `kestrel-signals`; payload-hash dedupe; EventBridge Scheduler: model every 12 h (default `0 20 2,14 ? * * *` UTC) + EventBridge-triggered OpenAQ refresh Lambda hourly, publishing a *light* update only if observed value changed materially (> 10%).
6. Feed failure isolation: `OPEN_METEO_AQ` and `OPENAQ` tracked separately; OpenAQ down ⇒ signals continue without `observed`.

## 4. REST
`GET /api/v1/aq/points` (current windows, observed, data age, resolutionKm) · `GET /api/v1/aq/points/{id}` · `GET /api/v1/aq/feed-status` (both feeds) · ADMIN `POST /api/v1/aq/admin/run-now`.

## 5. AQ action text (English; hi/mr NEEDS NATIVE REVIEW)
WATCH (Poor): "Air quality is poor. Children, elders and people with breathing problems should limit outdoor activity." · WARNING (Very Poor): "Air quality is very poor. Avoid outdoor exercise, keep windows closed, and wear an N95-type mask if you must go out." · SEVERE: "Air quality is severe. Stay indoors as much as possible and seek medical help if you have trouble breathing." Always state: "Estimated from a regional model (~45 km); local conditions may differ." plus provenance line.

## 6. Tests
Rolling-window math with gaps/native-resolution; unit conversion (CO); window validity rules; WireMock: Open-Meteo normal/failed, OpenAQ normal/401/429/empty; **fault-isolation IT:** AQ down → flood path latency unchanged; payload-hash dedupe; observed-attach logic.

## 7. Task breakdown
P3-06 DynamoDB tables/replica/Open-Meteo AQ client · P3-07 rolling averages (domain) · P3-08 OpenAQ client + station cache (DynamoDB) + DynamoDB budget · P3-09 EventBridge scheduling + `AqSignal` SNS publish + feed states · P3-10 APIs/tests (LocalStack)/README/Lambda/SAM.

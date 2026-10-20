# 09 — cap-ingest (Phase 2) · Lambda `kestrel-cap-ingest` · DynamoDB prefix `kestrel_cap_`
(Called `official-alert-ingest` in earlier planning docs; **canonical name is `cap-ingest`**.)

Tasks **P2-01 … P2-05**. Parses **SACHET** (NDMA's CAP-based national alert system; publishes IMD, CWC and state alerts) into the "official" signal. Third-party format → own failure mode → own service. Triggered by **EventBridge Scheduler** (poll interval). The **static CAP feed file** (Atom/XML) is generated and uploaded to S3 / served from Amplify or Cloudflare Pages.

## 1. Verify first (⚠ VERIFY-09, VERIFY-17)
Source docs say SACHET offers RSS feeds (national + state-level) whose items link to **CAP 1.2 XML**, with no key mentioned, and that **you must test the feed yourself**. Before coding: discover actual feed URLs from the SACHET public site, record URLs, update frequency, item structure, geometry types present (`polygon`, `circle`, `geocode`, `areaDesc` only?), languages, sample CAP XML in `tools/wiremock-upstream/fixtures/sachet/`. **Never hard-code feed URLs** — config `cap.feeds[]` (`SACHET_FEED_URLS`). Confirm CAP 1.2 required elements and download the OASIS XSD for validation tests.

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|
| `kestrel_cap_official_alert` | `capIdentifier` | — | sender, sentAt, status, msgType, scope, event, category, urgency, severity, certainty, onset, effective, expires (TTL), headline, description, instruction, areaDesc, geometry (map), geocodes (map), sourceUrl, rawXmlS3Key, hazard, mappedSeverity, firstSeen, lastSeen, supersededBy |
| `kestrel_cap_alert_location` | `capIdentifier` | `locationId` | matchMethod (POLYGON/CIRCLE/GEOCODE/NAME) |
| `kestrel_cap_location_replica` | `locationId` | — | code, name, state, district, lat, lon, version |
| `kestrel_cap_feed_state` | `feed` | — | state, lastSuccessAt, lastErrorAt, consecutiveFailures, lastError, etag, lastModified |
| `kestrel_cap_outbox` | `eventId` | — | topicSuffix, payload, createdAt, publishedAt (TTL), attempts |
| `kestrel_cap_inbox` | `eventId` | `consumerName` | processedAt, ttl |

Raw CAP XML stored in S3 (referenced by `rawXmlS3Key`); DynamoDB stores parsed fields only. Active alerts retain items; expired alerts have `expires` as TTL.

## 3. Behaviour
1. Poll each feed every `cap.pollInterval` (default 10 min) with `If-None-Match/If-Modified-Since`; parse RSS (disable DTD/external entities — **XXE-safe parser mandatory**; max size 2 MB; timeouts 10 s).
2. For each new/updated item: fetch CAP XML (rate-limited, ≤ N per cycle), validate vs XSD (warn+store on failure, skip signal if unparseable), upsert by `capIdentifier`.
3. **Hazard mapping** (config keyword lists, case-insensitive on `event`/`category`/`headline`): FLOOD ← flood, inundation, river, dam release, high flood; HEAT ← heat wave, hot, heat; AIR_QUALITY ← air quality, pollution; else `OTHER` (stored, **not** forwarded to fusion, but visible in dashboard official list).
4. **Severity map**: CAP `Extreme→SEVERE, Severe→WARNING, Moderate→WATCH, Minor→WATCH, Unknown→WATCH` (config).
5. **Location matching** against `location_replica`: polygon → point-in-polygon; circle → haversine ≤ radius; `geocode`/`areaDesc` → normalized state/district name match (fuzzy ≥ 0.85, config) ; always record `match_method`. Only matched locations get signals. Re-match when `LocationUpserted` arrives.
6. `msgType`: `Alert` new; `Update` supersedes `references`; `Cancel` ends referenced alerts. Expiry handled by `expires`.
7. Publish `OfficialAlertSignal` via DynamoDB outbox → SNS `kestrel-signals` per (alert, change); `FeedStatus` on change + heartbeat (`feed=SACHET`).
8. **Static CAP feed:** after each poll cycle, regenerate the Atom/XML feed of active alerts and upload to S3 (`cap-feed/feed.xml`); CloudFront serves it at `https://app.<domain>/cap/feed.xml` with `Cache-Control: max-age=60`.
8. **Provenance rule:** cap-ingest only reports facts; labelling decisions are in signal-fusion.

## 4. REST
`GET /api/v1/official/alerts?active=true&locationId=&hazard=` (COORDINATOR) · `GET /api/v1/official/alerts/{id}` (incl. raw CAP link) · `GET /api/v1/official/feed-status` · ADMIN `POST /api/v1/official/admin/poll-now`.

## 5. Config
`CAP_FEED_URLS`, `CAP_POLL_INTERVAL`, `CAP_MAX_ITEMS_PER_CYCLE`, `CAP_MATCHING_FUZZY_THRESHOLD`, `CAP_HAZARD_KEYWORDS_*`, `CAP_SEVERITY_MAP_*`, `TABLE_PREFIX`, `SNS_TOPIC_ARN_SIGNALS`, `S3_CAP_FEED_BUCKET`, `S3_CAP_FEED_KEY`.

## 6. Resilience
Feed failure → DEGRADED/DOWN; last-good retained; **official alerts remain active until their `expires`** (do not drop on feed outage); dashboard shows feed health. SACHET delays/outages must not affect model alerts.

## 7. Tests
XXE attempt rejected; sample CAP (polygon, circle, geocode-only) matched correctly; Update/Cancel chains; unmapped hazard stored but not published; expiry; conditional GET 304; malformed XML; large payload rejected; fuzzy match threshold edge cases; contract test for `OfficialAlertSignal`.

## 8. Task breakdown
P2-01 DynamoDB tables + replica + feed client (conditional GET, XXE-safe) · P2-02 CAP parser + XSD validation · P2-03 hazard/severity mapping + location matching · P2-04 SNS signal publishing + update/cancel/expiry + feed state + static CAP file to S3 · P2-05 APIs + tests (LocalStack) + README/Lambda/SAM.

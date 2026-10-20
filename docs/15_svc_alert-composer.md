# 15 — alert-composer (Phase 3) · Lambda `kestrel-alert-composer` · DynamoDB prefix `kestrel_composer_`

Tasks **P3-15 … P3-19**. Split out of alert-fanout once messages get complex. Builds **CAP-shaped**, localized messages and generates **our own CAP feed** as a **static file** uploaded to S3 / served from Amplify or Cloudflare Pages (same host as frontend).
Rebinding: set `FANOUT_INPUT_STAGE=COMPOSED` (env var only). If composer is disabled, fanout falls back to its internal templates (contract-compatible).

## 1. Verify first (⚠ VERIFY-17, VERIFY-18)
Fetch the OASIS CAP 1.2 spec + XSD: required elements, enumerations, identifier/`sent` formats, `references` syntax, `scope`, `status`. Source docs: CAP is an ITU standard (versions 1.1, 1.2).

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|
| `kestrel_composer_template` | `hazard#severity#language#kind` | — | body, needsReview, version, updatedBy, updatedAt |
| `kestrel_composer_composed_alert` | `alertId` | — | locationId, hazard, severity, msgType, provenance, content (map), capXmlS3Key, capIdentifier (GSI), sentAt, validUntil, references (list) |
| `kestrel_composer_outbox` | `eventId` | — | topicSuffix, payload, createdAt, publishedAt (TTL), attempts |
| `kestrel_composer_inbox` | `eventId` | `consumerName` | processedAt, ttl |

Composed CAP XML stored in S3 (`cap-alerts/{identifier}.xml`); DynamoDB stores parsed fields + S3 key. Active alert feed file regenerated on each compose and uploaded to S3 (`cap-feed/feed.xml`).

## 3. Composition (consumes `composer.fused`)
1. Select templates by hazard/severity/language; substitute `{location}`, `{validUntil}`, `{authority}`, metrics (e.g. `{firstExceedanceDate}`), keep placeholders whitelisted (no arbitrary expression evaluation).
2. **Provenance sentence is mandatory** and generated from provenance (doc 03 §7); templates cannot override it.
3. Build `content`: `{headline{lang}, instruction{lang}, short{lang}}` and `cap` fields. CAP defaults: WATCH→Moderate/Future/Possible, WARNING→Severe/Expected/Likely, SEVERE→Extreme/Immediate/Likely (doc 03 §6); `OFFICIAL_CONFIRMED` keeps the official certainty/severity values from `OfficialAlertSignal` where available.
4. Generate **CAP 1.2 XML** (`identifier` unique & stable pattern `kestrel-{alertId}-{seq}`, `sender` = configured, `sent`, `status` Actual (or Test when `COMPOSER_TEST_MODE=true`), `msgType` Alert/Update/Cancel with `references` to previous identifiers, `scope=Public`, one `info` per language with `category`, `event`, `urgency`, `severity`, `certainty`, `effective/expires`, `headline`, `description`, `instruction`, `area/areaDesc` + circle around the point, `parameter` `provenance=<label>`). XML built with a safe writer (escape all text; no external entities). Upload to S3 `cap-alerts/{identifier}.xml`.
5. **Dedupe**: skip if a `kestrel_composer_composed_alert` item with same (alertId, severity, provenance, msgType) exists in DynamoDB.
6. Publish `AlertEvent(stage=COMPOSED, content, cap)` via DynamoDB outbox → SNS `kestrel-alerts`. Regenerate and upload static `cap-feed/feed.xml` to S3.

## 4. CAP feed & APIs
`GET /api/v1/cap/alerts/{identifier}.xml` (from S3, served via CloudFront) · `GET /api/v1/cap/alerts?active=true` (JSON for dashboard, queried from DynamoDB) · The Atom feed (`feed.xml`) is served as a **static S3 file** via CloudFront at `https://api.<domain>/cap/feed.xml` with `Cache-Control: max-age=60` and ETag from S3. COORDINATOR/ADMIN: `GET|PUT /api/v1/cap/templates` (versioned, audit-logged in DynamoDB; `PUT` marks `needsReview=false` only by ADMIN) · `POST /api/v1/cap/templates/preview` (render with sample data per language).
CAP feed must never present model-only alerts as official (`sender` = our org; provenance parameter present).

## 5. Tests
XSD validation of generated XML for all hazards/severities/msgTypes (Update/Cancel with references); special characters in Hindi/Marathi (UTF-8, escaping); placeholder whitelist; mandatory provenance sentence cannot be removed; dedupe; feed lists only active alerts; contract test for COMPOSED event; fanout E2E using composed content.

## 6. Task breakdown
P3-15 DynamoDB tables + template store + seeds · P3-16 composition engine + provenance enforcement · P3-17 CAP XML builder + XSD tests + S3 upload · P3-18 SNS events + static CAP feed regeneration + template APIs · P3-19 fanout rebinding E2E + README/Lambda/SAM.

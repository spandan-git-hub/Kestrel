# 05 — subscriber-registry (MVP) · Lambda `kestrel-subscriber-registry` · DynamoDB prefix `kestrel_registry_`

Tasks **P1-01 … P1-10**. Owns locations, coordinators/auth (via Cognito), subscriptions, consent. Publishes replicas for other services via SNS.

## 1. Responsibilities
- Monitored **locations** catalog (the only place locations are created).
- Coordinator accounts + Cognito JWT issuance (login/refresh/logout) — D-07, D-25.
- Resident **subscriptions**: location(s), hazards, language, min severity, channels; opaque management token.
- **Consent** log; unsubscribe + erase.
- Telegram deep-link binding (long-polling `getUpdates` via EventBridge-triggered Lambda), Web Push subscription storage.
- Aggregate **stats** for coordinators (no PII).
- Load-test seeding (env var `LOADTEST_ENABLED=true` only).
Not responsible for: sending alerts, storing alert state, reports.

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|---|
| `kestrel_registry_location` | `locationId` | — | code, name, nameLocal (map), state, district, lat, lon, regionType, timezone, hazardsEnabled (list), active, version, createdAt, updatedAt |
| `kestrel_registry_coordinator` | `coordinatorId` | — | email (GSI), passwordHash, displayName, role, active, createdAt |
| `kestrel_registry_coordinator_location` | `coordinatorId` | `locationId` | — |
| `kestrel_registry_refresh_token` | `tokenHash` | — | coordinatorId, expiresAt (TTL), revokedAt |
| `kestrel_registry_subscription` | `subscriptionId` | — | tokenHash (GSI), language, minSeverity, status, version, createdAt, updatedAt, deletedAt (TTL) |
| `kestrel_registry_subscription_location` | `subscriptionId` | `locationId` | — |
| `kestrel_registry_subscription_hazard` | `subscriptionId` | `hazard` | — |
| `kestrel_registry_subscription_channel` | `subscriptionId` | `channel` | address, status, verifiedAt |
| `kestrel_registry_consent_log` | `subscriptionId` | `grantedAt` | channel, consentTextVersion, withdrawnAt, ipHash |
| `kestrel_registry_telegram_link` | `tokenHash` | — | subscriptionId, expiresAt (TTL), usedAt |
| `kestrel_registry_outbox` | `eventId` | — | topicSuffix, routingKey, payload, createdAt, publishedAt (TTL 7d once published), attempts |
| `kestrel_registry_inbox` | `eventId` | `consumerName` | processedAt, ttl (30d) |

GSIs: `kestrel_registry_coordinator` on `email`; `kestrel_registry_subscription` on `tokenHash`; `kestrel_registry_subscription` on `status, version` (for cursor-based changes API).
`version` is a global monotonically increasing value (use an atomic DynamoDB counter in a separate item `PK=COUNTER#version`) bumped on every subscription/location change.

## 3. REST API
**Public**
| Method & path | Notes |
|---|---|
| `GET /api/v1/locations?hazard=` | active locations (id, code, localized name, lat/lon, hazards) |
| `GET /api/v1/locations/nearest?lat&lon&limit=3` | haversine; validates India bounds |
| `POST /api/v1/subscriptions` | body: `{locationIds[1..5], hazards[], language, minSeverity?, consent:{accepted:true, textVersion}}` → `201 {subscriptionId, managementToken (shown ONCE), telegramLink?}`; rate-limited (DynamoDB counter + TTL); **rejects without explicit consent** |
| `GET /api/v1/subscriptions/me` | header `X-Subscriber-Token` |
| `PUT /api/v1/subscriptions/me` | change locations/hazards/language/minSeverity |
| `DELETE /api/v1/subscriptions/me` | unsubscribe + **erase** (hard-delete addresses; keep anonymized consent record) |
| `POST /api/v1/subscriptions/me/telegram-link` | returns `{deepLink:"https://t.me/<bot>?start=<token>", expiresAt}` (token TTL 15 min, single use) |
| `POST /api/v1/subscriptions/me/channels/web-push` | `{endpoint, keys:{p256dh,auth}}` → ACTIVE |
| `DELETE /api/v1/subscriptions/me/channels/{channel}` | withdraw consent for channel |
| `GET /api/v1/push/vapid-public-key` | |
**Auth (Cognito)**
`POST /api/v1/auth/login {email,password}` → calls Cognito `InitiateAuth` → `{accessToken, expiresIn}` + `Set-Cookie refresh` (httpOnly, Secure, SameSite=Lax, path `/api/v1/auth`) · `POST /auth/refresh` (Cognito token rotation) · `POST /auth/logout` (Cognito sign-out + revoke refresh). Login rate limit + lockout: 5 failures → Cognito account lockout (configurable in user pool). `⚠ VERIFY-25`.
**Coordinator/Admin**
`GET /api/v1/coordinators/me` · `GET /api/v1/subscribers/stats?locationId=` (counts by channel/language/hazard/minSeverity — **counts only**) · ADMIN: `POST|PUT|DELETE /api/v1/locations`, `POST /api/v1/admin/locations/import` (CSV), `POST|PUT /api/v1/admin/coordinators` (creates Cognito user).
**Internal** `GET /internal/v1/locations` · `GET /internal/v1/subscriptions/changes?cursor=&limit=500` (ordered by `version`, returns full `SubscriptionChanged` snapshots incl. DELETED tombstones). Internal paths blocked by CloudFront; called via direct Lambda invocation or same-VPC if needed.
**Loadtest** `POST /api/v1/admin/loadtest/subscribers {count, locationIds, channel:"MOCK"}` — only when `LOADTEST_ENABLED=true` (otherwise 404). Inserts in DynamoDB batch (BatchWriteItem, 25/batch), publishes events through outbox.

## 4. Events
Publishes (outbox → SNS `kestrel-registry`): `LocationUpserted`, `LocationDeleted`, `SubscriptionChanged`. Consumes: none.
On startup (cold start or first invocation with empty DynamoDB): if `kestrel_registry_location` empty and `infra/seed/locations.csv` present (config `REGISTRY_SEED_ENABLED=true`) → import. Bootstrap admin Cognito user from `ADMIN_BOOTSTRAP_EMAIL/PASSWORD` if no admin group member exists.

## 5. Logic details
- Token generation: 32 random bytes URL-safe base64; store SHA-256 only. Management token **never** retrievable again.
- Validation: ≤5 locations per subscription; language ∈ {en,hi,mr}; hazards ⊆ location.hazardsEnabled; consent text version must exist in config (`CONSENT_VERSIONS`).
- Telegram poller: separate EventBridge-triggered Lambda `kestrel-telegram-poller` runs every 30 s; calls Telegram `getUpdates?timeout=0&offset=…`; on `/start <token>` → bind chat to subscription (channel `TELEGRAM` ACTIVE) in DynamoDB, reply in subscriber language; `/stop` → withdraw channel; persists offset in DynamoDB item. `⚠ VERIFY-10`.
- Web push validation: endpoint must be https; store as JSON in `subscription_channel`; `⚠ VERIFY-11` (iOS requires installed PWA).
- Every mutation bumps `version` (atomic DynamoDB counter), writes outbox item in the same DynamoDB `TransactWriteItems`.
- Retention: erased subscriptions: set `deletedAt` TTL; consent log anonymized after `CONSENT_RETENTION_DAYS` (default 365 — **configurable pending VERIFY-13**).
- Authorization: coordinator may only see stats for `locationIds` in Cognito JWT custom attribute; ADMIN all. Cognito groups: `coordinator`, `admin`.

## 6. Config (SSM Parameter Store / environment)
`COGNITO_USER_POOL_ID`, `COGNITO_CLIENT_ID`, `COGNITO_JWK_URI`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, `TELEGRAM_ENABLED`, `VAPID_PUBLIC_KEY`, `RATE_LIMIT_SUBSCRIBE_PER_IP_PER_HOUR`, `REGISTRY_SEED_ENABLED`, `CONSENT_VERSIONS`, `TABLE_PREFIX`, `SNS_TOPIC_ARN_REGISTRY`.

## 7. Metrics (CloudWatch)
`kestrel.subscriptions.active{channel}`, `kestrel.subscription_changes.total`, `kestrel.auth.logins.total{outcome}`, `kestrel.telegram_link.total{outcome}`.

## 8. Tests (must exist)
Unit: token hashing, validation, scope. LocalStack IT: create→get→update→delete; consent required; Cognito mock or WireMock Cognito endpoint; refresh rotation + reuse detection; event emitted exactly once per change (DynamoDB outbox + SNS); `changes` cursor ordering incl. tombstones; Telegram poller against WireMock (/start, /stop); DynamoDB rate-limit; stats scope enforcement; loadtest seeding 50k < 60 s on dev machine.

## 9. Task breakdown
- P1-01 DynamoDB tables + entity models/repos · P1-02 locations CRUD/import/events/seed · P1-03 auth (Cognito integration, login/refresh/logout/bootstrap admin) · P1-04 subscriptions API + consent · P1-05 Telegram link + poller Lambda · P1-06 web-push storage + vapid key · P1-07 internal changes API + `SubscriptionChanged` completeness · P1-08 stats · P1-09 loadtest seeding · P1-10 README/Lambda handler/OpenAPI/ITs green (LocalStack).

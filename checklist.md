# CHECKLIST.md — Kestrel Development Progress Tracker

> **Living file.** The agent updates it at the end of EVERY task (see master.md §0 and §9).
> Tick format: `- [x] P1-12 — … · ✔ 2026-10-21 · note/evidence`. Never tick without green build + tests.

---

## ▶ CURRENT STATUS (update every task)

| Field | Value |
|---|---|
| Current phase | **P0 — Foundation** |
| Last completed task | **P0-01** |
| In progress | — |
| Next task | **P0-02** |
| Blockers / questions | none |
| Last updated | 2026-10-20 |
| Build status (backend / frontend) | n/a / n/a |

Progress: P0 `1/21` · P1 `0/51` · P2 `0/28` · P3 `0/34` · P4 `0/11`

---

## 📌 DECISION LOG (add new entries; never silently change a locked decision)

| ID | Decision | Status |
|---|---|---|
| D-01 | Java 21 + Spring Boot 3.5.x + Spring Cloud 2025.0.x (confirm in VERIFY-01; Boot 4.x only via new decision) | locked |
| D-02 | **SQS + SNS** for all async messaging (replaces RabbitMQ). One SQS queue per consumer; SNS where one event fans out to multiple services | locked |
| D-03 | Maven multi-module monorepo | locked |
| D-04 | **DynamoDB** as primary persistence (replaces PostgreSQL + Redis). Tables prefixed `kestrel_<service>_`. No cross-service table access. TTL used for cache/last-good entries. Aurora DSQL is an optional SQL-compatible alternative — decide per service in VERIFY-23 | locked |
| D-05 | Transactional outbox for all publishes (DynamoDB transaction for outbox item + business item); inbox idempotency in DynamoDB for all consumers | locked |
| D-06 | **CloudFront + Lambda Function URLs** as edge in MVP; Cognito JWT validation added in Phase 2 | locked |
| D-07 | Coordinator JWT issued by Cognito (replaces custom HS256 in subscriber-registry); refresh token httpOnly cookie | locked |
| D-08 | Flood thresholds: Gumbel fit from Open-Meteo history by default; GloFAS CSV override if model version matches | locked |
| D-09 | Severity mappings & CAP mappings (flood/heat/AQ) are adaptations; configurable | locked |
| D-10 | Persistence defaults WATCH=1, WARNING=2, SEVERE=2 runs; de-escalation 2 runs; cooldown 12 h | locked (tune via backtest) |
| D-11 | Auto-send only for severity ≥ WARNING; WATCH needs coordinator approval (per-location override) | locked |
| D-12 | Leaflet + OSM tiles for maps | locked |
| D-13 | Tailwind; hand-rolled SVG charts; no UI kit | locked |
| D-14 | Standards-based Web Push (VAPID) rather than FCM | locked |
| D-15 | Local replicas via events (+ reconciliation) in ingest services and fanout — replicas stored in DynamoDB | locked |
| D-16 | Community reports never raise severity by themselves | locked |
| D-17 | subscriber-registry owns the locations catalog | locked |
| D-18 | Official SACHET alerts are relayed with explicit source label | locked |
| D-19 | **CloudFront** as TLS/CDN edge (replaces Caddy + Oracle VM); custom domain `api.<domain>` + `app.<domain>` | locked |
| D-20 | Off-platform backup target: **S3 (free tier 5 GB)** for DynamoDB export and SAM/CDK state | **OPEN — confirm in P0-09** |
| D-21 | Project name: `kestrel`; Base Java package: `com.kestrel` (`<service>`, `common.*`); Telegram bot: `kestrel_alert_bot`; Domains: `api.kestrel.in`, `app.kestrel.in` (local: `localhost` / `kestrel.local`) | locked |
| D-22 | **AWS SAM** (preferred for simplicity) or **AWS CDK** for infrastructure as code — whole setup in one template | **OPEN — decide in P0-09** |
| D-23 | **AWS Amplify Hosting** for frontend for first 12 months (1,000 build minutes/month free); migrate back to **Cloudflare Pages** at month 12 (git-reconnect only, no rewrite) | locked |
| D-24 | **EventBridge Scheduler** triggers all ingest Lambda functions on schedule (replaces `@Scheduled` inside a long-running process) | locked |
| D-25 | **Amazon Cognito** for coordinator/admin auth (10,000 MAU free). SnapStart enabled for user-facing Lambda functions | locked |

---

## 🔎 VERIFICATION REGISTER (facts the source docs did NOT confirm — verify before coding against them)

Status values: `TODO` · `DONE (date, url, finding)` · `CANNOT-VERIFY (assumption made, configurable)`.

| ID | What to verify | Needed before | Status |
|---|---|---|---|
| VERIFY-01 | Spring Boot ↔ Spring Cloud compatible versions, support window (3.5.x vs 4.x), correct Gateway starter artifact | P0-02 | TODO |
| VERIFY-02 | Open-Meteo **Flood** API: params (`ensemble`, members naming/count 50 vs 51, `forecast_days`, history range), multi-location syntax + **how calls are counted**, free limits (600/min, 10k/day, 300k/month) | P1-12 | TODO |
| VERIFY-03 | Open-Meteo **Weather/Archive** variable names (daily temp max, apparent temp max, humidity daily vs hourly), archive range | P3-01 | TODO |
| VERIFY-04 | Open-Meteo **Air Quality** params (`past_days`, domains), units (CO µg/m³), native resolution/update cadence outside Europe | P3-06 | TODO |
| VERIFY-05 | **CPCB AQI** breakpoints table, minimum-data rules, CO unit (mg/m³), top-band handling | P3-12 | TODO |
| VERIFY-06 | **IMD/NDMA heat-wave criteria** from official docs (gate temps per region, departure thresholds, 45/47 °C, 2-day/stations rule), normals base period | P3-11 | TODO |
| VERIFY-07 | GloFAS threshold raster **model version vs Open-Meteo's** (v4?) → decide `GLOFAS_CSV` vs `GUMBEL_FIT` | P1-13 | TODO |
| VERIFY-08 | OpenAQ v3 endpoints, key, rate limits, **current** India station coverage | P3-08 | TODO |
| VERIFY-09 | SACHET feed URLs, formats, update cadence, geometry types; test live | P2-01 | TODO |
| VERIFY-10 | Telegram Bot API limits (global ~30 msg/s, ≤1/s per chat, group limits), getUpdates behaviour | P1-05, P1-29 | TODO |
| VERIFY-11 | Web Push + VAPID library compatibility; **iOS** requirements (installed PWA) | P1-06, P1-30, P1-44 | TODO |
| VERIFY-12 | WhatsApp share link format (`wa.me/?text=` vs `api.whatsapp.com/send?text=`) on Android/iOS/desktop | P1-40 | TODO |
| VERIFY-13 | India **DPDP Act 2023** + rules obligations before real users | P4-02 (and before any pilot) | TODO |
| VERIFY-14 | **AWS Amplify Hosting** free tier current terms (build minutes, bandwidth, hosting limits). **Cloudflare Pages** free plan limits (500 builds/month, 20,000 files). Verify no card required for Amplify free tier | P0-09 / P1-50 | TODO |
| VERIFY-15 | OpenStreetMap tile usage policy / alternative tile provider for production | P1-38 | TODO |
| VERIFY-16 | Open-Meteo/CAMS/OpenAQ terms & exact attribution wording (non-commercial limits) | P1-12 | TODO |
| VERIFY-17 | CAP 1.2 spec/XSD required elements & enumerations | P2-02, P3-17 | TODO |
| VERIFY-18 | Hindi & Marathi message/UI translations reviewed by native speakers | P1-46 / P4-05 | TODO |
| VERIFY-19 | **AWS Lambda** free tier: 1M requests/month + 400,000 GB-seconds/month — confirm limits and cold-start behaviour for Spring Boot handlers | P0-09 | TODO |
| VERIFY-20 | **DynamoDB** free tier: 25 GB storage + 25 provisioned RCU/WCU — confirm on-demand vs provisioned trade-off for free tier | P0-09 | TODO |
| VERIFY-21 | **SQS** free tier (1M requests/month) and **SNS** free tier (1M publishes/month) — confirm message size limits and FIFO vs standard queue | P0-09 | TODO |
| VERIFY-22 | **Lambda SnapStart** for Java 21 — supported runtimes, cold-start improvement, restore hooks needed | P0-10 | TODO |
| VERIFY-23 | **Aurora DSQL** free tier (100,000 DPUs, 1 GB-month), Flyway compatibility, no extensions/triggers constraint — decide DynamoDB vs Aurora DSQL per service | P0-09 | TODO |
| VERIFY-24 | **EventBridge Scheduler** free tier (14M invocations/month) and minimum schedule interval | P0-09 | TODO |
| VERIFY-25 | **Amazon Cognito** free tier (10,000 MAU) — confirm JWT format for Spring Security integration | P0-06 | TODO |
| VERIFY-26 | **CloudFront** free tier (1 TB transfer + 10M requests/month) — confirm Lambda Function URL integration and custom-domain setup | P0-09 | TODO |

---

## 🚧 BLOCKERS / QUESTIONS FOR THE USER
_(none yet — agent appends here with date and task ID)_

---

## PHASE 0 — Foundation `docs: 02, 03, 04, 17`

- [x] P0-01 — Create monorepo skeleton (`backend/ frontend/ infra/ tools/ docs/`), copy master.md/checklist.md/docs, `.gitignore`, `.editorconfig`, root README. · confirm D-21 · ✔ 2026-10-20 · skeleton dirs (.gitkeep), .gitignore, .editorconfig, root README created, D-21 locked · deps: —
- [ ] P0-02 — Backend parent POM (BOMs, plugin mgmt, Java 21, enforcer incl. ban Lombok, Spotless, JaCoCo, surefire/failsafe), Maven wrapper. · doc 02 §1 · deps: P0-01, VERIFY-01
- [ ] P0-03 — `libs/common-events`: envelope, enums, all event records, JSON Schemas, serialization + schema tests. · doc 02 §2.1, doc 03 · deps: P0-02
- [ ] P0-04 — `libs/common-core`: problem+json advice, correlation filter, JSON logging, Clock, PageResponse, GeoUtils, FeedStateTracker, CallBudget (DynamoDB-backed). · doc 02 §2.2 · deps: P0-02
- [ ] P0-05 — `libs/common-outbox`: DynamoDB outbox/inbox tables, publisher (DynamoDB transactional write), relay to SQS/SNS, idempotent handler, DLQ configuration, metrics, LocalStack tests. · doc 02 §2.3 · deps: P0-03, P0-04
- [ ] P0-06 — `libs/common-security`: Cognito JWT decode/filter, roles, scope check, subscriber token hash, config validation, tests. · doc 02 §2.4 · deps: P0-04, VERIFY-25
- [ ] P0-07 — `libs/common-testing`: LocalStack (Testcontainers) bases for DynamoDB/SQS/SNS, WireMock support, fixtures, fixed clock, Awaitility helpers. · doc 02 §2.5 · deps: P0-02
- [ ] P0-08 — ArchUnit rule set (layering, no cross-service deps, outbox-only publishing, no `now()` in domain/app). · doc 02 §2.6 · deps: P0-07
- [ ] P0-09 — `infra/` SAM/CDK template (DynamoDB tables, SQS queues, SNS topics, Cognito, CloudWatch, EventBridge Scheduler, CloudFront), `.env.example`, seed scripts; **decide D-20, D-22; verify VERIFY-14,19,20,21,23,24,26**. · doc 04 §1–6, §8 · deps: P0-01
- [ ] P0-10 — Lambda packaging: Maven shade/assembly for Lambda fat JARs, `new-service.sh` generates handler skeleton; SnapStart config. · doc 04 §3 · deps: P0-02, VERIFY-22
- [ ] P0-11 — CloudWatch dashboards and alarms: 4 dashboards (feeds, alerts, fanout, platform) provisioned via CDK/SAM. · doc 04 §7 · deps: P0-09
- [ ] P0-12 — GitHub Actions: `backend.yml`, `deploy.yml` (SAM deploy, manual). · doc 04 §9 · deps: P0-02, P0-10
- [ ] P0-13 — `tools/wiremock-upstream` skeleton + scenario switch endpoint + flood/history fixtures (`calm`, `flood-watch/warning/severe`). · doc 04 §11 · deps: P0-09
- [ ] P0-14 — `tools/new-service.sh` generator (module, yml, DynamoDB table definitions in SAM/CDK, SQS queue, Lambda handler, README, ArchUnit + health IT). · doc 02 §3 · deps: P0-05, P0-06, P0-08, P0-10
- [ ] P0-15 — Frontend scaffold: Vite + React + TS strict + Tailwind + ESLint/Prettier + Vitest + scripts. · doc 17 §1–2 · deps: P0-01
- [ ] P0-16 — Store, `baseApi` (+reauth), `auth`/`ui`/`map`/`broadcastDraft`/`resident` slices, env, router skeleton, listener middleware. · doc 17 §3–4 · deps: P0-15
- [ ] P0-17 — Design primitives: SeverityBadge, ProvenanceBadge, DataAge, StaleBanner, ForecastBandChart, MapView, AttributionFooter + tests. · doc 17 §5 · deps: P0-16
- [ ] P0-18 — i18n (en/hi/mr) scaffolding + key-parity script + review-status file. · doc 17 §6 · deps: P0-16
- [ ] P0-19 — PWA shell: manifest, service worker (Workbox), install prompt, offline banner. · doc 17 §7 · deps: P0-16
- [ ] P0-20 — MSW handlers skeleton + `frontend.yml` CI (lint/type/test/build/bundle budget). · doc 17 §8, doc 04 §9 · deps: P0-17
- [ ] **P0-21 — GATE P0:** `sam deploy` (or `cdk deploy`) succeeds; LocalStack DynamoDB/SQS healthy; `./mvnw verify` green; `npm run build` green; generator creates a working service; ArchUnit enforced in CI. · deps: all P0

---

## PHASE 1 — MVP (flood-ingest, rules-engine, alert-fanout, subscriber-registry + frontend slice)

### subscriber-registry `doc 05`
- [ ] P1-01 — DynamoDB tables (location, coordinator, subscription, consent, telegram-link, outbox/inbox) + entity models/repos. · deps: P0-14
- [ ] P1-02 — Locations CRUD (ADMIN), CSV import, seed bootstrap, `LocationUpserted/Deleted` via SQS/SNS. · deps: P1-01
- [ ] P1-03 — Auth: Cognito user pool integration, login/refresh/logout, bootstrap admin, lockout, JWT issuing. · deps: P1-01, P0-06, VERIFY-25
- [ ] P1-04 — Public subscriptions API (create/get/update/delete+erase), management token, consent enforcement, `SubscriptionChanged` via SNS. · deps: P1-02
- [ ] P1-05 — Telegram deep-link tokens + long-poll `/start`,`/stop` handler (EventBridge-triggered Lambda). · deps: P1-04, VERIFY-10
- [ ] P1-06 — Web-push channel storage + VAPID public key endpoint. · deps: P1-04, VERIFY-11
- [ ] P1-07 — Internal `changes` API (cursor by version, tombstones) + `GET /internal/v1/locations`. · deps: P1-04
- [ ] P1-08 — Coordinator stats API (counts only, scope-enforced). · deps: P1-03
- [ ] P1-09 — Loadtest seeding endpoint (profile `loadtest`, 50k bulk). · deps: P1-04
- [ ] P1-10 — ITs green (LocalStack), OpenAPI, README, Lambda handler, SAM entry. · deps: P1-01…09

### flood-ingest `doc 06`
- [ ] P1-11 — DynamoDB tables + location replica + bootstrap (table → registry → seed CSV). · deps: P0-14, P1-02
- [ ] P1-12 — Open-Meteo client (batching, retry, breaker, call budget via DynamoDB, attribution) — **record VERIFY-02/16**. · deps: P1-11, VERIFY-02, VERIFY-16
- [ ] P1-13 — Threshold computation: Gumbel fit + GloFAS CSV import + strategy AUTO — **VERIFY-07**. · deps: P1-12
- [ ] P1-14 — Snap-candidates admin tool + verify flow. · deps: P1-13
- [ ] P1-15 — EventBridge-triggered run, exceedance shares (25/51→0.490 test), snapshots, retention, DynamoDB cache. · deps: P1-13
- [ ] P1-16 — `FloodSignal` SQS/SNS publish, FeedState + `FeedStatus` heartbeat, stale/last-good behaviour. · deps: P1-15
- [ ] P1-17 — Read APIs (points, forecast, public summary, feed-status, admin run-now) + ETag. · deps: P1-15
- [ ] P1-18 — WireMock/LocalStack ITs incl. resilience IT; README/Lambda/SAM. · deps: P1-16, P1-17

### rules-engine `doc 07`
- [ ] P1-19 — DynamoDB tables (rule_config, alert_state, alert_history, outbox/inbox). · deps: P0-14
- [ ] P1-20 — Domain: `FloodRule` + `SeverityStateMachine` (persistence, hysteresis, cooldown, stale guard) with ≥30 table-driven tests. · deps: P1-19
- [ ] P1-21 — Isolated SQS consumers (`rules-flood`, `rules-heat`, `rules-aq` separate Lambda triggers) + inbox idempotency. · deps: P1-20, P0-05
- [ ] P1-22 — `AlertEvent(DECISION)` SQS/SNS emission, expiry EventBridge rule (CANCEL). · deps: P1-21
- [ ] P1-23 — Status APIs, rules-config API (ADMIN), dry-run API. · deps: P1-22
- [ ] P1-24 — Isolation IT (blocked `rules-aq` SQS trigger), contract tests, README/Lambda/SAM. · deps: P1-22, P1-23

### alert-fanout `doc 08`
- [ ] P1-25 — DynamoDB tables + subscriber/location replica sync + 10-min reconciliation (EventBridge rule). · deps: P0-14, P1-07
- [ ] P1-26 — Planner (recipient filtering, keyset batches), dedupe (DynamoDB + TTL), auto-send/held-for-approval, quiet hours. · deps: P1-25
- [ ] P1-27 — Template renderer (en/hi/mr, mandatory provenance line, flood action texts) — hi/mr flagged for review. · deps: P1-26
- [ ] P1-28 — SQS delivery queues + priority simulation (SEVERE first via ordering attribute) + batching receipts writer. · deps: P1-26
- [ ] P1-29 — Telegram worker Lambda + DynamoDB token bucket (rate limiter), 429/403 handling. · deps: P1-28, VERIFY-10
- [ ] P1-30 — Web Push worker Lambda (VAPID, 404/410 cleanup). · deps: P1-28, VERIFY-11
- [ ] P1-31 — Mock sender Lambda + metrics (TTFD/TTLD). · deps: P1-28
- [ ] P1-32 — Broadcast preview/send APIs, previewId, idempotency, estimated seconds, share text. · deps: P1-27, P1-28
- [ ] P1-33 — History, deliveries summary, queue-status, held/approve APIs. · deps: P1-32
- [ ] P1-34 — Retry/backoff/DLQ + `DeliveryReceipt` events via SNS. · deps: P1-29…31
- [ ] P1-35 — ITs (rate-limit assertion, ordering, dedupe, registry-down, unsubscribe propagation), README/Lambda/SAM. · deps: P1-33, P1-34

### Edge
- [ ] P1-36 — CloudFront distribution: path routing directly to Lambda Function URLs per doc-03 §5 prefix table; block `/internal/*`; HTTPS-only; security headers; custom domain per D-19. · doc 04 §6 · deps: P1-10, P1-18, P1-24, P1-35, VERIFY-26

### Frontend MVP `docs 18, 19`
- [ ] P1-37 — Coordinator login (Cognito Hosted UI or SDK), shell, route guards, silent refresh. · deps: P0-16, P1-03 (or MSW)
- [ ] P1-38 — Hazard map (flood), side panel, held-alerts approval — **VERIFY-15**. · deps: P1-37
- [ ] P1-39 — Point detail + ForecastBandChart + thresholds + caveats. · deps: P1-38
- [ ] P1-40 — Broadcast composer (preview→send, progress, idempotency) + WhatsApp share — **VERIFY-12**. · deps: P1-38
- [ ] P1-41 — History + feed/system status pages. · deps: P1-40
- [ ] P1-42 — Resident status home + location chooser (nearest) + disclaimers. · deps: P0-17
- [ ] P1-43 — Subscribe wizard + consent + token handling. · deps: P1-42
- [ ] P1-44 — Telegram connect flow, web-push enable (iOS guidance), my-alerts management/erase. · deps: P1-43
- [ ] P1-45 — Offline last-known status + data-age/stale handling + install prompt. · deps: P1-42
- [ ] P1-46 — Complete en/hi/mr strings + parity check — **VERIFY-18 (draft)**. · deps: P1-44
- [ ] P1-47 — A11y (axe), Lighthouse PWA/perf, bundle ≤200 KB gzip. · deps: P1-46

### MVP acceptance
- [ ] P1-48 — **E2E demo:** `flood-severe` scenario (mock upstream) → decision → Telegram (test bot) + web push + mock delivery → dashboard shows status/provenance/data age. Script `tools/e2e/mvp-demo.sh`. · doc 20 §3 · deps: P1-36, P1-47
- [ ] P1-49 — **50,000 mock-subscriber load test** (TTFD/TTLD, zero loss/dup) + Telegram bucket-rate proof; document results. · doc 20 §2D · deps: P1-35, P1-09
- [ ] P1-50 — Deploy MVP to AWS (SAM/CDK deploy) + frontend to Amplify Hosting; smoke tests; record Lambda invocation metrics in CloudWatch. · doc 04 · deps: P1-48, VERIFY-14, VERIFY-19
- [ ] **P1-51 — GATE P1:** acceptance A (flood/state machine), B (provenance), C1/C2/C4 (isolation, registry-down), D partial pass; evidence pasted below. · deps: P1-48…50

---

## PHASE 2 — Official + community + edge

### cap-ingest `doc 09`
- [ ] P2-01 — DynamoDB tables + replica + feed client (conditional GET, XXE-safe) — **VERIFY-09 live test, fixtures**. · deps: P1-51
- [ ] P2-02 — CAP 1.2 parser + XSD validation — **VERIFY-17**. · deps: P2-01
- [ ] P2-03 — Hazard/severity mapping + location matching (polygon/circle/geocode/name). · deps: P2-02
- [ ] P2-04 — `OfficialAlertSignal` publishing via SNS, Update/Cancel/expiry, feed state. CAP feed = static JSON/XML generated and uploaded to S3/Amplify. · deps: P2-03
- [ ] P2-05 — APIs, tests (LocalStack), README/Lambda/SAM. · deps: P2-04

### report-intake `doc 10`
- [ ] P2-06 — DynamoDB tables + replica + validation rules. · deps: P1-51
- [ ] P2-07 — `POST /reports` + idempotency + reporter key + DynamoDB rate limits. · deps: P2-06
- [ ] P2-08 — Inbox/summary/moderation APIs (scope-enforced). · deps: P2-07
- [ ] P2-09 — `report.accepted/updated` events via SNS + public recent-count. · deps: P2-08
- [ ] P2-10 — Tests (validation matrix, rate limit, PII hygiene, burst), README/Lambda/SAM. · deps: P2-09

### signal-fusion `doc 11`
- [ ] P2-11 — DynamoDB tables + caches + SQS consumers (decision/report/official). · deps: P2-04, P2-09
- [ ] P2-12 — Fusion engine (pure domain) + provenance truth-table tests (≥24). · deps: P2-11
- [ ] P2-13 — `AlertEvent(FUSED)` emission on change + expiry EventBridge rule + fusion_log in DynamoDB. · deps: P2-12
- [ ] P2-14 — Status + explain APIs. · deps: P2-13
- [ ] P2-15 — **Rebind fanout to fused SQS queue (config only)** + E2E; README/Lambda/SAM. · deps: P2-14

### api-gateway / edge `doc 12`
- [ ] P2-16 — CloudFront distribution update: Cognito JWT authorizer, route table, CORS — **VERIFY-25, VERIFY-26**. · deps: P1-51
- [ ] P2-17 — Cognito role rules + public/protected matrix. · deps: P2-16
- [ ] P2-18 — DynamoDB rate limits, circuit breakers + fallbacks, platform health route via Lambda. · deps: P2-17
- [ ] P2-19 — CloudFront P2 cut-over + route contract tests + README. · deps: P2-18

### Frontend P2 `docs 18, 19`
- [ ] P2-20 — Provenance badges + evidence in map/detail (fusion status). · deps: P2-14
- [ ] P2-21 — Reports inbox + moderation (optimistic w/ rollback). · deps: P2-08
- [ ] P2-22 — Official alerts panel. · deps: P2-05
- [ ] P2-23 — Broadcast-from-report / from-official prefill. · deps: P2-21, P2-22
- [ ] P2-24 — Platform health via CloudFront in feeds page. · deps: P2-18
- [ ] P2-25 — Resident report flow (offline queue) + nearby-reports count. · deps: P2-09

### Phase 2 acceptance
- [ ] P2-26 — Chaos: disable `cap-ingest`, `report-intake`, `signal-fusion` Lambda triggers individually → flood path behaviour as specified; CloudFront fallback test. · doc 20 §2C · deps: P2-15, P2-19
- [ ] P2-27 — Deploy Phase 2 (verify DynamoDB + SQS cost stays in free tier). · deps: P2-26
- [ ] **P2-28 — GATE P2:** provenance upgrade E2E (`sachet-flood-official`, reports corroboration), reports never escalate, XXE test, rate limits, updated evidence. · deps: P2-20…27

---

## PHASE 3 — Heat, air quality, composer, audit

### heat-ingest `doc 13` / rules `doc 07`
- [ ] P3-01 — DynamoDB tables/replica/Open-Meteo weather+archive client — **VERIFY-03**. · deps: P2-28
- [ ] P3-02 — Normals computation (1991–2020, ±7-day smoothing, leap-year) + tests. · deps: P3-01
- [ ] P3-03 — EventBridge-triggered runs (twice daily), departure calc, `HeatSignal` publish via SNS, shared Open-Meteo budget. · deps: P3-02
- [ ] P3-04 — APIs + CloudWatch metrics. · deps: P3-03
- [ ] P3-05 — Tests/README/Lambda/SAM. · deps: P3-04
- [ ] P3-11 — `HeatRule` (IMD-style, boundary tests 40/45/47, 2-day rule) — **VERIFY-06**. · deps: P3-03, VERIFY-06

### aq-ingest `doc 14`
- [ ] P3-06 — DynamoDB tables/replica/Open-Meteo AQ client — **VERIFY-04**. · deps: P2-28
- [ ] P3-07 — Rolling averages (24h/8h, CO unit, validity rules) domain + tests. · deps: P3-06
- [ ] P3-08 — OpenAQ client + station cache + call budget (DynamoDB) — **VERIFY-08**. · deps: P3-06
- [ ] P3-09 — EventBridge scheduling + `AqSignal` publish via SNS + dual feed states. · deps: P3-07, P3-08
- [ ] P3-10 — APIs/tests/README/Lambda/SAM (fault-isolation IT). · deps: P3-09
- [ ] P3-12 — `AqiCalculator` (**45→75, 150→323**, all breakpoints) — **VERIFY-05**. · deps: VERIFY-05
- [ ] P3-13 — `AqRule` + model-vs-observed cross-check (`autoSend=false` on disagreement). · deps: P3-12, P3-09
- [ ] P3-14 — rules-engine heat/aq SQS triggers wired + config + docs. · deps: P3-11, P3-13

### alert-composer `doc 15`
- [ ] P3-15 — DynamoDB tables + template store + seed templates (flood/heat/AQ × 3 langs, needs_review). · deps: P2-15
- [ ] P3-16 — Composition engine + mandatory provenance enforcement + placeholder whitelist. · deps: P3-15
- [ ] P3-17 — CAP 1.2 XML builder + XSD validation tests — **VERIFY-17**. CAP feed = static file uploaded to S3/Amplify. · deps: P3-16
- [ ] P3-18 — `AlertEvent(COMPOSED)` events via SNS + static CAP feed generation + template APIs. · deps: P3-17
- [ ] P3-19 — **Rebind fanout to composed SQS queue (config only)** + E2E; README/Lambda/SAM. · deps: P3-18

### audit-evaluation `doc 16`
- [ ] P3-20 — DynamoDB tables + read-only SQS taps + batching writer + secret stripping. · deps: P2-28
- [ ] P3-21 — Trace + events APIs. · deps: P3-20
- [ ] P3-22 — Observed-event CSV import + backtest engine (pure domain, hand-built scenario tests). · deps: P3-20
- [ ] P3-23 — Backtest job (EventBridge-triggered) + APIs (n<5 suppression, caveats). · deps: P3-22
- [ ] P3-24 — KPIs + public feedback endpoint. · deps: P3-21
- [ ] P3-25 — Tests (tap resilience), README/Lambda/SAM. · deps: P3-23, P3-24

### Channels & frontend P3
- [ ] P3-26 — Email channel Lambda (SMTP, coordinator digests). · doc 08 · deps: P3-19
- [ ] P3-27 — Hazard toggle (heat/AQ) + detail tabs. · deps: P3-14
- [ ] P3-28 — Evaluation page (ADMIN). · deps: P3-23
- [ ] P3-29 — Settings: rule params, snap tool UI. · deps: P3-14
- [ ] P3-30 — Template editor with preview + needs-review flags. · deps: P3-18
- [ ] P3-31 — Resident multi-hazard tabs + email/digest preferences + polish. · deps: P3-27

### Phase 3 acceptance
- [ ] P3-32 — Full chaos suite (disable each of aq/heat/cap/report/audit/composer Lambda triggers) + 50k load re-run + verify DynamoDB/SQS still within free tier. · doc 20 §2C–D · deps: P3-19, P3-25
- [ ] P3-33 — Deploy full stack to AWS; record Lambda invocation metrics in CloudWatch; merge Lambda functions if approaching free-tier limits (audit → composer → edge). · deps: P3-32
- [ ] **P3-34 — GATE P3:** acceptance A–E all pass; backtest report generated from logged alerts. · deps: P3-26…33

---

## PHASE 4 — Hardening & release

- [ ] P4-01 — Security review (auth matrix, Cognito tokens, SQLi/XSS/XXE, secrets scan, dependency audit, CloudFront headers). · doc 20 §2F
- [ ] P4-02 — **DPDP Act 2023 diligence** (VERIFY-13) → conclusions recorded, consent text finalized. · doc 21 §4
- [ ] P4-03 — Performance + memory tuning (JVM flags, DynamoDB read patterns, SQS batch size), Lighthouse/bundle re-check. · doc 04 §2
- [ ] P4-04 — Backup + **restore drill** executed and timed (DynamoDB export to S3 + restore). · doc 04 §10
- [ ] P4-05 — Native review of hi/mr translations (VERIFY-18); clear `needs_review` flags. · doc 21 §2
- [ ] P4-06 — README, Limitations, "real vs simulated", runbook (on-call, redeploy, key rotation), architecture diagram export. · doc 21 §7
- [ ] P4-07 — Demo script + seed data + scenario walkthrough video script. · doc 20 §3
- [ ] P4-08 — Pilot kit: coordinator guide, consent/notice copy, metrics sheet (doc 20 §2H).
- [ ] P4-09 — Backtest on real observed events (if available) with honest reporting.
- [ ] P4-10 — Full acceptance run A–H; paste results below.
- [ ] **P4-11 — RELEASE GATE:** all acceptance criteria evidenced, VERIFY register has no `TODO` for shipped features, tag `v1.0.0`. At month 12 post-deploy: migrate frontend from Amplify to Cloudflare Pages (git-reconnect per D-23).

---

## 🧾 EVIDENCE LOG (paste results at each gate)
_(Format: date · task · command · result summary · link to report/log)_

## 🗒 CHANGE LOG
_(date · task · short note)_

2026-10-20 · Architecture migration · Replaced Oracle VM+Docker Compose with AWS Lambda+EventBridge; RabbitMQ→SQS/SNS; Redis+PostgreSQL→DynamoDB; Caddy→CloudFront+Cognito; Prometheus/Grafana→CloudWatch; Cloudflare Pages→Amplify(12mo)→Cloudflare Pages; Docker Compose deploy→AWS SAM/CDK. Platform renamed MHAR→Kestrel.

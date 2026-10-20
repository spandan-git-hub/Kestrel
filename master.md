# MASTER.md — Kestrel (Multi-Hazard Alert Relay)

> **Paste ONLY this file as the prompt — it tells the agent what to do next** (§0). Optional one-line overrides are listed in §0.2.
> Task-level detail lives in `docs/`. Progress lives in `checklist.md`.

---

## 0. How this file drives the work (read first, every time)

**You (the agent) are self-directing. The user normally pastes ONLY this file.** Anything the user adds after it is an optional override (see 0.2). Never ask the user "what should I do?" when the checklist already says.

### 0.1 Default behaviour (nothing added after this file)
1. Open `checklist.md`. Read **Current Status**, **Blockers**, **Decision Log**, **Verification Register**.
2. If a Blocker is open, resolve it with the user first (ask one concise question, then stop).
3. Select the **first unchecked task whose dependencies are all ticked** (or the task named in "Next task"). If it is a **GATE** task, run the gate procedure (0.3).
4. Open ONLY the doc(s) the task references (see §10). Do not load other docs.
5. **Verification step:** if the task lists a `VERIFY-nn` that is still `TODO`, do that verification FIRST using official docs/web search only: record exact names, limits, units, URL and date in the Verification Register (`DONE` or `CANNOT-VERIFY` + the configurable assumption). Then continue the task in the same turn.
6. Implement exactly the task scope. No extra features, no refactors of unrelated code, no new services, no new dependencies outside §3 without a Decision Log entry.
7. Run build + tests (§9 Definition of Done). Fix until green. Never skip, disable or mock away a required test.
8. Update `checklist.md`: tick the box with date + one-line evidence, update **Current Status** and progress counters, add new decisions / verifications / blockers.
9. **Continue to the next task** in the same service/phase if the context budget allows (max 4 tasks per turn, same service only, run tests after each). Stop at any gate, any failing test you cannot fix, any open question, or a service/phase change.
10. End every reply with exactly this block:
```
DONE: <task IDs ticked>
FILES: <changed files>
COMMANDS: <build/test commands + pass/fail>
CHECKLIST: <what changed>
NEXT: <next task ID> — start a NEW chat and paste master.md again if the service or phase changes
```

### 0.2 Optional overrides the user may append (after this file)
| User adds | You do |
|---|---|
| `Task: P1-12` (or a range `P1-01..P1-04`, same service) | Do those tasks (steps 4–10) |
| `Status` | Read checklist; report phase, progress counts, blockers, open VERIFY/D items, next 3 tasks. Change nothing |
| `Verify: VERIFY-02, VERIFY-03` | Do only step 5 for those items; write no application code |
| `Fix: <pasted error/test output>` (+ optional task ID) | Root cause in ≤3 lines, minimal fix, re-run tests, note in checklist Change Log; no unrelated edits |
| `Review: Phase 1` | Gate procedure (0.3) |
| `Decide: D-19 = <choice>` | Record in Decision Log, then continue |

### 0.3 Gate procedure (tasks named GATE, or `Review:`)
Check every task in the phase is ticked with evidence; run full build, integration tests and the phase E2E/chaos/load tests; compare against `docs/20_testing_and_acceptance.md`; paste results into the Evidence Log; tick the gate only if every acceptance criterion passes, otherwise list gaps as new unchecked tasks (IDs `Pn-xxG`) and stop.

### 0.4 Rules of engagement
- If a task is ambiguous or a doc contradicts this file, **this file wins**; log the conflict under Blockers and proceed with the most conservative reading.
- If blocked, stop, write the blocker in the checklist and ask. Never fake data, silently stub a requirement, or tick a task with failing tests.
- Commit messages: `feat(flood-ingest): exceedance share calculation [P1-15]`. If you can run git, commit after each ticked task; otherwise state the suggested message.
- Context hygiene: you re-read this file and the checklist at the start of every turn; do not rely on memory of earlier turns.

---

## 1. Product in one paragraph

**Kestrel** is a free-tier, software-only multi-hazard alert relay for India. A ward/village coordinator gets one reliable place that warns about **floods, extreme heat and bad air**, and can broadcast a locally confirmed, plain-language, regional-language alert in one tap. Residents subscribe by location, hazard and language and receive alerts on **Telegram** and **web push**; a **WhatsApp share button** lets the coordinator forward alert text to community groups.

**POV:** *A ward or village coordinator needs an early, locally confirmed and trusted warning in the residents' own language, because official warnings often arrive late or from sources people don't trust.*

**The real gap is last-mile delivery and trust**, not raw forecasting. India already has an official system (**SACHET**, NDMA, CAP 1.2). Kestrel **consumes SACHET as an input** and adds model-based early signals, community ground reports, local-language delivery and provenance labelling. It is **never** an official warning source.

### What Kestrel is NOT
- Not an official warning system. Never imply it is.
- Not a replacement for IMD / CWC / NDMA / SACHET.
- Not using paid services (no SMS, no WhatsApp Business API, no paid weather APIs).
- Not claiming forecast accuracy it has not measured.

---

## 2. Non-negotiable principles

1. **Provenance on everything.** Every alert, status and message carries one label: `MODEL_ONLY`, `MODEL_PLUS_REPORTS` or `OFFICIAL_CONFIRMED`. Model-derived text must say it is guidance needing local confirmation, not an official warning.
2. **Fault isolation.** A broken/slow air-quality or heat feed must **never** delay a flood alert. Separate Lambda functions, separate SQS queues, separate DynamoDB tables/prefixes per service.
3. **Show uncertainty and data age.** Cache last-good data; every data view shows "data age" and flags staleness. Never present stale data as fresh.
4. **Free-only.** Free tiers, open APIs, open-source. No feature may require a paid service. Mock senders are used for volume tests.
5. **Consent and minimal data.** Opt-in only, no names/phones collected, consent logged, one-call unsubscribe + erase.
6. **Severity is never colour alone.** Always colour + icon + text.
7. **Don't invent facts.** Anything marked `⚠ VERIFY-nn` must be checked against the live official doc/API **before** coding against it. If it can't be verified, make it configurable, document the assumption, and log it in the Verification Register.
8. **Measure, don't promise.** Forecast quality is reported as measured detection rate / false-alarm ratio by lead time (backtests), never as a guarantee.
9. **Low-bandwidth friendly.** Mobile-first PWA, light payloads, works on slow connections.
10. **Source-of-truth ownership.** Each service owns its data. No cross-service DynamoDB table reads outside event-driven replicas. Cross-service data moves by events (SQS/SNS with local DynamoDB replicas) or REST (via CloudFront + Lambda Function URLs).

---

## 3. Locked technology stack

| Layer | Choice |
|---|---|
| Language/runtime | **Java 21 (LTS)**, Maven 3.9 (wrapper), monorepo multi-module |
| Backend framework | **Spring Boot 3.5.x** (newest patch) + **Spring Cloud 2025.0.x** BOM `⚠ VERIFY-01` |
| Compute | **AWS Lambda** (one function per microservice); ingest services triggered by **EventBridge Scheduler**; user-facing functions use **SnapStart** `⚠ VERIFY-22` |
| Web | Spring MVC (servlet adapter for Lambda) for all services **except** the edge layer (CloudFront + Lambda Function URLs) |
| Persistence | **DynamoDB** (primary store for all services — last-good data + TTL for cache entries, full records otherwise). Alternative: **Aurora DSQL** (keeps SQL, no extensions/triggers) `⚠ VERIFY-23` |
| Messaging | **SQS** queues (one per consumer); **SNS** topics where one event feeds several services. Decision D-02-revised |
| Cache / rate-limit / dedupe | **DynamoDB with TTL** (replaces Redis); call-budget counters in DynamoDB |
| Resilience | Resilience4j (retry, circuit breaker, rate limiter, bulkhead) |
| Observability | **CloudWatch** (metrics, logs, alarms). 10 custom metrics, 10 alarms, 5 GB logs on free tier. Micrometer → CloudWatch bridge in each Lambda |
| API docs | springdoc-openapi per service |
| Auth / edge | **Amazon Cognito** (10,000 monthly active users free) + **CloudFront** in front of Lambda Function URLs |
| Testing | JUnit 5, AssertJ, Mockito, **Testcontainers** (LocalStack for DynamoDB/SQS/SNS), WireMock, ArchUnit, Awaitility |
| Style | Spotless (google-java-format), no Lombok (use records), constructor injection only |
| Frontend | **React 18+ / TypeScript (strict) / Vite**, **Redux Toolkit + RTK Query**, React Router, Tailwind CSS, react-i18next, Leaflet (+OSM tiles), vite-plugin-pwa (Workbox), Vitest + React Testing Library + MSW, Playwright (e2e) |
| Delivery channels | Telegram Bot API (primary), Web Push (VAPID, standards-based), Email SMTP (Phase 3, optional), Mock sender (tests), WhatsApp **share link** (human-in-the-loop). CAP feed = static file served from Amplify/Cloudflare Pages host |
| Hosting (backend) | **AWS Lambda** + **EventBridge Scheduler** — free indefinitely (1M requests/month + 400,000 GB-s/month for Lambda; 14M invocations/month for Scheduler) |
| Hosting (frontend) | **AWS Amplify Hosting** (first 12 months — 1,000 build minutes/month) → **Cloudflare Pages** (permanent, 500 builds/month, no-expiry free plan). Switch at month 12 is a git-reconnect, not a rewrite `⚠ VERIFY-14` |
| Infrastructure as code | **AWS SAM** or **AWS CDK** (whole setup in one file; no extra cost beyond the services). Decision D-22 |
| Scheduler | **EventBridge Scheduler** triggers all ingest services (replaces Docker Compose cron + `@Scheduled` inside the VM) |

Do not introduce other frameworks (no Kafka, no Kubernetes, no GraphQL, no MUI, no Lombok, no Python services) without a Decision Log entry approved by the user.

---

## 4. Architecture snapshot

```
External inputs ──pull──► [flood|heat|aq|cap]-ingest (Lambda, triggered by EventBridge Scheduler)
 (Open-Meteo, GloFAS,           │
  OpenAQ, SACHET)               ├──► SQS signal queues ──► rules-engine (Lambda) ──► SQS alert queue ──► signal-fusion (Lambda) ──► alert-composer (Lambda) ──► alert-fanout (Lambda) ──► Telegram / Web Push / Email / Mock
                                │                                ▲                                                                                                      │
                         report-intake (Lambda) ─────────────────┘ (joins at signal-fusion, P2)                                                                       ▼
                                                                                                                                                                    Residents

React PWA (Coordinator Dashboard + Resident page) ──HTTPS──► CloudFront ──► Lambda Function URLs ──► per-service Lambda
CAP feed = static JSON/XML file served from Amplify / Cloudflare Pages (same host as frontend)
Shared infra: DynamoDB (per-service table prefix) · SQS queues · SNS topics · Cognito user pool · CloudWatch · EventBridge
```

| # | Service | Lambda function name | DynamoDB prefix | Phase | One-line job |
|---|---|---|---|---|---|
| 1 | `subscriber-registry` | `kestrel-subscriber-registry` | `kestrel_registry_` | **MVP** | Locations, subscribers, consent, coordinator auth (Cognito) |
| 2 | `flood-ingest` | `kestrel-flood-ingest` | `kestrel_flood_` | **MVP** | Open-Meteo ensemble pull → exceedance shares |
| 3 | `rules-engine` | `kestrel-rules-engine` | `kestrel_rules_` | **MVP** | Thresholds, persistence, severity state machine, cooldown |
| 4 | `alert-fanout` | `kestrel-alert-fanout` | `kestrel_fanout_` | **MVP** | Queue + send within channel limits, broadcasts |
| 5 | `cap-ingest` | `kestrel-cap-ingest` | `kestrel_cap_` | 2 | Parse SACHET CAP → official signal; CAP feed = static file |
| 6 | `report-intake` | `kestrel-report-intake` | `kestrel_report_` | 2 | Community reports, validation, spam limits |
| 7 | `signal-fusion` | `kestrel-signal-fusion` | `kestrel_fusion_` | 2 | Merge model + reports + official → one status/provenance |
| 8 | `api-gateway` | CloudFront + Lambda Function URLs | — | 2 | Single entry, Cognito JWT check, rate limit |
| 9 | `heat-ingest` | `kestrel-heat-ingest` | `kestrel_heat_` | 3 | Heat forecasts + normals |
| 10 | `aq-ingest` | `kestrel-aq-ingest` | `kestrel_aq_` | 3 | AQ model + rolling averages + OpenAQ cross-check |
| 11 | `alert-composer` | `kestrel-alert-composer` | `kestrel_composer_` | 3 | CAP-shaped, localized messages; static CAP feed file |
| 12 | `audit-evaluation` | `kestrel-audit-evaluation` | `kestrel_audit_` | 3 | Audit log, backtests (POD/FAR), feedback |

Infra: DynamoDB (on-demand, free 25 GB + 25 RCU/WCU provisioned) · SQS (1M requests/month free) · SNS (1M publishes/month free) · Cognito (10,000 MAU free) · CloudWatch · EventBridge Scheduler · CloudFront (1 TB + 10M requests free).
The frontend is **not** a microservice. Full detail: `docs/01_architecture.md`.

**Pipeline stages are contract-stable:** MVP `rules-engine → alert-fanout`; Phase 2 inserts `signal-fusion`; Phase 3 inserts `alert-composer`. The single `AlertEvent` contract and a config-only SQS queue binding (`fanout.input-queue`) make these insertions require **no code change in alert-fanout**.

---

## 5. Repository layout (monorepo `kestrel/`)

```
kestrel/
├─ master.md  checklist.md  docs/
├─ backend/
│  ├─ pom.xml                      (parent: BOM, plugin mgmt)
│  ├─ libs/ common-events  common-core  common-outbox  common-security  common-testing
│  └─ services/ <service-name>/    (each: pom.xml, src/, sam-template.yaml or cdk/, README.md)
├─ frontend/                       (Vite React TS app)
├─ infra/  sam-template.yaml (or cdk/)  dynamodb/  sqs-sns/  cognito/  cloudwatch/  scripts/
├─ tools/  wiremock-upstream/  loadtest/  chaos/  new-service.sh
└─ .github/workflows/
```
Base Java package: `com.kestrel.<service>` (e.g. `com.kestrel.floodingest`); libs `com.kestrel.common.*`.
A service may depend on `libs/*` only — **never on another service**. ArchUnit enforces this.

---

## 6. Backend standards (all services)

**Structure per service** (hexagonal-lite): `api` (handlers/DTOs) · `app` (use cases) · `domain` (pure logic, **no Spring imports**) · `infra` (dynamodb, sqs, sns, http clients) · `config`.
**Domain logic must be pure functions/records** so it is unit-testable in isolation (rules, AQI, exceedance, Gumbel).

**API:** `/api/v1/**` public/coordinator (via CloudFront); `/internal/v1/**` service-to-service only (never routed externally via CloudFront). Errors are **RFC 7807** `application/problem+json` with `traceId`. Pagination `page,size` → `{items,page,size,total}`. All timestamps ISO-8601 **UTC** (`Instant`); display conversion is a frontend concern. `Idempotency-Key` header on non-idempotent POSTs that create broadcasts/reports.

**Data:** DynamoDB tables named `kestrel_<service>_<entity>` (on-demand billing mode, free tier). Each service owns its tables; **no cross-service DynamoDB access**. For services migrating from PostgreSQL to Aurora DSQL: no extensions, no triggers. Schema migrations for Aurora DSQL via Flyway (verify compatibility `⚠ VERIFY-23`).

**Messaging:** All publishes go through **`common-outbox`** (transactional outbox pattern adapted for DynamoDB — write outbox item + business item in a DynamoDB transaction, relay polls and publishes to SQS/SNS). All consumers are **idempotent** (inbox item in DynamoDB keyed by `eventId`). At-least-once delivery assumed. Every consumer SQS queue has a DLQ and bounded retry with backoff.

**Resilience:** every outbound HTTP call has timeout, retry-with-backoff+jitter, circuit breaker; ingest services record `FeedState` (HEALTHY/DEGRADED/DOWN) and keep last-good snapshots in DynamoDB (TTL). Respect external rate limits with a DynamoDB-backed call budget (Open-Meteo: cap at 8,000/day and 500/min `⚠ VERIFY-02`).

**Security:** Cognito JWT validated at CloudFront (or by `common-security` in Lambda); roles `COORDINATOR`, `ADMIN`; coordinators are scoped to `locationIds`. Residents use an opaque `X-Subscriber-Token` (stored hashed in DynamoDB). No secrets in git; `.env.example` only. Validate all input; disable XXE on every XML parser; never log PII/tokens; Lambda function URL health/metrics endpoints never routed externally by CloudFront.

**Observability:** every Lambda publishes CloudWatch custom metrics via Micrometer. Mandatory metrics listed per service doc and in `docs/04_infrastructure.md`. Log JSON with `service`, `traceId`, `eventId`, `alertId`, `locationId` to CloudWatch Logs.

**JVM/Lambda:** `JAVA_TOOL_OPTIONS=-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:+TieredCompilation -XX:TieredStopAtLevel=1`; use **SnapStart** for user-facing Lambda functions (subscriber-registry, alert-fanout broadcast API) to reduce cold-start latency `⚠ VERIFY-22`.

**Testing minimums per task:** unit tests for domain; LocalStack (Testcontainers) integration test for each DynamoDB/SQS/SNS interaction; WireMock for every external API; contract test for every published/consumed event against the JSON Schemas; ≥80% line coverage on `domain` and `app` packages.

---

## 7. Frontend standards

- **Redux Toolkit** store with: `auth`, `ui`, `map`, `broadcastDraft`, `resident` slices; **one `baseApi` (RTK Query) extended with `injectEndpoints`** per backend domain. Server state lives in RTK Query only — never copy it into slices. Side effects via `createListenerMiddleware`.
- TypeScript `strict`, no `any`; API types generated from service OpenAPI with `openapi-typescript`.
- Routes: `/` resident, `/coordinator/*` dashboard (role-guarded). Code-split per route; target initial JS ≤ 200 KB gzip.
- Mobile-first, accessible (WCAG AA contrast, keyboard, aria-live for alerts), `SeverityBadge` = colour + icon + text. `ProvenanceBadge` and `DataAge` are mandatory wherever a status is shown.
- i18n: `en`, `hi`, `mr` from day one (all strings via i18next; no hard-coded text). Translations flagged `NEEDS NATIVE REVIEW` until reviewed `⚠ VERIFY-18`.
- Charts: hand-rolled lightweight SVG (forecast band vs 2/5/20-yr thresholds). Map: Leaflet. No heavy UI kits.
- Access token in memory (Redux); refresh token in httpOnly cookie (same-site subdomains). Resident subscriber token in `localStorage`.
- Develop against **MSW** mocks first, then switch to the real API; both must work.
- Required attribution footer (Open-Meteo, CAMS, OpenAQ, OpenStreetMap) — see `docs/21_ethics_compliance_and_attribution.md`.
- **CAP feed** link on resident page points to the static file hosted on Amplify/Cloudflare Pages (same domain as frontend).

---

## 8. Canonical enums (use exactly these)

`Hazard`: `FLOOD` `HEAT` `AIR_QUALITY` · `Severity`: `NONE` `WATCH` `WARNING` `SEVERE` · `Provenance`: `MODEL_ONLY` `MODEL_PLUS_REPORTS` `OFFICIAL_CONFIRMED` · `Channel`: `TELEGRAM` `WEB_PUSH` `EMAIL` `MOCK` · `Language`: `en` `hi` `mr` · `CapMsgType`: `ALERT` `UPDATE` `CANCEL` · `FeedState`: `HEALTHY` `DEGRADED` `DOWN` · `ReportCategory`: `WATER_RISING` `ROAD_BLOCKED` `NEED_HELP` `EXTREME_HEAT` `BAD_AIR` `OTHER` · `ReportStatus`: `PENDING` `VERIFIED` `REJECTED` · `RegionType`: `PLAINS` `COASTAL` `HILLY`.

---

## 9. Definition of Done (every task)

- [ ] Compiles with `./mvnw -B verify` (backend) / `npm run lint && npm run typecheck && npm test && npm run build` (frontend) — **all green**.
- [ ] Tests required by the task's acceptance criteria exist and pass; no skipped/disabled tests.
- [ ] DynamoDB table definitions (CDK/SAM) or Aurora DSQL migration present; OpenAPI visible; health + metrics present in CloudWatch.
- [ ] Service `README.md` updated (deploy, config, endpoints, events).
- [ ] Config via `application.yml` + env overrides; sensible defaults; no magic numbers in code.
- [ ] No `TODO`/`FIXME` left without a matching checklist item.
- [ ] `checklist.md` updated (tick, date, note, next task).
- [ ] Conventional commit message: `feat(flood-ingest): exceedance share calculation [P1-15]`.

**When blocked:** stop, write the blocker in `checklist.md`, and ask. Never fake data, stub away a requirement silently, or mark a task done with failing tests.

---

## 10. Doc map

| Need | Read |
|---|---|
| System design, flows, ownership | `docs/01_architecture.md` |
| Maven/monorepo, shared libs, conventions | `docs/02_backend_foundation.md` |
| Events, SQS/SNS topology, DynamoDB ownership, REST rules | `docs/03_data_contracts.md` |
| SAM/CDK, observability (CloudWatch), CI, deploy | `docs/04_infrastructure.md` |
| Services | `05_svc_subscriber-registry` · `06_svc_flood-ingest` · `07_svc_rules-engine` · `08_svc_alert-fanout` · `09_svc_cap-ingest` · `10_svc_report-intake` · `11_svc_signal-fusion` · `12_svc_api-gateway` · `13_svc_heat-ingest` · `14_svc_aq-ingest` · `15_svc_alert-composer` · `16_svc_audit-evaluation` |
| Frontend | `17_frontend_foundation` · `18_frontend_coordinator-dashboard` · `19_frontend_resident-pwa` |
| Tests / acceptance criteria | `docs/20_testing_and_acceptance.md` |
| Ethics, consent, attribution, disclaimers | `docs/21_ethics_compliance_and_attribution.md` |

---

## 11. Phases

**P0** Foundation (monorepo, libs, infra-as-code, CI, frontend scaffold) → **P1 MVP** (4 Lambda functions + resident/dashboard flood slice, deployed to AWS) → **P2** official + community (cap-ingest, report-intake, signal-fusion, CloudFront+Cognito edge) → **P3** heat + AQ + composer + audit → **P4** hardening, pilot kit, release.
Do not start a phase until the previous phase's **gate task** is checked.

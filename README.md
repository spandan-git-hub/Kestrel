# Kestrel — Multi-Hazard Alert Relay

> Free-tier, software-only multi-hazard alert relay for India. Delivers locally confirmed, plain-language, regional-language warnings for **floods**, **extreme heat**, and **bad air quality**.

---

## 1. Overview & Purpose

India's primary challenge in disaster management is often not raw meteorological forecasting, but **last-mile delivery and trust**. Official warnings can arrive late, in non-local dialects, or via channels residents do not monitor.

**Kestrel** is designed for ward and village coordinators. It aggregates open model signals (Open-Meteo ensemble flood/weather/air quality, OpenAQ, GloFAS) and official national alerts (**SACHET / NDMA / CAP 1.2**), pairs them with community ground reports, and provides coordinators with a one-tap broadcast mechanism to reach residents via:
- **Telegram** bot channels and groups
- **Web Push** notifications (standards-based VAPID)
- **WhatsApp Share Links** (human-in-the-loop coordinator forwarding to community groups)
- **Static CAP Feeds** for open interoperability

### What Kestrel is NOT
- **Not an official warning system:** Kestrel is an alert relay and local decision support tool. It never claims official authority.
- **Not a replacement for IMD, CWC, NDMA, or SACHET:** It consumes official feeds whenever available.
- **Not reliant on paid APIs or SMS:** Entirely built to operate within AWS free-tier and open public APIs.
- **Not a black-box forecaster:** Every alert displays clear provenance labels and data age.

---

## 2. Non-Negotiable Core Principles

1. **Provenance on Everything:** Every alert, UI card, and message carries an explicit label:
   - `MODEL_ONLY`: Model-derived guidance requiring ground confirmation.
   - `MODEL_PLUS_REPORTS`: Model guidance corroborated by local resident reports.
   - `OFFICIAL_CONFIRMED`: Corroborated or relayed from official agencies (SACHET / NDMA / IMD / CWC).
2. **Fault Isolation:** Feeds and hazards are strictly isolated. A degraded air quality or heat feed will never block or delay flood warnings.
3. **Uncertainty & Data Age:** Every view explicitly displays data age and staleness warnings. Stale data is never disguised as fresh.
4. **Free-Tier Sustainability:** 100% serverless, zero ongoing paid service dependency.
5. **Consent & Minimal Data:** Opt-in only, no phone numbers or personal names collected; one-click unsubscribe and erase compliant with the Digital Personal Data Protection (DPDP) Act 2023.
6. **Severity is Never Colour Alone:** Always represented as colour + icon + plain text.
7. **No Unverified Facts:** External APIs and thresholds are verified against official documentation prior to implementation.
8. **Measure, Don't Promise:** Backtests measure and report Probability of Detection (POD) and False Alarm Ratio (FAR).
9. **Low-Bandwidth Friendly:** Mobile-first Progressive Web App (PWA) with lightweight payloads.
10. **Source-of-Truth Ownership:** Each microservice strictly owns its DynamoDB tables. No cross-service database access.

---

## 3. Architecture Snapshot

```
External Inputs ──pull──► [flood|heat|aq|cap]-ingest (Lambda, EventBridge Scheduler)
 (Open-Meteo, GloFAS,          │
  OpenAQ, SACHET)              ├──► SQS signal queues ──► rules-engine (Lambda) ──► SQS alert queue ──► signal-fusion (Lambda) ──► alert-composer (Lambda) ──► alert-fanout (Lambda) ──► Telegram / Web Push / WhatsApp
                               │                                ▲                                                                                                    │
                        report-intake (Lambda) ─────────────────┘ (joins at signal-fusion, Phase 2)                                                                  ▼
                                                                                                                                                                    Residents

React PWA (Coordinator Dashboard + Resident page) ──HTTPS──► CloudFront ──► Lambda Function URLs ──► Per-Service Lambda
CAP Feed: Static JSON/XML served from frontend hosting (Amplify / Cloudflare Pages)
Shared Infrastructure: DynamoDB (per-service table prefixes) · SQS queues · SNS topics · Cognito User Pool · CloudWatch · EventBridge
```

---

## 4. Repository Structure

```
kestrel/
├─ master.md                  # Autonomous agent workflow driver and project specifications
├─ checklist.md               # Living task tracker and decision/verification register
├─ README.md                  # Monorepo documentation and developer quickstart
├─ docs/                      # Architectural, service, compliance, and testing specifications (01–21)
├─ backend/
│  ├─ pom.xml                 # Maven parent POM (dependency BOMs, plugin management, Java 21)
│  ├─ libs/                   # Shared libraries
│  │  ├─ common-events/       # Canonical event schemas, envelopes, enums, records
│  │  ├─ common-core/         # RFC 7807 problem details, correlation filter, JSON logging, Clock
│  │  ├─ common-outbox/       # DynamoDB transactional outbox + idempotent inbox
│  │  ├─ common-security/     # Cognito JWT validation, roles, token filters
│  │  └─ common-testing/      # LocalStack Testcontainers, WireMock fixtures, test bases
│  └─ services/               # Microservices (AWS Lambda handlers)
│     ├─ subscriber-registry/ # Locations, subscriber subscriptions, consent, coordinator auth
│     ├─ flood-ingest/        # Open-Meteo ensemble flood forecasts & exceedance calculations
│     ├─ rules-engine/        # Multi-hazard threshold state machines, persistence, cooldown
│     ├─ alert-fanout/        # Multi-channel delivery engine (Telegram, Web Push, Mock)
│     ├─ cap-ingest/          # SACHET CAP 1.2 parser and official alert ingest (Phase 2)
│     ├─ report-intake/       # Community ground reports & spam mitigation (Phase 2)
│     ├─ signal-fusion/       # Model + report + official alert fusion (Phase 2)
│     ├─ api-gateway/         # CloudFront + Lambda Function URL routing & rate limits
│     ├─ heat-ingest/         # Heat-wave forecasts & departure calculations (Phase 3)
│     ├─ aq-ingest/           # Air quality models & CPCB AQI calculations (Phase 3)
│     ├─ alert-composer/      # Multi-lingual CAP message composer (Phase 3)
│     └─ audit-evaluation/    # Historical audit logs & backtest verification (Phase 3)
├─ frontend/                  # React 18+ / TypeScript / Vite / Tailwind CSS / PWA
├─ infra/                     # Infrastructure-as-Code (AWS SAM / CDK templates)
│  ├─ dynamodb/               # DynamoDB table definitions
│  ├─ sqs-sns/                # Queue and topic definitions
│  ├─ cognito/                # User pool and app client definitions
│  ├─ cloudwatch/             # Dashboards and alarms
│  └─ scripts/                # Deployment and local bootstrapping scripts
├─ tools/                     # Development and testing utilities
│  ├─ wiremock-upstream/      # Upstream mocks for Open-Meteo, OpenAQ, SACHET
│  ├─ loadtest/               # Volume and load generation tools
│  ├─ chaos/                  # Failure injection scenarios
│  └─ new-service.sh          # Microservice scaffold generator
└─ .github/
   └─ workflows/              # GitHub Actions CI/CD pipelines
```

---

## 5. Technology Stack

| Layer | Technology |
|---|---|
| **Runtime & Language** | Java 21 (LTS), Node.js 20+, TypeScript (strict) |
| **Backend Framework** | Spring Boot 3.5.x, Spring Cloud 2025.0.x BOM |
| **Compute** | AWS Lambda (Java 21 runtime with SnapStart) |
| **Persistence** | Amazon DynamoDB (Single-table per service, TTL for caches) |
| **Messaging** | Amazon SQS (per-consumer queues) & Amazon SNS (fanout topics) |
| **Edge & Auth** | Amazon CloudFront + Lambda Function URLs, Amazon Cognito (JWT) |
| **Observability** | AWS CloudWatch (Logs, Custom Metrics via Micrometer, Alarms) |
| **Frontend** | React 18+, Redux Toolkit + RTK Query, Tailwind CSS, Leaflet, Workbox PWA |
| **Testing** | JUnit 5, AssertJ, Mockito, Testcontainers (LocalStack), WireMock, ArchUnit, Vitest, Playwright |

---

## 6. Canonical Enums

- **Hazard:** `FLOOD`, `HEAT`, `AIR_QUALITY`
- **Severity:** `NONE`, `WATCH`, `WARNING`, `SEVERE`
- **Provenance:** `MODEL_ONLY`, `MODEL_PLUS_REPORTS`, `OFFICIAL_CONFIRMED`
- **Channel:** `TELEGRAM`, `WEB_PUSH`, `EMAIL`, `MOCK`
- **Language:** `en`, `hi`, `mr`
- **CapMsgType:** `ALERT`, `UPDATE`, `CANCEL`
- **FeedState:** `HEALTHY`, `DEGRADED`, `DOWN`
- **ReportCategory:** `WATER_RISING`, `ROAD_BLOCKED`, `NEED_HELP`, `EXTREME_HEAT`, `BAD_AIR`, `OTHER`
- **ReportStatus:** `PENDING`, `VERIFIED`, `REJECTED`
- **RegionType:** `PLAINS`, `COASTAL`, `HILLY`

---

## 7. Developer Quickstart

### Prerequisites
- **Java 21** JDK
- **Apache Maven 3.9+** (or `./mvnw`)
- **Node.js 20+** & **npm 10+**
- **Docker** (required for Testcontainers & LocalStack)
- **AWS CLI** & **AWS SAM CLI** (optional for local deployment)

### Development Workflow
Development is driven systematically via `master.md` and tracked in `checklist.md`:
1. Check `checklist.md` for current task status and dependencies.
2. For backend modules:
   ```bash
   ./mvnw clean verify
   ```
3. For frontend:
   ```bash
   cd frontend
   npm install
   npm run lint && npm run typecheck && npm test && npm run build
   ```

---

## 8. Documentation Index

Detailed specifications are maintained in the `docs/` directory:
- [01 Architecture Overview](docs/01_architecture.md)
- [02 Backend Foundation](docs/02_backend_foundation.md)
- [03 Data Contracts & Events](docs/03_data_contracts.md)
- [04 Infrastructure, Observability & CI](docs/04_infrastructure.md)
- [05 Service: Subscriber Registry](docs/05_svc_subscriber-registry.md)
- [06 Service: Flood Ingest](docs/06_svc_flood-ingest.md)
- [07 Service: Rules Engine](docs/07_svc_rules-engine.md)
- [08 Service: Alert Fanout](docs/08_svc_alert-fanout.md)
- [09 Service: CAP Ingest](docs/09_svc_cap-ingest.md)
- [10 Service: Report Intake](docs/10_svc_report-intake.md)
- [11 Service: Signal Fusion](docs/11_svc_signal-fusion.md)
- [12 Service: API Gateway](docs/12_svc_api-gateway.md)
- [13 Service: Heat Ingest](docs/13_svc_heat-ingest.md)
- [14 Service: AQ Ingest](docs/14_svc_aq-ingest.md)
- [15 Service: Alert Composer](docs/15_svc_alert-composer.md)
- [16 Service: Audit & Evaluation](docs/16_svc_audit-evaluation.md)
- [17 Frontend Foundation](docs/17_frontend_foundation.md)
- [18 Frontend Coordinator Dashboard](docs/18_frontend_coordinator-dashboard.md)
- [19 Frontend Resident PWA](docs/19_frontend_resident-pwa.md)
- [20 Testing & Acceptance](docs/20_testing_and_acceptance.md)
- [21 Ethics, Compliance & Attribution](docs/21_ethics_compliance_and_attribution.md)

# CHECKLIST.md — Kestrel Microservices Prototype Progress Tracker

> **Living file.** The agent updates it at the end of EVERY task (see `prototype/master.md` §0 and §9).
> Tick format: `- [x] PT1-02 — … · ✔ 2026-10-22 · note/evidence`. Never tick without passing build and tests.

---

## ▶ CURRENT STATUS (update every task)

| Field | Value |
|---|---|
| Current phase | **Phase 0 — Monorepo & Shared Foundations** |
| Last completed task | — |
| In progress | — |
| Next task | **PT0-01** |
| Blockers / questions | none |
| Last updated | 2026-10-22 |
| Build status (backend / frontend) | not started / not started |

Progress: Phase 0 `0/5` · Phase 1 `0/5` · Phase 2 `0/5` · Phase 3 `0/6` · Phase 4 `0/4` (Total: `0/25`)

---

## 📌 DECISION LOG (Microservices Prototype Specific)

| ID | Decision | Rationale | Status |
|---|---|---|---|
| PT-D-01 | **Java 21 + Maven Multi-Module Monorepo** | Parent POM managing shared BOMs, plugins, and dependencies across all modules | locked |
| PT-D-02 | **Independent Microservices per Bounded Context** | Distinct Spring Boot services (`api-gateway`, `subscriber-registry`, `flood-ingest`, `rules-engine`, `alert-fanout`) | locked |
| PT-D-03 | **Spring Cloud Gateway as Edge (Port 8080)** | Central entry point routing `/api/v1/**` to downstream services; handles CORS | locked |
| PT-D-04 | **Per-Service Database Isolation (H2)** | Each microservice owns its own isolated H2 database; zero cross-service database access | locked |
| PT-D-05 | **Shared Libraries (`common-events`, `common-core`)** | Canonical event records, enums, RFC 7807 problem details, and shared utilities | locked |
| PT-D-06 | **Event-Driven Communication** | Microservices communicate via asynchronous event contracts (`LocationUpserted`, `FloodSignal`, `AlertEvent`) | locked |
| PT-D-07 | **React 18 + TypeScript (strict) + Vite** | High-performance, modern reactive web application | locked |
| PT-D-08 | **Redux Toolkit (RTK) + RTK Query** | Central Redux store, domain slices for client state, `baseApi` with `injectEndpoints` for server cache | locked |
| PT-D-09 | **Live Open-Meteo + 5-Mode Simulation Injector** | Real-time river discharge forecasts plus instant demo trigger (`CALM`, `WATCH`, `WARNING`, `SEVERE`) | locked |
| PT-D-10 | **Dual Delivery Channel: Telegram Bot + Mock Queue** | Real Telegram messaging when configured; observable in-memory feed for UI verification | locked |
| PT-D-11 | **Client-side WhatsApp Share Link (`wa.me/?text=...`)** | One-tap WhatsApp forwarding for coordinators with pre-formatted bilingual alert text | locked |
| PT-D-12 | **4 Seeded Pilot Locations** | Mumbai Kurla (Mithi), Patna Ward 12 (Ganges), Guwahati (Brahmaputra), Cuttack (Mahanadi) | locked |
| PT-D-13 | **Triple-Language Support (`en`, `hi`, `mr`)** | Real localization across all alert templates, emergency actions, and UI components | locked |
| PT-D-14 | **Leaflet + OpenStreetMap for Hazard Maps** | Interactive map with animated severity-pulsing markers connected to Redux `map` slice | locked |
| PT-D-15 | **Community Ground Reports Upgrade Provenance** | Resident reports immediately upgrade active alert provenance to `MODEL_PLUS_REPORTS` | locked |
| PT-D-16 | **Port Allocation Matrix** | Gateway `8080`, Registry `8081`, Flood Ingest `8082`, Rules Engine `8083`, Fanout `8084`, Frontend `5173` | locked |

---

## 🔎 VERIFICATION REGISTER

| ID | What to verify | Needed before | Status |
|---|---|---|---|
| PT-VERIFY-01 | Spring Boot 3.4.x ↔ Spring Cloud 2024.0.x compatibility | PT0-01, PT0-04 | DONE (compatible with Spring Cloud 2024.0.0 / 2024.0.1) |
| PT-VERIFY-02 | Open-Meteo Flood API ensemble query syntax and dynamic member parsing | PT1-01 | DONE (verified `daily=river_discharge&ensemble=true&forecast_days=10`) |
| PT-VERIFY-03 | Redux Toolkit `createApi` + `injectEndpoints` with Vite proxy | PT0-05, PT3-02 | DONE (standard RTK Query pattern with single base query) |
| PT-VERIFY-04 | WhatsApp share URL encoding across mobile and web (`https://wa.me/?text=...`) | PT2-04 | DONE (standard URL encoding works seamlessly) |
| PT-VERIFY-05 | Accurate Hindi and Marathi emergency flood warning terminology | PT2-03 | DONE (reviewed NDMA/SDMA disaster management terms) |

---

## 🚧 BLOCKERS / QUESTIONS FOR THE USER
*(none yet — agent appends here with date and task ID)*

---

## PHASE 0 — Monorepo & Shared Foundations `docs: 01, 04`

- [ ] **PT0-01** — Create Maven Parent POM (`prototype/backend/pom.xml`) with Java 21, Spring Boot 3.4.x BOM, Spring Cloud BOM, submodules declaration (`libs/*`, `services/*`), plugin management, and Maven wrapper. Verify `./mvnw clean compile` passes. · doc 01 §2 · deps: —
- [ ] **PT0-02** — Implement `libs/common-events`: canonical Java records for `FloodSignal`, `AlertEvent`, `LocationUpserted`, `SubscriptionChanged`, Enums (`Hazard`, `Severity`, `Provenance`, `Channel`, `Language`), Jackson ISO-8601 UTC config, and round-trip serialization tests. · doc 01 §3 · deps: PT0-01
- [ ] **PT0-03** — Implement `libs/common-core`: RFC 7807 `ProblemDetail` `@RestControllerAdvice`, `CorrelationIdFilter`, `Clock` / `TimeProvider` bean, and `GeoUtils` (haversine, bounding box). · doc 01 §4 · deps: PT0-01
- [ ] **PT0-04** — Implement `services/api-gateway`: Spring Cloud Gateway application on port 8080 with route mappings to downstream microservices (`/api/v1/registry/**` $\to$ 8081, `/api/v1/flood/**` $\to$ 8082, `/api/v1/rules/**` $\to$ 8083, `/api/v1/fanout/**` $\to$ 8084) and CORS filter for `http://localhost:5173`. · doc 01 §2 · deps: PT0-01, PT0-03
- [ ] **PT0-05** — Initialize `prototype/frontend`: Vite + React 18 + TypeScript (strict) + Redux Toolkit + RTK Query + Tailwind CSS + Lucide React + Leaflet scaffold. Configure Redux store skeleton and verify `npm run build` succeeds. · doc 04 §1, §2 · deps: —

---

## PHASE 1 — Ingest & Rules Microservices `docs: 01, 02`

- [ ] **PT1-01** — Implement `services/flood-ingest` (Port 8082): standalone Spring Boot app, isolated H2 database, Open-Meteo `OpenMeteoClient` (using Spring `RestClient`) fetching 10-day ensemble forecasts, dynamic member parsing (50 vs 51 members), and Gumbel/GloFAS return period calculation ($T_2, T_5, T_{20}$). · doc 02 §1 · deps: PT0-02, PT0-03
- [ ] **PT1-02** — Implement `services/flood-ingest` scenario simulator: built-in `ScenarioSimulator` supporting 5 modes (`LIVE_OPEN_METEO`, `CALM`, `WATCH_2YR`, `WARNING_5YR`, `SEVERE_20YR`) and publish `FloodSignal` events to the inter-service event bus. · doc 02 §2 · deps: PT1-01
- [ ] **PT1-03** — Implement `services/rules-engine` (Port 8083): standalone Spring Boot app, isolated H2 database, pure domain `ExceedanceCalculator` and `SeverityStateMachine` with persistence (`WATCH: 1`, `WARNING: 2`, `SEVERE: 2`), hysteresis de-escalation, 12h cooldown, and stale data guard (>36h). Unit test with table-driven tests. · doc 02 §3, §4 · deps: PT0-02, PT0-03
- [ ] **PT1-04** — Wire `services/rules-engine` event subscriber: consume `FloodSignal` $\to$ run state machine $\to$ persist state transition $\to$ emit `AlertEvent(stage=DECISION, provenance=MODEL_ONLY)`. · doc 02 §4 · deps: PT1-02, PT1-03
- [ ] **PT1-05** — Implement Rules Engine REST APIs (`GET /api/v1/rules/status`, `GET /api/v1/rules/forecast/{locationId}`) and simulation trigger (`POST /api/v1/flood/simulate`). Verify requests route cleanly through `api-gateway` (Port 8080). · doc 01 §5, doc 02 §5 · deps: PT0-04, PT1-04

---

## PHASE 2 — Registry & Fanout Microservices `docs: 01, 03`

- [ ] **PT2-01** — Implement `services/subscriber-registry` (Port 8081): standalone Spring Boot app, isolated H2 database, auto-seed 4 Indian pilot locations (Mumbai, Patna, Guwahati, Cuttack), subscription CRUD with UUID tokens, and publish `LocationUpserted` on bootstrap. · doc 03 §1, §2 · deps: PT0-02, PT0-03
- [ ] **PT2-02** — Implement event-carried location replication in `services/flood-ingest` and `services/alert-fanout`: consume `LocationUpserted` to maintain local replica tables without cross-service database access. · doc 01 §2, doc 03 §1 · deps: PT1-01, PT2-01
- [ ] **PT2-03** — Implement `services/alert-fanout` (Port 8084): standalone Spring Boot app, isolated H2 database, consume `AlertEvent(stage=DECISION)`, and implement `AlertTemplateComposer` rendering localized alert texts in English, Hindi, and Marathi with mandatory provenance labels. · doc 03 §3 · deps: PT0-02, PT1-04
- [ ] **PT2-04** — Implement `services/alert-fanout` delivery channels: WhatsApp share link generator (`https://wa.me/?text=...`), Telegram Bot API client (dispatches if token configured), in-memory observable `MockDispatchQueue`, and REST endpoints (`GET /api/v1/fanout/recent`, `POST /api/v1/fanout/broadcast`). · doc 03 §4, §5 · deps: PT2-03
- [ ] **PT2-05** — Implement Community Ground Report intake in `services/alert-fanout` (`POST /api/v1/fanout/reports`): accept resident reports, link to location, and immediately upgrade active alert provenance from `MODEL_ONLY` to `MODEL_PLUS_REPORTS`. · doc 03 §6 · deps: PT2-04

---

## PHASE 3 — Redux Store & Frontend UI `docs: 01, 04`

- [ ] **PT3-01** — Configure Redux Toolkit architecture: `src/app/store.ts`, typed hooks `useAppDispatch` / `useAppSelector`, and client slices (`authSlice`, `uiSlice`, `mapSlice`, `broadcastDraftSlice`, `residentSlice`). · doc 04 §2 · deps: PT0-05
- [ ] **PT3-02** — Implement RTK Query `baseApi` with injected endpoints per microservice domain: `registryApi.ts` (locations, subscriptions), `floodApi.ts` (simulation triggers), `rulesApi.ts` (alert status with 5s polling, forecast data), and `fanoutApi.ts` (recent dispatches, broadcasts, reports). · doc 04 §3 · deps: PT3-01, PT1-05, PT2-04
- [ ] **PT3-03** — Implement design primitives: `SeverityBadge` (Color + Icon + Text), `ProvenanceBadge` (`MODEL_ONLY` / `MODEL_PLUS_REPORTS`), `DataAge` freshness indicator, `StaleBanner`, and lightweight hand-rolled SVG `ForecastBandChart` displaying 10-day spread vs return levels. · doc 04 §4 · deps: PT3-01
- [ ] **PT3-04** — Implement interactive Leaflet hazard map (`MapView.tsx`): center on India, display 4 pilot locations with animated severity-colored pulsing markers, and bind selection to Redux `mapSlice`. · doc 04 §5 · deps: PT3-02, PT3-03
- [ ] **PT3-05** — Build Coordinator Dashboard view: location detail cards, sticky simulation control bar (Calm, Watch, Warning, Severe, Live), One-Tap Broadcast Modal, and one-click "Share to WhatsApp" trigger. · doc 04 §6 · deps: PT3-04
- [ ] **PT3-06** — Build Resident Public View: mobile-friendly ward status in selected language (`en`, `hi`, `mr`), "Subscribe for Alerts" modal, 2-click Ground Report modal, and collapsible Live Dispatch Feed drawer. · doc 04 §7 · deps: PT3-02, PT3-05

---

## PHASE 4 — End-to-End Orchestration & Polish `docs: 05`

- [ ] **PT4-01** — Implement multi-service orchestration: create single-command startup script (`start-prototype-microservices.bat` / `.sh`) that launches all 5 backend microservices and the Vite frontend. · doc 05 §3 · deps: PT1-05, PT2-04, PT3-06
- [ ] **PT4-02** — Implement automated E2E integration test: simulate 5-Year Flood on Kurla Ward $\to$ verify Rules Engine transitions to `WARNING` $\to$ submit ground report $\to$ verify provenance upgrades to `MODEL_PLUS_REPORTS` $\to$ trigger broadcast $\to$ verify dispatch logged in fanout queue. · doc 05 §2 · deps: PT4-01
- [ ] **PT4-03** — Write comprehensive developer documentation in `prototype/README.md` covering architecture, port matrix, API endpoints, Redux state flow, and local run instructions. · doc 05 §1 · deps: PT4-02
- [ ] **PT4-04 — GATE PT-GATE:** Execute complete verification walk-through of the 5 Golden Demo Scenarios. Verify all microservice builds pass (`./mvnw clean verify`), frontend build passes (`npm run build`), zero console errors, and live WhatsApp link opens correctly. · doc 05 §1 · deps: all PT tasks

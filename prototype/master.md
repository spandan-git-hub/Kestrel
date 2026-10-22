# MASTER.md — Kestrel Prototype (Java Spring Boot Microservices + React Redux)

> **Paste ONLY this file as the prompt — it tells the agent what to do next** (§0). Optional one-line overrides are listed in §0.2.
> Task-level detail lives in `prototype/*.md`. Progress lives in `prototype/checklist.md`.
> **Scope:** A fully functional, true **Java Spring Boot microservices** project with an **API Gateway**, independent bounded contexts, event-driven contracts, and a **React 18 + TypeScript + Redux Toolkit (RTK Query)** frontend.

---

## 0. How this file drives the work (read first, every time)

**You (the agent) are self-directing. The user normally pastes ONLY this file.** Anything the user adds after it is an optional override (see §0.2). Never ask the user "what should I do?" when the checklist already specifies the next task.

### 0.1 Default behaviour (nothing added after this file)
1. Open [`prototype/checklist.md`](file:///d:/Kestrel/prototype/checklist.md). Read **Current Status**, **Blockers**, **Decision Log**, **Verification Register**.
2. If a Blocker is open, resolve it with the user first (ask one concise question, then stop).
3. Select the **first unchecked task whose dependencies are all ticked** (or the task named in "Next task"). If it is a **GATE** task, run the gate procedure (§0.3).
4. Open **ONLY the doc(s)** the task references in `prototype/` (see §10). Do not load unrelated documents.
5. Implement exactly the task scope. Maintain **strict microservices separation** (independent Spring Boot applications, independent databases, no cross-service database access, communication via shared event contracts or Gateway REST).
6. In the frontend, follow the **Redux Toolkit + RTK Query** architecture (slices for client state, `baseApi` with `injectEndpoints` for server cache).
7. Run build + tests (§9 Definition of Done). Fix until green.
8. Update [`prototype/checklist.md`](file:///d:/Kestrel/prototype/checklist.md): tick the box with date + one-line evidence, update **Current Status** and progress counters, add any new decisions / blockers.
9. **Continue to the next task** in the same service if context budget allows (max 2–3 tasks per turn, run tests after each). Stop at any gate, failing test you cannot fix, or a service boundary.
10. End every reply with exactly this block:
```
DONE: <task IDs ticked>
FILES: <changed files>
COMMANDS: <build/test/dev commands + pass/fail>
CHECKLIST: <what changed>
NEXT: <next task ID> — paste prototype/master.md again to proceed
```

### 0.2 Optional overrides the user may append (after this file)
| User adds | You do |
|---|---|
| `Task: PT1-03` (or range `PT1-01..PT1-03`) | Do those specific tasks (steps 4–10) |
| `Status` | Read checklist; report phase, progress counts, blockers, next 3 tasks. Change nothing |
| `Fix: <pasted error/test output>` | Root cause in ≤3 lines, minimal fix, re-run tests, note in checklist; no unrelated edits |
| `Review: Phase 1` | Run gate procedure (§0.3) |
| `Decide: PT-D-xx = <choice>` | Record in Decision Log, then continue |

### 0.3 Gate procedure (tasks named GATE, or `Review:`)
Check every task in the phase is ticked with evidence; run full Maven multi-module verify and frontend test suite; verify with manual/automated scenario checks against [`prototype/05_demo_scenarios_and_testing.md`](file:///d:/Kestrel/prototype/05_demo_scenarios_and_testing.md); tick the gate only if all criteria pass.

### 0.4 Rules of engagement
- **Microservices Discipline**: Each backend microservice is a standalone Spring Boot application with its own `pom.xml`, its own port, its own database schema/file, and its own package namespace.
- **No Cross-Service Database Access**: Microservices communicate strictly via event contracts (`common-events`) or via REST through the API Gateway.
- **Frontend Architecture**: React 18+ / TypeScript (strict) / Vite, state managed via **Redux Toolkit (RTK Query)** with slices and injected endpoints.
- **Interactive Simulation**: Built-in scenario simulation switcher (`CALM`, `WATCH`, `WARNING`, `SEVERE`, `LIVE`) so any evaluator can demonstrate live hazard alerting in seconds.
- **Multilingual & Multichannel**: Full support for English, Hindi, and Marathi; dispatch to Telegram Bot API, Mock queue, and one-tap **WhatsApp share link** (`wa.me/?text=...`).

---

## 1. Product in one paragraph (Microservices Prototype)

**Kestrel Prototype** is a complete, functioning microservices implementation of the Kestrel multi-hazard alert relay for India. Built on **Java 21, Spring Boot 3.4+, Spring Cloud Gateway**, and **React 18 + Redux Toolkit**, it partitions responsibilities across 5 dedicated services: an **API Gateway** (single public entry), **subscriber-registry** (pilot locations and resident subscriptions), **flood-ingest** (Open-Meteo ensemble river discharge ingestion and scenario simulation), **rules-engine** (exceedance detection, hysteresis and severity state machine), and **alert-fanout** (multilingual template generation, Telegram Bot dispatch, in-memory live feed, and WhatsApp share link builder). Residents and coordinators interact with a high-performance Redux-powered PWA featuring an interactive Leaflet hazard map, SVG ensemble band charts, and 2-click ground reports that dynamically upgrade alert provenance.

---

## 2. Non-Negotiable Principles

1. **Microservices Architecture**:
   - Independent Spring Boot microservices with distinct runtime ports (`8080` Gateway, `8081` Registry, `8082` Flood Ingest, `8083` Rules Engine, `8084` Fanout).
   - Database per service (isolated H2 file/in-memory instances per microservice; zero shared tables).
   - Decoupled asynchronous event contracts for inter-service workflows (`LocationUpserted`, `FloodSignal`, `AlertEvent`, `ReportAccepted`).
2. **Redux Toolkit + RTK Query**:
   - Single central Redux store.
   - Client state in domain slices (`auth`, `ui`, `map`, `broadcastDraft`, `resident`).
   - Server state strictly managed via RTK Query `baseApi` with `injectEndpoints` per microservice domain.
   - Zero duplication of server state in client slices; memoized selectors (`createSelector`).
3. **Provenance on Everything**:
   - Every alert, card, and message carries an explicit label: `MODEL_ONLY`, `MODEL_PLUS_REPORTS`, or `OFFICIAL_CONFIRMED`.
4. **Local & Cloud-Ready**:
   - Runs 100% locally with a single startup script without requiring AWS accounts, while maintaining strict architectural parity with the production microservices blueprint.
5. **WhatsApp & Multichannel Delivery**:
   - Coordinators can click one button to open WhatsApp (`wa.me/?text=...`) with pre-composed, bilingual alert text formatted for community chat groups.
6. **Triple-Language Support**:
   - Full English (`en`), Hindi (`hi`), and Marathi (`mr`) localization across alert messages, UI labels, and emergency advice.

---

## 3. Technology Stack

| Layer | Choice | Rationale |
|---|---|---|
| Runtime / Build | **Java 21 (LTS)**, Maven 3.9 (wrapper) | Monorepo multi-module; strict record types and modern Java features |
| Framework | **Spring Boot 3.4.x** + **Spring Cloud 2024.0.x** | Enterprise-grade microservices foundation |
| API Gateway | **Spring Cloud Gateway** (Port 8080) | Unified API entry point, route forwarding, CORS, header enrichment |
| Microservices | 4 Independent Spring Boot Apps (Ports 8081–8084) | True bounded contexts, independent deployment, resilience |
| Persistence | **Per-Service Embedded H2 Database** (JPA / Hibernate) | Isolated database per service; auto-DDL; zero external DB installation |
| Messaging / Events | **Event Contracts (`common-events`)** via Event Bus | Decoupled publish-subscribe; contract stability |
| Frontend | **React 18+ / TypeScript (strict) / Vite** | Fast build, modern reactive rendering, strict type safety |
| State Management | **Redux Toolkit (RTK) + RTK Query** | Centralized client state, automatic caching, deduplication, polling |
| Map Layer | **Leaflet + react-leaflet + OpenStreetMap** | Interactive hazard map with animated risk markers |
| Styling & UI | **Tailwind CSS + Lucide React** | Sleek dark theme (`#0b0f19`), glassmorphism, responsive cards |
| Delivery | **Telegram Bot API**, **WhatsApp Link**, **Mock Queue** | Real messaging when token provided; 100% demoable offline |

---

## 4. Microservices Topology Snapshot

```
                       REACT 18 + TYPESCRIPT + REDUX TOOLKIT (Port 5173)
                         [Coordinator Dashboard | Resident PWA | Sim Bar]
                                              │ HTTP / REST
                                              ▼
                        SPRING CLOUD API GATEWAY (Port 8080)
                         routes: /api/v1/{registry, flood, rules, fanout}
                                              │
         ┌──────────────────┬─────────────────┼──────────────────┬─────────────────┐
         │                  │                 │                  │                 │
         ▼                  ▼                 ▼                  ▼                 ▼
 ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
 │ subscriber-   │  │ flood-ingest  │  │ rules-engine  │  │ alert-fanout  │  │ report-intake │
 │ registry      │  │ (Port 8082)   │  │ (Port 8083)   │  │ (Port 8084)   │  │ (Port 8085 /  │
 │ (Port 8081)   │  │               │  │               │  │               │  │  in Fanout)   │
 └───────┬───────┘  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘
         │ DB: registry     │ DB: flood        │ DB: rules        │ DB: fanout       │ DB: reports
         │                  │                  │                  │                  │
         │ LocationUpserted │ FloodSignal      │ AlertEvent       │ DeliveryReceipt  │ ReportAccepted
         └──────────────────┴─────────▲────────┴─────────▲────────┴──────────────────┴───────┘
                                      │ Event Bus        │
                                      └──────────────────┘
```

### Microservices Directory & Responsibility Matrix

| # | Module Name | Port | Database | Primary Responsibility |
|---|---|---|---|---|
| 1 | `api-gateway` | `8080` | None | Reverse proxy, routes `/api/v1/**` to downstream services, CORS |
| 2 | `subscriber-registry`| `8081` | `kestrel_registry` | Locations catalog, subscriber registrations, consent, token auth |
| 3 | `flood-ingest` | `8082` | `kestrel_flood` | Open-Meteo pull, scenario simulation, exceedance shares ($T_2, T_5, T_{20}$) |
| 4 | `rules-engine` | `8083` | `kestrel_rules` | Severity state machine, persistence, hysteresis, cooldown, stale guard |
| 5 | `alert-fanout` | `8084` | `kestrel_fanout` | Multilingual templates, Telegram Bot, Mock feed, WhatsApp share link |

---

## 5. Monorepo Layout (`prototype/`)

```
prototype/
├─ master.md                           # This driver prompt file
├─ checklist.md                        # Living progress tracker
├─ 01_architecture_and_contracts.md    # Multi-service topology, routing, schemas, DTOs
├─ 02_ingest_and_rules.md              # flood-ingest and rules-engine microservices specs
├─ 03_registry_and_dispatch.md         # subscriber-registry and alert-fanout microservices specs
├─ 04_frontend_ui.md                   # React + TypeScript + Redux Toolkit (RTK Query) specs
├─ 05_demo_scenarios_and_testing.md    # Multi-service orchestration, test suite, demo script
├─ backend/
│  ├─ pom.xml                          # Maven Parent POM (BOMs, modules, plugins)
│  ├─ libs/
│  │  ├─ common-events/                # Canonical records (FloodSignal, AlertEvent, etc.)
│  │  └─ common-core/                  # ProblemDetail advice, correlation ID, clock, GeoUtils
│  └─ services/
│     ├─ api-gateway/                  # Spring Cloud Gateway (Port 8080)
│     ├─ subscriber-registry/          # Port 8081
│     ├─ flood-ingest/                 # Port 8082
│     ├─ rules-engine/                 # Port 8083
│     └─ alert-fanout/                 # Port 8084
└─ frontend/                           # React 18 + TypeScript + Vite + Redux Toolkit
   ├─ package.json
   ├─ src/
   │  ├─ app/                          # store.ts, hooks.ts (useAppDispatch, useAppSelector)
   │  ├─ api/                          # baseApi.ts, registryApi.ts, floodApi.ts, rulesApi.ts, fanoutApi.ts
   │  ├─ features/                     # auth/, ui/, map/, broadcast/, resident/ slices
   │  ├─ components/                   # SeverityBadge, ProvenanceBadge, DataAge, ForecastBandChart, MapView
   │  ├─ views/                        # CoordinatorDashboard, ResidentView, SimulationBar
   │  └─ i18n/                         # en, hi, mr locales
```

---

## 6. Backend Standards (All Microservices)

1. **Hexagonal-Lite Structure**:
   `api` (REST controllers, request/response DTOs) $\to$ `app` (application services, use cases) $\to$ `domain` (pure domain models, calculations, zero framework dependencies) $\to$ `infra` (JPA repositories, HTTP clients, event publishers) $\to$ `config`.
2. **Database Isolation**:
   Each service configures its own H2 database URL (e.g. `jdbc:h2:mem:kestrel_flood_db` or file-based). Cross-service joins or database sharing are strictly forbidden.
3. **API Standards**:
   - Base paths: `/api/v1/<service-name>/**` routed through Gateway port 8080.
   - RFC 7807 `ProblemDetail` for all error responses (`application/problem+json`).
   - Timestamps: ISO-8601 UTC (`Instant`).
4. **Resilience & Fallback**:
   Ingest services cache last-good forecasts. If Open-Meteo is unreachable, `dataAgeSeconds` increases while serving cached or simulated data without failing the service.

---

## 7. Frontend Standards (React + TypeScript + Redux Toolkit)

1. **Redux Architecture**:
   - `store.ts`: Configures Redux store combining `baseApi.reducer` and domain slices.
   - Typed hooks: `useAppDispatch` and `useAppSelector` exported from `src/app/hooks.ts`.
   - **Client Slices**:
     - `authSlice`: coordinator mode / resident role.
     - `uiSlice`: language (`en`, `hi`, `mr`), theme, active toast messages, simulation mode.
     - `mapSlice`: selectedLocationId, active hazard filter, viewport.
     - `broadcastDraftSlice`: coordinator draft alert, preview data, custom notes.
     - `residentSlice`: resident's selected ward, subscription state.
   - **RTK Query**:
     - `baseApi`: Configured with base query pointing to `http://localhost:8080/api/v1`.
     - Injected endpoints per domain (`registryApi`, `floodApi`, `rulesApi`, `fanoutApi`).
     - Automated polling for active alerts (`pollingInterval: 5000` ms).
2. **Design Primitives**:
   - `SeverityBadge`: Color + Icon + Text (`NONE`, `WATCH`, `WARNING`, `SEVERE`).
   - `ProvenanceBadge`: `MODEL_ONLY`, `MODEL_PLUS_REPORTS`, `OFFICIAL_CONFIRMED`.
   - `ForecastBandChart`: Lightweight responsive SVG showing 10-day ensemble spread vs $T_2, T_5, T_{20}$ thresholds.
   - `MapView`: Leaflet map with pulsing severity markers.

---

## 8. Canonical Enums

```java
public enum Hazard { FLOOD, HEAT, AIR_QUALITY }
public enum Severity { NONE, WATCH, WARNING, SEVERE }
public enum Provenance { MODEL_ONLY, MODEL_PLUS_REPORTS, OFFICIAL_CONFIRMED }
public enum Channel { TELEGRAM, MOCK, WEB_PUSH }
public enum Language { en, hi, mr }
public enum ReportCategory { WATER_RISING, ROAD_BLOCKED, NEED_HELP, OTHER }
public enum SimulationScenario { LIVE_OPEN_METEO, CALM, WATCH_2YR, WARNING_5YR, SEVERE_20YR }
```

---

## 9. Definition of Done (Every Task)

- [ ] Code compiles without warnings (`./mvnw clean compile` across all modules / `npm run build`).
- [ ] Required unit and integration tests exist and pass.
- [ ] Microservice starts cleanly on its designated port and responds via Gateway.
- [ ] [`prototype/checklist.md`](file:///d:/Kestrel/prototype/checklist.md) updated with task ticked, date, and verification notes.
- [ ] Standard completion block outputted at the end of the response.

---

## 10. Doc Map

| If your task is about | Read this document |
|---|---|
| Topology, Gateway routing, Event contracts, Schemas, Ports | [`prototype/01_architecture_and_contracts.md`](file:///d:/Kestrel/prototype/01_architecture_and_contracts.md) |
| `flood-ingest` & `rules-engine` microservices, Open-Meteo, Exceedance, State Machine | [`prototype/02_ingest_and_rules.md`](file:///d:/Kestrel/prototype/02_ingest_and_rules.md) |
| `subscriber-registry` & `alert-fanout`, Multilingual templates, Telegram, WhatsApp | [`prototype/03_registry_and_dispatch.md`](file:///d:/Kestrel/prototype/03_registry_and_dispatch.md) |
| React 18, TypeScript, Redux Toolkit, RTK Query, Leaflet, UI Design | [`prototype/04_frontend_ui.md`](file:///d:/Kestrel/prototype/04_frontend_ui.md) |
| Multi-service launch script, automated tests, 5-scenario demo script | [`prototype/05_demo_scenarios_and_testing.md`](file:///d:/Kestrel/prototype/05_demo_scenarios_and_testing.md) |

---

## 11. Phases

- **Phase 0 — Monorepo & Shared Foundations**: Maven Parent POM, `common-events`, `common-core`, `api-gateway`, and React + Redux frontend scaffold.
- **Phase 1 — Ingest & Rules Microservices**: `flood-ingest` (Open-Meteo + simulation), `rules-engine` (state machine), and inter-service signal flow.
- **Phase 2 — Registry & Fanout Microservices**: `subscriber-registry`, `alert-fanout` (multilingual templates, WhatsApp, Telegram/Mock), and community reports.
- **Phase 3 — Redux Store & Frontend UI**: Redux slices, RTK Query endpoints, Leaflet map, Coordinator Dashboard, and Resident View.
- **Phase 4 — End-to-End Orchestration & Polish**: Multi-service launch scripts, integration test suite, and 5-scenario live demo verification.

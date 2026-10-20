# MASTER.md — Kestrel Prototype (Quick, Functional Multi-Hazard Alert Relay)

> **Paste ONLY this file as the prompt — it tells the agent what to do next** (§0). Optional one-line overrides are listed in §0.2.
> Task-level detail lives in `prototype/*.md`. Progress lives in `prototype/checklist.md`.
> **Scope:** A fast, runnable, end-to-end prototype proving Kestrel's core value: Live/Simulated Hazard Ingest → Automated Rule Detection → Coordinator 1-Tap Broadcast → Multilingual Alert Relay (Telegram & WhatsApp).

---

## 0. How this file drives the work (read first, every time)

**You (the agent) are self-directing. The user normally pastes ONLY this file.** Anything the user adds after it is an optional override (see §0.2). Never ask the user "what should I do?" when the checklist already specifies the next task.

### 0.1 Default behaviour (nothing added after this file)
1. Open [`prototype/checklist.md`](file:///d:/Kestrel/prototype/checklist.md). Read **Current Status**, **Blockers**, **Decision Log**, **Verification Register**.
2. If a Blocker is open, resolve it with the user first (ask one concise question, then stop).
3. Select the **first unchecked task whose dependencies are all ticked** (or the task named in "Next task"). If it is a **GATE** task, run the gate procedure (§0.3).
4. Open **ONLY the doc(s)** the task references in `prototype/` (see §10). Do not load unrelated documents.
5. Implement exactly the task scope. Keep code lean, modular, and functional. No unnecessary enterprise ceremony (no distributed AWS Lambdas, no DynamoDB outbox relays, no multi-queue SQS/SNS).
6. Run build + tests (§9 Definition of Done). Fix until green.
7. Update [`prototype/checklist.md`](file:///d:/Kestrel/prototype/checklist.md): tick the box with date + one-line evidence, update **Current Status** and progress counters, add any new decisions / blockers.
8. **Continue to the next task** if context budget allows (max 3 tasks per turn, run tests after each). Stop at any gate, failing test you cannot fix, or an open question.
9. End every reply with exactly this block:
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
| `Task: PT1-03` (or range `PT1-01..PT1-03`) | Do those specific tasks (steps 4–9) |
| `Status` | Read checklist; report phase, progress counts, blockers, next 3 tasks. Change nothing |
| `Fix: <pasted error/test output>` | Root cause in ≤3 lines, minimal fix, re-run tests, note in checklist; no unrelated edits |
| `Review: Phase 1` | Run gate procedure (§0.3) |
| `Decide: PT-D-xx = <choice>` | Record in Decision Log, then continue |

### 0.3 Gate procedure (tasks named GATE, or `Review:`)
Check every task in the phase is ticked with evidence; run full build and test suite; verify with manual/automated scenario checks against [`prototype/05_demo_scenarios_and_testing.md`](file:///d:/Kestrel/prototype/05_demo_scenarios_and_testing.md); tick the gate only if all criteria pass.

### 0.4 Rules of engagement
- Keep it runnable locally with zero cloud or paid account dependencies.
- Use live Open-Meteo flood/weather API for real Indian coordinates, with an in-memory scenario injector for deterministic demo testing.
- Base code directory: `prototype/backend` (Spring Boot 3.4+ / Java 21) and `prototype/frontend` (React 18+ / Vite / Tailwind / Leaflet).
- Preserve domain fidelity: 3-level provenance (`MODEL_ONLY`, `MODEL_PLUS_REPORTS`, `OFFICIAL_CONFIRMED`), canonical severities (`NONE`, `WATCH`, `WARNING`, `SEVERE`), and multilingual templates (`en`, `hi`, `mr`).

---

## 1. Product in one paragraph (Prototype Edition)

**Kestrel Prototype** is a lean, self-contained, working demonstration of the Kestrel multi-hazard alert relay for India. It ingests live river-discharge and weather forecasts from Open-Meteo for 4 pilot Indian wards (Mumbai Kurla, Patna Ward 12, Guwahati Brahmaputra, Cuttack Mahanadi), computes flood exceedance probabilities against return periods (2-yr, 5-yr, 20-yr), applies a hysteresis-backed severity state machine, and displays real-time hazard status on an interactive Leaflet map. Coordinators can trigger an immediate one-tap broadcast, forward pre-composed regional-language alerts directly to **WhatsApp** (`wa.me` share link), and dispatch alerts to **Telegram** or a live mock feed. Residents can check their ward's alert status in English, Hindi, or Marathi, subscribe for alerts, and submit 2-click ground reports that instantly upgrade alert provenance.

---

## 2. Non-negotiable principles for the Prototype

1. **Zero Cloud Friction**: Runs 100% on a local laptop using standard Java 21 and Node.js. No AWS account, Docker containers, LocalStack, or paid SaaS required.
2. **Provenance on Everything**: Every alert, card, and message carries an explicit label: `MODEL_ONLY`, `MODEL_PLUS_REPORTS`, or `OFFICIAL_CONFIRMED`.
3. **Interactive Simulation Bar**: Must include a one-click simulation switcher (`Calm`, `Simulate 2-Yr Watch`, `Simulate 5-Yr Warning`, `Simulate 20-Yr Severe`, `Live Open-Meteo`) so any viewer can see alerts trigger dynamically without waiting for real rainfall.
4. **WhatsApp One-Tap Forwarding**: Coordinators must be able to click a button that opens WhatsApp (`wa.me/?text=...`) with pre-composed, regional-language alert text formatted for community chat groups.
5. **Severity is Never Colour Alone**: Always colour + icon + plain-language text.
6. **Triple-Language Support**: All alerts and core resident text must support English (`en`), Hindi (`hi`), and Marathi (`mr`).
7. **Fast Feedback Loop**: Single backend command, single frontend command, fast unit tests (< 5 seconds).

---

## 3. Prototype Technology Stack

| Layer | Choice | Rationale for Prototype |
|---|---|---|
| Backend runtime | **Java 21**, Maven wrapper | Matches root project language; rock-solid domain modeling |
| Backend framework | **Spring Boot 3.4.x** (Web, Validation, H2, Spring Data JPA) | Single monolithic service; fast startup; zero cloud setup |
| Ingestion | **Spring `RestClient` / `WebClient`** → Open-Meteo API | Free, keyless, real-time river-discharge and weather data |
| Database | **Embedded H2** (in-memory or file-backed) | Instant startup, auto-DDL, pre-seeded pilot locations |
| Eventing | **Spring `ApplicationEventPublisher`** | In-process decoupled events; mirrors future SQS/SNS without infra |
| Scheduling | **Spring `@Scheduled`** + Manual trigger endpoint | Ingests periodically or on-demand via REST API |
| Frontend | **React 18+ / TypeScript / Vite / Tailwind CSS** | Lightning-fast HMR, rich modern UI, strict types |
| Mapping | **Leaflet + OpenStreetMap tiles** via `react-leaflet` | Free, interactive, beautiful custom risk markers |
| Icons | **Lucide React** | Clean, modern hazard and UI icons |
| Delivery channels | **Telegram Bot API** (if token provided) + **In-Memory Mock Dispatcher** + **WhatsApp Share Link Generator** | Guaranteed working output even without live API credentials |

---

## 4. Prototype Architecture Snapshot

```
                       ┌─────────────────────────┐
                       │  Open-Meteo Flood API   │ (Live free data)
                       │           OR            │
                       │ Synthetic Scenario Mock │ (Instant demo switcher)
                       └───────────┬─────────────┘
                                   │ HTTP
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ KESTREL PROTOTYPE BACKEND (Spring Boot 3.4, Port 8080)                 │
│                                                                        │
│  [Hazard Ingest] ──► [Exceedance & Rules Engine] ──► [Alert State]    │
│   (Open-Meteo /       (2yr/5yr/20yr thresholds,        (Severity       │
│    Simulator)          hysteresis & persistence)        machine)       │
│                                                            │           │
│  [Community Reports] ──────────────────────────┐           ▼           │
│   (Resident ground reports)                    ▼   [Alert Composer]    │
│                                       [Provenance: (EN / HI / MR)      │
│  [Registry & Subscriptions]           MODEL_ONLY /         │           │
│   (Pilot locations & residents)       MODEL_PLUS]          ▼           │
│                                                    [Alert Dispatcher]  │
│                                                    (Telegram / Mock /  │
│                                                     WhatsApp Link)     │
└──────────────────────────────────▲─────────────────────────────────────┘
                                   │ REST API (JSON)
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ KESTREL PROTOTYPE FRONTEND (React + Vite + Tailwind, Port 5173)        │
│                                                                        │
│  • Demo Simulation Bar (Calm / Watch / Warning / Severe / Live)        │
│  • Coordinator Dashboard (Leaflet Map, Hazard Metrics, 1-Tap Broadcast)│
│  • Resident View (Ward Selector, Multilingual Advice, Ground Report)   │
│  • Live Dispatch Feed & WhatsApp Forward Modal                         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Repository Layout (`prototype/`)

```
prototype/
├─ master.md                           # This driver prompt file
├─ checklist.md                        # Living progress & task tracker
├─ 01_architecture_and_contracts.md    # System design, DTOs, REST API endpoints
├─ 02_ingest_and_rules.md              # Open-Meteo, Gumbel exceedance, state machine
├─ 03_registry_and_dispatch.md         # Locations, subscriptions, i18n, Telegram/WhatsApp
├─ 04_frontend_ui.md                   # React architecture, Leaflet map, dashboard & resident UI
├─ 05_demo_scenarios_and_testing.md    # Step-by-step demo script & test suite guide
├─ backend/                            # Spring Boot 3 single application
│  ├─ pom.xml                          # Maven configuration
│  └─ src/main/java/com/kestrel/prototype/
│     ├─ domain/                       # Pure logic: rules, exceedance, state machine
│     ├─ ingest/                       # Open-Meteo client & scenario generator
│     ├─ registry/                     # Location catalog & resident subscriptions
│     ├─ rules/                        # Rule evaluator service & persistence
│     ├─ dispatch/                     # Multilingual templates, Telegram, WhatsApp link, Mock
│     └─ web/                          # REST controllers & simulation endpoints
└─ frontend/                           # Vite + React + TypeScript app
   ├─ package.json
   ├─ src/
   │  ├─ components/                   # Map, SeverityBadge, ProvenanceBadge, ForecastChart
   │  ├─ views/                        # CoordinatorDashboard, ResidentView, SimulationBar
   │  ├─ services/                     # API client
   │  └─ i18n/                         # en, hi, mr translation dictionaries
```

---

## 6. Backend Standards (Prototype)

1. **Domain Isolation**: Pure calculation logic (`ExceedanceCalculator`, `GumbelThresholds`, `SeverityStateMachine`) lives in `domain` with zero Spring dependencies, covered by fast unit tests.
2. **REST API**: Clean JSON endpoints under `/api/v1/**` with proper HTTP status codes.
3. **Simulation Endpoints**: Dedicated `/api/v1/simulate/**` endpoints to inject scenarios (`CALM`, `WATCH`, `WARNING`, `SEVERE`, `RESET`) for seamless live demos.
4. **Resilience & Fallbacks**: If Open-Meteo is unreachable or times out, gracefully return last-good cached forecast with an incremented `dataAge` indicator, or fallback to simulated data.
5. **No Secrets in Git**: Telegram token configured via `application.yml` or `TELEGRAM_BOT_TOKEN` environment variable. If missing, automatically route messages to the in-memory `MockDispatcher` without failing.

---

## 7. Frontend Standards (Prototype)

1. **Rich Aesthetics**: Deep, professional palette (slate `#0b0f19`, glassmorphic cards, vivid emerald/amber/orange/red badges). Avoid plain unstyled HTML.
2. **Interactive Map**: Leaflet map centered on India displaying 4 pilot locations with pulsing markers indicating severity.
3. **Language Switcher**: Instant switching between English, Hindi, and Marathi across the entire interface.
4. **Coordinator 1-Tap Broadcast**: One modal showing the auto-generated alert in 3 languages, recipient count, and two big action buttons: `Send to Telegram / Mock` and `Share to WhatsApp`.
5. **Resident Experience**: Clear, calm, accessible mobile-friendly view with a 2-click ground reporting tool ("Water rising on main road", "Drainage blocked").

---

## 8. Canonical Enums (Strict)

- `Hazard`: `FLOOD`, `HEAT`, `AIR_QUALITY` (Flood implemented first; Heat/AQ ready in schema)
- `Severity`: `NONE`, `WATCH`, `WARNING`, `SEVERE`
- `Provenance`: `MODEL_ONLY`, `MODEL_PLUS_REPORTS`, `OFFICIAL_CONFIRMED`
- `Channel`: `TELEGRAM`, `MOCK`, `WEB_PUSH`
- `Language`: `en`, `hi`, `mr`
- `ReportCategory`: `WATER_RISING`, `ROAD_BLOCKED`, `NEED_HELP`, `OTHER`
- `SimulationScenario`: `LIVE_OPEN_METEO`, `CALM`, `WATCH_2YR`, `WARNING_5YR`, `SEVERE_20YR`

---

## 9. Definition of Done (Every Task)

- [ ] Code compiles without warnings (`./mvnw test-compile` / `npm run build`).
- [ ] Required unit or component tests pass.
- [ ] Application starts cleanly and endpoints return valid JSON.
- [ ] `prototype/checklist.md` updated with task ticked, date, and verification notes.
- [ ] Standard completion block outputted at the end of the response.

---

## 10. Doc Map

| If your task is about | Read this document |
|---|---|
| Architecture, REST endpoints, DTO models, Enums | [`prototype/01_architecture_and_contracts.md`](file:///d:/Kestrel/prototype/01_architecture_and_contracts.md) |
| Open-Meteo ingest, Gumbel math, Scenario simulator, Rules engine | [`prototype/02_ingest_and_rules.md`](file:///d:/Kestrel/prototype/02_ingest_and_rules.md) |
| Locations, Subscriptions, Multilingual templates, Telegram, WhatsApp | [`prototype/03_registry_and_dispatch.md`](file:///d:/Kestrel/prototype/03_registry_and_dispatch.md) |
| React UI, Leaflet map, Coordinator dashboard, Resident page | [`prototype/04_frontend_ui.md`](file:///d:/Kestrel/prototype/04_frontend_ui.md) |
| Interactive demo walkthrough, test scenarios, execution scripts | [`prototype/05_demo_scenarios_and_testing.md`](file:///d:/Kestrel/prototype/05_demo_scenarios_and_testing.md) |

---

## 11. Phases

- **Phase 0 — Prototype Foundation & Setup**: Spring Boot 3 + React Vite scaffolding, pilot locations seed, single run command.
- **Phase 1 — Ingest, Simulation & Rules Engine**: Open-Meteo client, scenario generator, exceedance math, severity state machine.
- **Phase 2 — Registry, Multilingual Alerting & Dispatch**: Subscriptions, EN/HI/MR templates, WhatsApp URL generator, Telegram/Mock dispatch, Ground reports.
- **Phase 3 — Coordinator Dashboard & Resident UI**: Leaflet map, live status cards, forecast chart, 1-tap broadcast modal, resident reporting.
- **Phase 4 — End-to-End Demo Scenarios & Polish**: Verification of 5 live scenarios, styling polish, quickstart guide.

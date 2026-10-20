# CHECKLIST.md — Kestrel Prototype Progress Tracker

> **Living file.** The agent updates it at the end of EVERY task (see `prototype/master.md` §0 and §9).
> Tick format: `- [x] PT1-02 — … · ✔ 2026-10-21 · note/evidence`. Never tick without passing build and tests.

---

## ▶ CURRENT STATUS (update every task)

| Field | Value |
|---|---|
| Current phase | **Phase 0 — Prototype Foundation & Setup** |
| Last completed task | — |
| In progress | — |
| Next task | **PT0-01** |
| Blockers / questions | none |
| Last updated | 2026-10-21 |
| Build status (backend / frontend) | not started / not started |

Progress: Phase 0 `0/4` · Phase 1 `0/6` · Phase 2 `0/5` · Phase 3 `0/6` · Phase 4 `0/4` (Total: `0/25`)

---

## 📌 DECISION LOG (Prototype Specific)

| ID | Decision | Rationale | Status |
|---|---|---|---|
| PT-D-01 | **Spring Boot 3.4 + Java 21 single service** | Zero AWS Lambda or container overhead; fast local startup; reuses domain logic | locked |
| PT-D-02 | **Vite + React 18 + TypeScript + Tailwind CSS** | Instant hot-reload, strict typing, responsive glassmorphic modern UI | locked |
| PT-D-03 | **Embedded H2 Database with JPA** | Zero installation; automatic schema generation; pre-seeded with Indian pilot locations | locked |
| PT-D-04 | **In-Process Spring Events** | Replaces SQS/SNS for decoupled pub-sub inside the prototype without broker infra | locked |
| PT-D-05 | **Live Open-Meteo API + Synthetic Scenario Switcher** | Real data by default, but provides deterministic instant demo triggers (Calm/Watch/Warning/Severe) | locked |
| PT-D-06 | **Dual Delivery Channel: Telegram Bot + Mock In-Memory Queue** | Works with real Telegram if token provided; otherwise records to an in-app visible feed | locked |
| PT-D-07 | **Client-side WhatsApp Share Link (`wa.me/?text=...`)** | Coordinator can forward local-language alert text directly to community WhatsApp groups in 1 tap | locked |
| PT-D-08 | **4 Pilot Indian Locations Pre-Seeded** | Mumbai Kurla (Mithi River), Patna Ward 12 (Ganges), Guwahati (Brahmaputra), Cuttack (Mahanadi) | locked |
| PT-D-09 | **3 Canonical Languages (`en`, `hi`, `mr`)** | Real bilingual and trilingual alert templates ready out of the box | locked |
| PT-D-10 | **Leaflet + OpenStreetMap for Maps** | Free, lightweight, zero API key required, customizable pulsing hazard markers | locked |
| PT-D-11 | **Local Ports: Backend 8080, Frontend 5173** | Standard Vite proxy configuration forwarding `/api` calls to Spring Boot | locked |
| PT-D-12 | **Ground Reports Upgrade Provenance to `MODEL_PLUS_REPORTS`** | Resident reports do not alter severity directly, but immediately corroborate model guidance | locked |

---

## 🔎 VERIFICATION REGISTER

| ID | What to verify | Needed before | Status |
|---|---|---|---|
| PT-VERIFY-01 | Open-Meteo Flood API parameter syntax (`ensemble=true`, `daily=river_discharge...`) | PT1-01 | DONE (verified: `https://flood-api.open-meteo.com/v1/flood?latitude=...&longitude=...&daily=river_discharge&ensemble=true&forecast_days=10`) |
| PT-VERIFY-02 | Telegram Bot API `sendMessage` payload format and MarkdownV2 escaping rules | PT2-04 | DONE (verified: `POST https://api.telegram.org/bot<TOKEN>/sendMessage` with `chat_id` and `text`) |
| PT-VERIFY-03 | WhatsApp share link syntax (`https://wa.me/?text=` vs `api.whatsapp.com/send?text=`) | PT2-03 | DONE (verified: `https://wa.me/?text=` with standard URL encoding works across mobile and web) |
| PT-VERIFY-04 | Accurate Hindi and Marathi emergency alert wording for flood warnings | PT2-02 | DONE (reviewed standard disaster terminology from NDMA / SDMA guidelines) |

---

## 🚧 BLOCKERS / QUESTIONS FOR THE USER
*(none yet — agent appends here with date and task ID)*

---

## PHASE 0 — Prototype Foundation & Setup `docs: 01`

- [ ] **PT0-01** — Initialize `prototype/backend` Spring Boot 3.4 project (Maven wrapper, Java 21, Spring Web, Spring Data JPA, H2 Database, Validation, Lombok/Records, OpenAPI). Verify `./mvnw clean compile` succeeds. · deps: —
- [ ] **PT0-02** — Initialize `prototype/frontend` React + Vite + TypeScript project (Tailwind CSS, Lucide React, Leaflet, React-Leaflet, Axios). Verify `npm run build` succeeds. · deps: —
- [ ] **PT0-03** — Create seed dataset for 4 Indian pilot locations (`Mumbai Kurla`, `Patna Ward 12`, `Guwahati Riverside`, `Cuttack Mahanadi`) with coordinates, return periods (2-yr, 5-yr, 20-yr in m³/s), and regional language metadata. Auto-load into H2 on startup. · doc 01 §4, doc 03 §1 · deps: PT0-01
- [ ] **PT0-04** — Configure Vite reverse proxy to `http://localhost:8080/api` and write single-command start scripts (`start-prototype.bat` / `start-prototype.sh`). Verify frontend can fetch seeded locations from backend. · doc 01 §2 · deps: PT0-01, PT0-02, PT0-03

---

## PHASE 1 — Ingest, Simulation & Rules Engine `docs: 01, 02`

- [ ] **PT1-01** — Implement Open-Meteo Flood client (`OpenMeteoClient`) using Spring `RestClient` to fetch 10-day ensemble river discharge forecasts for monitored coordinates. Include error handling and fallback cache. · doc 02 §1 · deps: PT0-03
- [ ] **PT1-02** — Build deterministic `ScenarioSimulator` supporting 5 modes (`LIVE_OPEN_METEO`, `CALM`, `WATCH_2YR`, `WARNING_5YR`, `SEVERE_20YR`) for instant demo triggering. · doc 02 §2 · deps: PT1-01
- [ ] **PT1-03** — Implement `ExceedanceCalculator` (pure domain): compute daily ensemble exceedance share `(# members > threshold) / memberCount` for 2-yr, 5-yr, 20-yr levels, and forecast statistics (min, p25, median, p75, max). Unit test with known vectors. · doc 02 §3 · deps: PT1-01
- [ ] **PT1-04** — Implement `SeverityStateMachine` with hysteresis: persistence runs (`WATCH: 1`, `WARNING: 2`, `SEVERE: 2`), de-escalation (2 runs step-down), cooldown (12h), and stale data guard (>36h data cannot escalate). · doc 02 §4 · deps: PT1-03
- [ ] **PT1-05** — Build `HazardEngineService` and REST endpoints: `GET /api/v1/status` (current alerts, metrics, provenance, dataAge) and `GET /api/v1/forecast/{locationId}` (detailed 10-day band chart data). · doc 01 §3, doc 02 §5 · deps: PT1-04
- [ ] **PT1-06** — Build simulation REST endpoints: `POST /api/v1/simulate/scenario` (`scenario=WARNING_5YR`) and `POST /api/v1/simulate/reset`. Verify state transitions immediately trigger and update `/api/v1/status`. · doc 02 §2 · deps: PT1-05

---

## PHASE 2 — Registry, Multilingual Alerting & Dispatch `docs: 01, 03`

- [ ] **PT2-01** — Build resident subscription persistence & REST endpoints (`POST /api/v1/public/subscriptions`, `GET /api/v1/public/subscriptions/{id}`, `DELETE ...`) supporting preferred language (`en`, `hi`, `mr`), channel (`TELEGRAM`, `MOCK`), and minSeverity filter. · doc 03 §2 · deps: PT0-03
- [ ] **PT2-02** — Implement `AlertTemplateComposer`: renders localized alert messages in English, Hindi, and Marathi with clear severity prefix, actionable safety instructions, and mandatory provenance disclaimer. Unit test each language output. · doc 03 §3 · deps: PT1-05
- [ ] **PT2-03** — Implement `WhatsAppShareService`: generates properly encoded `https://wa.me/?text=...` URLs with emoji badges and bilingual alert text ready for coordinator one-click forwarding. · doc 03 §4 · deps: PT2-02
- [ ] **PT2-04** — Build `AlertDispatcher`: sends to Telegram Bot API if `TELEGRAM_BOT_TOKEN` is configured; always records all dispatches to an in-memory observable `MockDispatchQueue` for real-time frontend inspection (`GET /api/v1/dispatch/recent`). · doc 03 §5 · deps: PT2-02
- [ ] **PT2-05** — Implement Community Ground Report intake (`POST /api/v1/public/reports`) with categories (`WATER_RISING`, `ROAD_BLOCKED`, `NEED_HELP`). Automatically upgrades active alert provenance from `MODEL_ONLY` to `MODEL_PLUS_REPORTS`. · doc 03 §6 · deps: PT1-05

---

## PHASE 3 — Coordinator Dashboard & Resident UI `docs: 01, 04`

- [ ] **PT3-01** — Scaffold UI layout: Topbar with role toggle (Coordinator vs Resident) & Language picker (`EN`, `हिंदी`, `मराठी`), and a sticky **Demo Simulation Control Bar** (quick buttons: Calm, 2-Yr Watch, 5-Yr Warning, 20-Yr Severe, Live Data). · doc 04 §2 · deps: PT0-04, PT1-06
- [ ] **PT3-02** — Implement interactive Leaflet Hazard Map: center on India, render 4 pilot locations with animated pulsing markers colored by severity (`green`, `amber`, `orange`, `red`), click to select location. · doc 04 §3 · deps: PT3-01, PT1-05
- [ ] **PT3-03** — Build Location Hazard Detail Panel: `SeverityBadge` (color + icon + label), `ProvenanceBadge` (`MODEL_ONLY` / `MODEL_PLUS_REPORTS`), `DataAge` indicator, and a lightweight SVG 10-day ensemble forecast chart showing the spread vs return-period thresholds. · doc 04 §4 · deps: PT3-02
- [ ] **PT3-04** — Build Coordinator One-Tap Broadcast Modal: preview alert in 3 languages, view estimated recipient count, click "Broadcast Alert" to dispatch, and click "Share to WhatsApp" to open WhatsApp with pre-filled message. · doc 04 §5 · deps: PT3-03, PT2-03, PT2-04
- [ ] **PT3-05** — Build Resident Public View: simple mobile-first card showing ward hazard status in chosen language, "Subscribe for Alerts" modal, and a "Report Ground Situation" 2-click submission modal. · doc 04 §6 · deps: PT3-01, PT2-01, PT2-05
- [ ] **PT3-06** — Build Live Dispatch Activity Drawer: slide-over feed showing all recent dispatches in real-time with channel badge, recipient, timestamp, and message preview. · doc 04 §7 · deps: PT3-04, PT2-04

---

## PHASE 4 — End-to-End Demo Scenarios & Polish `docs: 05`

- [ ] **PT4-01** — Implement automated integration test verifying the full pipeline: Simulate 5-Yr Flood → Rule transitions to WARNING → Provenance is MODEL_ONLY → Resident submits report → Provenance becomes MODEL_PLUS_REPORTS → Broadcast dispatched to Mock Queue. · doc 05 §2 · deps: PT1-06, PT2-05, PT2-04
- [ ] **PT4-02** — Visual & UX polish: ensure WCAG AA contrast, smooth transitions, mobile responsiveness, empty states, and crisp badge styling across dark theme. · doc 04 §1 · deps: PT3-06
- [ ] **PT4-03** — Write interactive Demo Guide in `prototype/README.md` with 5 quick demonstration scripts and one-click launch instructions. · doc 05 §1 · deps: PT4-01
- [ ] **PT4-04 — GATE PT-GATE:** Run full demo walk-through (Calm → Warning → Report → Broadcast → WhatsApp → All-Clear). Verify backend tests green, frontend build clean, zero console errors. · doc 05 §3 · deps: all PT tasks

# 05 — Multi-Service Orchestration, Testing & Demo Guide

This document provides the startup scripts, automated testing specifications, and interactive 5-scenario demo walkthrough for the **Kestrel Microservices Prototype**.

---

## 1. Multi-Service Orchestration & Startup

The prototype backend consists of 5 independent Spring Boot microservices behind a Spring Cloud Gateway, plus the Vite React Redux frontend.

### 1.1 Port Allocation Matrix
| Service | Module Directory | Port | Health Check Endpoint |
|---|---|---|---|
| **API Gateway** | `backend/services/api-gateway` | `8080` | `http://localhost:8080/actuator/health` |
| **Subscriber Registry** | `backend/services/subscriber-registry` | `8081` | `http://localhost:8081/actuator/health` |
| **Flood Ingest** | `backend/services/flood-ingest` | `8082` | `http://localhost:8082/actuator/health` |
| **Rules Engine** | `backend/services/rules-engine` | `8083` | `http://localhost:8083/actuator/health` |
| **Alert Fanout** | `backend/services/alert-fanout` | `8084` | `http://localhost:8084/actuator/health` |
| **Frontend UI** | `frontend` | `5173` | `http://localhost:5173` |

### 1.2 One-Command Startup Script (`start-microservices.bat`)
Run from the `prototype/` directory on Windows:
```cmd
@echo off
echo ===================================================
echo Starting Kestrel Microservices Prototype Stack...
echo ===================================================

echo [1/6] Building Maven Monorepo...
cd backend
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 ( echo Maven build failed! & pause & exit /b %ERRORLEVEL% )

echo [2/6] Starting Subscriber Registry (Port 8081)...
start "Kestrel: Subscriber Registry (8081)" java -jar services/subscriber-registry/target/subscriber-registry-0.0.1-SNAPSHOT.jar

echo [3/6] Starting Flood Ingest (Port 8082)...
start "Kestrel: Flood Ingest (8082)" java -jar services/flood-ingest/target/flood-ingest-0.0.1-SNAPSHOT.jar

echo [4/6] Starting Rules Engine (Port 8083)...
start "Kestrel: Rules Engine (8083)" java -jar services/rules-engine/target/rules-engine-0.0.1-SNAPSHOT.jar

echo [5/6] Starting Alert Fanout (Port 8084)...
start "Kestrel: Alert Fanout (8084)" java -jar services/alert-fanout/target/alert-fanout-0.0.1-SNAPSHOT.jar

echo Waiting for downstream microservices to initialize...
timeout /t 6 /nobreak > nul

echo [6/6] Starting Spring Cloud Gateway (Port 8080)...
start "Kestrel: API Gateway (8080)" java -jar services/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar

cd ..\frontend
echo Starting Vite Frontend with Redux Toolkit (Port 5173)...
start "Kestrel: Frontend (5173)" cmd /c "npm run dev"

echo ===================================================
echo Kestrel Microservices Stack is Running!
echo Frontend: http://localhost:5173
echo API Gateway: http://localhost:8080
echo ===================================================
pause
```

---

## 2. Automated Testing Suite

The testing strategy covers domain math, event serialization, microservice slice contracts, and Redux client state:

### 2.1 Backend Automated Tests (`./mvnw test`)
- **`libs/common-events`**:
  - `EventSerializationTest`: Verifies Jackson round-trip serialization and deserialization of `FloodSignal`, `AlertEvent`, and `LocationUpserted` with ISO-8601 UTC timestamps.
- **`services/flood-ingest`**:
  - `ExceedanceCalculatorTest`: Tests dynamic member arrays (50 vs 51 members) against return period discharge levels.
  - `ScenarioSimulatorTest`: Verifies deterministic values for `CALM`, `WATCH`, `WARNING`, and `SEVERE`.
- **`services/rules-engine`**:
  - `SeverityStateMachineTest`: Table-driven tests covering persistence (candidate `WARNING` requires 2 runs), hysteresis (de-escalating from `SEVERE` steps through `WARNING` before `NONE`), 12h cooldown, and stale data guard (>36h).
- **`services/alert-fanout`**:
  - `AlertTemplateComposerTest`: Verifies placeholder substitution and mandatory provenance disclaimer across English, Hindi, and Marathi templates.
  - `WhatsAppShareUrlTest`: Verifies valid UTF-8 encoding for `https://wa.me/?text=...`.

### 2.2 Frontend Automated Tests (`npm run test` & `npm run typecheck`)
- TypeScript compiler verification with zero `any` or strict errors (`tsc --noEmit`).
- Redux slice tests verifying state transitions for `authSlice`, `uiSlice`, and `mapSlice`.
- React component tests for `SeverityBadge`, `ProvenanceBadge`, and `ForecastBandChart`.

---

## 3. The 5 Golden Demo Scenarios

These 5 scenarios walk through the entire microservices lifecycle to prove Kestrel's value to stakeholders in under 5 minutes.

### Scenario 1: Calm Baseline & Live Open-Meteo Sync
1. Open the frontend at `http://localhost:5173`.
2. Notice the Leaflet map centered on India with 4 pilot markers (Mumbai, Patna, Guwahati, Cuttack) glowing in **Emerald Green** (`NONE / Normal`).
3. Switch languages via the header dropdown: **English**, **हिंदी**, and **मराठी**. Notice all labels, headers, and tooltips update instantly via `uiSlice`.
4. In the network tab, observe RTK Query polling `GET http://localhost:8080/api/v1/rules/status` every 5 seconds.

### Scenario 2: Simulated Monsoon Flood Warning (Orange / 5-Yr Flood)
1. Select **Kurla Ward L, Mumbai** from the map or list.
2. In the sticky top Simulation Bar, click **`[Simulate Warning (5-Yr)]`**.
3. **What happens under the hood**:
   - Frontend triggers `useSimulateScenarioMutation` $\to$ Gateway $\to$ `flood-ingest` (8082).
   - `flood-ingest` injects the synthetic 5-year discharge crest and publishes `FloodSignalPayload`.
   - `rules-engine` (8083) receives the signal, calculates exceedance ($S_5 = 0.68 \ge 0.30$), transitions state to `WARNING`, and emits `AlertEventPayload(stage=DECISION)`.
   - On the next 5s poll, RTK Query receives the updated status and updates the Redux store.
4. **Visual Observation**:
   - The Mumbai marker on the Leaflet map turns **Orange** and begins pulsing with an animated CSS ripple (`animate-ping`).
   - The `SeverityBadge` changes to **`Warning (5-Yr Flood)`**.
   - The SVG `ForecastBandChart` shows the ensemble spread breaching the 5-year threshold line ($85 \text{ m}^3/\text{s}$).
   - The `ProvenanceBadge` reads **`MODEL_ONLY`** (Blue badge).

### Scenario 3: Community Corroboration & Provenance Upgrade
1. In the header, toggle the view to **Resident Public View**.
2. Select **Kurla Ward L, Mumbai** and change language to **मराठी**.
3. The resident sees a bold emergency alert card: *"⚠️ केस्ट्रेल पूर इशारा — पुढील ४८ तासांत मिठी नदीची पातळी वाढण्याची शक्यता."*
4. The resident clicks the **"Report Ground Situation"** button.
5. In the modal, they select **"Water rising rapidly on main road"** and click **"Submit Report"**.
6. **Under the hood**:
   - Frontend calls `useSubmitGroundReportMutation` $\to$ Gateway $\to$ `alert-fanout` (8084).
   - `alert-fanout` persists the report and immediately upgrades the location provenance to `MODEL_PLUS_REPORTS`.
7. Switch back to **Coordinator Dashboard**:
   - Notice the `ProvenanceBadge` has automatically updated from `MODEL_ONLY` to **`MODEL_PLUS_REPORTS`** (Purple badge with community icon).
   - The coordinator sees "1 Ground Report Received" with the resident's submission time.

### Scenario 4: One-Tap Broadcast & WhatsApp Relay
1. As the coordinator, click **"Broadcast Alert"** on the active Mumbai card.
2. The modal previews the generated alert in 3 tabs: **English**, **Hindi**, and **Marathi**.
3. Click **"Share to WhatsApp"**:
   - A new browser tab opens `https://wa.me/?text=...` with the pre-formatted bilingual alert text and official provenance disclaimer ready to send to community WhatsApp groups.
4. Click **"Send Broadcast"**:
   - The modal closes with a green success toast.
   - In the bottom **Live Dispatch Activity Feed**, observe new entries dispatched to Telegram and Mock channels from `useGetRecentDispatchesQuery`.

### Scenario 5: De-escalation & "All Clear"
1. In the Simulation Bar, click **`[Calm Baseline]`**.
2. `flood-ingest` resets forecasts to normal levels and emits a calm `FloodSignal`.
3. `rules-engine` applies hysteresis and de-escalates state back to `NONE`.
4. The Leaflet marker returns to Emerald Green, and an "All-Clear" log is recorded in the dispatch feed.

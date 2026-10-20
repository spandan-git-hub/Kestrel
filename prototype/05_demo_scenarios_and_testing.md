# 05 — Demo Scenarios, Testing & Verification

This document provides the interactive 5-scenario demo script, automated test specifications, and launch instructions for the **Kestrel Prototype**.

---

## 1. The 5 Golden Demo Scenarios

These 5 scenarios walk through the entire lifecycle of an alert to prove Kestrel's core value proposition to stakeholders in under 5 minutes.

### Scenario 1: Calm Baseline & Live Open-Meteo Sync
1. Open the application at `http://localhost:5173`.
2. Notice the Leaflet map centered on India with 4 pilot markers (Mumbai, Patna, Guwahati, Cuttack) glowing in **Emerald Green** (`NONE / Normal`).
3. Click through the language toggle in the header: **English**, **हिंदी**, and **मराठी**. Notice all labels, status cards, and headers instantly translate.
4. Verify the `DataAge` badge displays "Live Open-Meteo Feed: Fresh (Updated < 1h ago)".

### Scenario 2: Simulated Monsoon Flood Warning (Orange / 5-Yr Flood)
1. Select **Kurla Ward L, Mumbai** from the map or list.
2. In the sticky top Simulation Bar, click **`[Simulate Warning (5-Yr)]`**.
3. **Observations**:
   - The Mumbai marker on the Leaflet map turns **Orange** and begins pulsing with an animated ripple effect.
   - The `SeverityBadge` changes to **`Warning (5-Yr Flood)`**.
   - The SVG `ForecastBandChart` displays a sudden discharge surge at Day 2 breaching the 5-year threshold line ($85 \text{ m}^3/\text{s}$).
   - The `ProvenanceBadge` reads **`MODEL_ONLY`** (Blue badge).

### Scenario 3: Community Corroboration & Provenance Upgrade
1. In the top header, switch the toggle from **Coordinator Dashboard** to **Resident Public View**.
2. Select **Kurla Ward L, Mumbai** and change the language to **मराठी**.
3. The resident sees a bold emergency warning: *"⚠️ केस्ट्रेल पूर इशारा — पुढील ४८ तासांत मिठी नदीची पातळी वाढण्याची शक्यता."*
4. The resident clicks the **"Report Ground Situation"** button.
5. In the modal, they select **"Water rising rapidly on main road"** and click **"Submit Report"**.
6. Switch back to **Coordinator Dashboard**:
   - Notice the `ProvenanceBadge` has automatically updated from `MODEL_ONLY` to **`MODEL_PLUS_REPORTS`** (Purple badge with community icon).
   - The coordinator sees "1 Ground Report Received" with the resident's timestamp.

### Scenario 4: One-Tap Broadcast & WhatsApp Relay
1. As the coordinator, click **"Broadcast Alert"** on the active Mumbai card.
2. The modal previews the generated alert in 3 tabs: **English**, **Hindi**, and **Marathi**.
3. Click **"Share to WhatsApp"**:
   - A new browser tab opens `https://wa.me/?text=...` with the pre-formatted bilingual alert text and official provenance disclaimer ready to send to community WhatsApp groups.
4. Click **"Send Broadcast"**:
   - The modal closes with a green success toast.
   - In the bottom **Live Dispatch Activity Feed**, observe new entries dispatched to Telegram and Mock channels.

### Scenario 5: De-escalation & "All Clear"
1. In the Simulation Bar, click **`[Calm Baseline]`**.
2. Discharge forecasts return to normal levels.
3. The state machine applies hysteresis (requiring confirmation runs) and then issues a de-escalation transition.
4. The status returns to `NONE` (Normal / Safe), and an "All-Clear" log is recorded in the dispatch feed.

---

## 2. Automated Testing Suite

The prototype includes targeted automated tests covering domain calculations, API contracts, and component rendering.

### 2.1 Backend Unit & Integration Tests (`mvn test`)
- **`ExceedanceCalculatorTest`**:
  - Tests exceedance share with 50 ensemble members against 2-yr, 5-yr, and 20-yr thresholds.
  - Verifies exact GloFAS $0.30$ consensus trigger boundary ($14/50 = 0.28$ false vs $15/50 = 0.30$ true).
- **`SeverityStateMachineTest`**:
  - Verifies persistence: candidate `WARNING` requires 2 runs before escalating from `NONE`.
  - Verifies hysteresis: candidate `NONE` requires 2 runs before de-escalating from `WARNING`.
  - Verifies stale data guard: signal older than 36h never escalates severity.
- **`AlertTemplateComposerTest`**:
  - Verifies correct placeholder substitution and formatting in `en`, `hi`, and `mr`.
  - Ensures mandatory provenance disclaimer is present in every template.
- **`WhatsAppShareServiceTest`**:
  - Verifies generated URL begins with `https://wa.me/?text=` and URL encoding is valid UTF-8.

### 2.2 Frontend Verification (`npm test` & `npm run build`)
- Compiles with zero TypeScript errors (`tsc --noEmit`).
- Verifies all 3 language bundles contain identical translation keys.
- Confirms production bundle builds cleanly under 300 KB.

---

## 3. Quickstart & Single-Command Launch

### 3.1 Prerequisites
- **Java 21 (LTS)** installed (`java -version`).
- **Node.js 18+** installed (`node -v`).

### 3.2 Running the Prototype

#### Option A: One-Click Windows Script
Run the root batch script from the `prototype/` directory:
```powershell
.\start-prototype.bat
```

#### Option B: Manual Terminal Execution
1. **Start Backend (Port 8080)**:
   ```bash
   cd prototype/backend
   ./mvnw spring-boot:run
   ```
2. **Start Frontend (Port 5173)**:
   ```bash
   cd prototype/frontend
   npm install
   npm run dev
   ```
3. Open your browser at **`http://localhost:5173`**.

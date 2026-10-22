# 02 — Ingest & Rules Microservices (`flood-ingest` & `rules-engine`)

This document provides the technical specification, domain models, algorithms, and event flows for the **`flood-ingest`** (Port 8082) and **`rules-engine`** (Port 8083) Spring Boot microservices.

---

## 1. `flood-ingest` Microservice (Port 8082)

The `flood-ingest` microservice is an independent Spring Boot application responsible for pulling live river-discharge forecasts, calculating return-period thresholds, running synthetic demo scenarios, and emitting `FloodSignal` events.

### 1.1 Service Structure
- **Package**: `com.kestrel.floodingest`
- **Port**: `8082`
- **Database**: H2 isolated instance (`jdbc:h2:mem:kestrel_flood_db`)
- **Key Entities**:
  - `MonitoredPointReplica`: Local read-replica of monitored coordinates synced via `LocationUpsertedEvent`.
  - `FloodSnapshot`: Latest 10-day forecast run per location with timestamp and hash.

### 1.2 Open-Meteo Flood API Client (`OpenMeteoClient`)
- **Base URL**: `https://flood-api.open-meteo.com/v1/flood`
- **Request Parameters**:
  - `latitude`, `longitude`
  - `daily=river_discharge,river_discharge_median,river_discharge_max,river_discharge_min,river_discharge_p25,river_discharge_p75`
  - `ensemble=true`
  - `forecast_days=10`
- **Dynamic Member Parsing**:
  GloFAS ensemble runs may contain 50 or 51 members. The parser dynamically reads all keys starting with `river_discharge_member`:
  ```java
  Map<String, List<Double>> members = new HashMap<>();
  dailyNode.fieldNames().forEachRemaining(field -> {
      if (field.startsWith("river_discharge_member")) {
          List<Double> discharge = new ArrayList<>();
          dailyNode.get(field).forEach(val -> discharge.add(val.asDouble()));
          members.put(field, discharge);
      }
  });
  int totalMembers = members.size(); // 50 or 51
  ```
- **Resilience**: If the API call times out or returns HTTP 429/5xx, `flood-ingest` logs a warning, sets `isStale=true`, and serves the last-good cached forecast from H2.

### 1.3 5-Mode Scenario Simulator (`ScenarioSimulator`)
To enable instant, deterministic demonstrations for evaluators, `flood-ingest` provides a simulation injector triggered via `POST /api/v1/flood/simulate`:

1. **`LIVE_OPEN_METEO`**: Calls live Open-Meteo API for real coordinates.
2. **`CALM`**: Hydrodynamically quiet baseline ($15 - 35 \text{ m}^3/\text{s}$). Exceedance shares = `0.00`. Severity = `NONE`.
3. **`WATCH_2YR`**: Crests at Day 3. $42\%$ of ensemble members exceed $T_2$. Exceedance shares: $S_2 = 0.42, S_5 = 0.05, S_{20} = 0.00$.
4. **`WARNING_5YR`**: Sharp crest at Day 2. $68\%$ of ensemble members exceed $T_5$. Exceedance shares: $S_2 = 0.95, S_5 = 0.68, S_{20} = 0.15$.
5. **`SEVERE_20YR`**: Extreme catastrophic crest at Day 1–2. $78\%$ of ensemble members exceed $T_{20}$. Exceedance shares: $S_2 = 1.00, S_5 = 0.92, S_{20} = 0.78$.

### 1.4 Publishing `FloodSignal` Event
Upon completing a live fetch or simulated scenario run, `flood-ingest` publishes `FloodSignalPayload` containing the calculated statistics and exceedance shares for all 10 forecast days.

---

## 2. `rules-engine` Microservice (Port 8083)

The `rules-engine` microservice is an independent Spring Boot application that turns raw hazard signals into actionable, hysteresis-backed alert decisions.

### 2.1 Service Structure
- **Package**: `com.kestrel.rulesengine`
- **Port**: `8083`
- **Database**: H2 isolated instance (`jdbc:h2:mem:kestrel_rules_db`)
- **Key Entities**:
  - `AlertState`: Current severity, candidate severity, candidate runs counter, active alert UUID, provenance, and data timestamp.
  - `AlertHistory`: Full audit log of all severity transitions with timestamps and metrics.

### 2.2 Exceedance Math & GloFAS Criteria
For each forecast day $d \in [0 .. \text{horizonDays}]$ (default 7 days):
$$\text{share}_k(d) = \frac{\# \{m \in \text{members} \mid q_m(d) \ge T_k\}}{M}$$
- Standard GloFAS consensus threshold: $\text{share}_k(d) \ge 0.30$ (30% of ensemble members agree).
- Candidate evaluation:
  - If $\exists d: \text{share}_{20}(d) \ge 0.30 \implies \text{Candidate} = \text{SEVERE}$.
  - Else if $\exists d: \text{share}_5(d) \ge 0.30 \implies \text{Candidate} = \text{WARNING}$.
  - Else if $\exists d: \text{share}_2(d) \ge 0.30 \implies \text{Candidate} = \text{WATCH}$.
  - Else $\text{Candidate} = \text{NONE}$.

### 2.3 `SeverityStateMachine` with Hysteresis
To prevent noisy alert flickering, the state machine applies strict hysteresis:

```
        Candidate ≥ WATCH (1 run)
  NONE ──────────────────────────► WATCH
   ▲                                 │ Candidate ≥ WARNING (2 runs)
   │                                 ▼
   │                              WARNING
   │                                 │ Candidate ≥ SEVERE (2 runs)
   │                                 ▼
   │                              SEVERE
   │                                 │
   └── De-escalation: candidate < current for 2 runs (steps down 1 level)
```

1. **Persistence Runs**:
   - Escalating to `WATCH`: 1 run.
   - Escalating to `WARNING`: 2 consecutive runs.
   - Escalating to `SEVERE`: 2 consecutive runs.
   *(Note: Simulation injector calls via `POST /api/v1/flood/simulate` immediately apply candidate persistence to facilitate live testing).*
2. **De-escalation**:
   - If candidate severity is lower than current severity for **2 consecutive runs**, the state steps down **one level at a time** (e.g. `SEVERE` $\to$ `WARNING` $\to$ `WATCH` $\to$ `NONE`).
   - Transition to `NONE` issues an "All-Clear" notification.
3. **Cooldown**:
   - Notifications for identical severity at the same location are suppressed for **12 hours** unless severity escalates.
4. **Stale Data Guard**:
   - If $\text{now} - \text{dataAsOf} > 36\text{ hours}$, the state machine flags `isStale=true` and **refuses to escalate**.

### 2.4 Emitting `AlertEvent` Event
On any severity change, `rules-engine` persists the transition to `AlertState` and `AlertHistory`, and publishes `AlertEventPayload(stage="DECISION", provenance=MODEL_ONLY)`.

---

## 3. Inter-Service Communication Flow

```
1. flood-ingest (8082) fetches/simulates forecast
   │
   ▼ publishes FloodSignalPayload
2. Event Bus delivers to rules-engine (8083)
   │
   ▼ rules-engine evaluates SeverityStateMachine
3. rules-engine persists AlertState in kestrel_rules_db
   │
   ▼ publishes AlertEventPayload(stage=DECISION)
4. Event Bus delivers to alert-fanout (8084) for multilingual delivery
```

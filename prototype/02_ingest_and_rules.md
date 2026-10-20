# 02 — Ingest, Simulation & Rules Engine

This document provides the technical context, algorithms, formulas, and implementation specifications for hazard data ingestion, synthetic scenario generation, and the severity state machine in the **Kestrel Prototype**.

---

## 1. Open-Meteo Flood API Client

The prototype connects directly to the free, public **Open-Meteo Flood API** (powered by GloFAS river routing models).

### 1.1 Endpoint & Query Parameters
- **Base URL**: `https://flood-api.open-meteo.com/v1/flood`
- **Method**: `GET`
- **Parameters**:
  - `latitude`: e.g. `19.0726`
  - `longitude`: e.g. `72.8797`
  - `daily`: `river_discharge,river_discharge_median,river_discharge_max,river_discharge_min,river_discharge_p25,river_discharge_p75`
  - `ensemble`: `true`
  - `forecast_days`: `10`

### 1.2 Parsing Ensemble Members
Open-Meteo returns daily arrays for each ensemble member. Because member counts can vary between model runs (50 vs 51 members), **never hardcode member counts**:
```java
// Extract all keys starting with "river_discharge_member"
Map<String, List<Double>> memberSeries = new HashMap<>();
dailyNode.fields().forEachRemaining(entry -> {
    if (entry.getKey().startsWith("river_discharge_member")) {
        List<Double> values = new ArrayList<>();
        entry.getValue().forEach(v -> values.add(v.asDouble()));
        memberSeries.put(entry.getKey(), values);
    }
});
int totalMembers = memberSeries.size(); // Typically 50 or 51
```

### 1.3 Resilience & Fallback
- **Timeout**: Connect timeout 5s, read timeout 8s.
- **Circuit Breaker / Fallback**: If Open-Meteo returns HTTP 429, 5xx, or times out, the client logs a warning, updates `FeedState` to `DEGRADED`, and serves the **last-good cached snapshot** from H2 with an incremented `dataAgeSeconds`.
- If no previous snapshot exists, it falls back to the deterministic `CALM` simulation fixture so the application never crashes.

---

## 2. Deterministic Scenario Simulator

For live demonstrations, rainfall cannot be predicted. The `ScenarioSimulator` can override the live feed with deterministic, hydrodynamically realistic discharge curves with a single button click.

### 2.1 Supported Scenarios
1. **`LIVE_OPEN_METEO`**:
   Pulls live real-time forecast from Open-Meteo API for the selected location coordinates.
2. **`CALM`** (Baseline):
   Discharge remains low across all 10 days ($15 - 35 \text{ m}^3/\text{s}$). Exceedance shares for 2-yr, 5-yr, 20-yr thresholds are strictly `0.00`. Resulting severity: `NONE`.
3. **`WATCH_2YR`** (Early Signal):
   A mild crest occurs at Day 3. $42\%$ of ensemble members exceed the 2-year threshold. Exceedance share $S_2 = 0.42$, $S_5 = 0.05$, $S_{20} = 0.00$. Resulting severity: `WATCH`.
4. **`WARNING_5YR`** (High Flood Threat):
   Heavy upstream rainfall causing a sharp crest at Day 2. $68\%$ of members exceed the 5-year threshold. Exceedance share $S_2 = 0.95$, $S_5 = 0.68$, $S_{20} = 0.15$. Resulting severity: `WARNING`.
5. **`SEVERE_20YR`** (Extreme Inundation):
   Extreme event cresting at Day 1–2. $78\%$ of members exceed the 20-year return level. Exceedance share $S_2 = 1.00$, $S_5 = 0.92$, $S_{20} = 0.78$. Resulting severity: `SEVERE`.

### 2.2 Synthetic Data Generation Logic
For each day $d \in [0..9]$ and member $m \in [1..50]$:
$$Q(d, m) = Q_{\text{base}} + \Delta Q \cdot \exp\left(-\frac{(d - d_{\text{peak}})^2}{2 \sigma^2}\right) + \epsilon_m$$
Where $\epsilon_m \sim \mathcal{N}(0, \sigma_{\text{spread}})$ models ensemble dispersion.

---

## 3. Exceedance Math & Thresholds

### 3.1 Threshold Definitions
Each pilot location has pre-seeded return period discharge levels:
- $T_2$ (2-Year Flood): Bankfull discharge; minor low-lying waterlogging.
- $T_5$ (5-Year Flood): Moderate flood; roads and ground floors affected.
- $T_{20}$ (20-Year Flood): Severe inundation; structural threat and mass evacuation needed.

### 3.2 Exceedance Share Formula
For day $d$ and return period $k \in \{2, 5, 20\}$:
$$\text{share}_k(d) = \frac{\sum_{m=1}^{M} \mathbb{I}(q_m(d) \ge T_k)}{M}$$
- $M$: Total valid ensemble members in the run.
- $\mathbb{I}$: Indicator function (1 if discharge meets or exceeds threshold $T_k$, else 0).
- GloFAS trigger standard: A threshold is considered breached on day $d$ if $\text{share}_k(d) \ge 0.30$ (30% member consensus).

### 3.3 Forecast Quantiles (for Range Chart)
For each forecast day $d$, compute:
- `qMin`: Minimum discharge across members
- `qP25`: 25th percentile
- `qMedian`: 50th percentile (median)
- `qP75`: 75th percentile
- `qMax`: Maximum discharge across members

---

## 4. Severity State Machine with Hysteresis

The `SeverityStateMachine` transforms noisy model signals into stable, reliable alerts without rapid flickering (flapping).

```
         ┌────────┐
         │  NONE  │◄───────────────────────────┐
         └───┬────┘                            │
             │ Candidate ≥ WATCH               │ Candidate < Current
             │ for 1 run                       │ for 2 consecutive runs
             ▼                                 │ (Emits CANCEL "All Clear")
         ┌────────┐                            │
    ┌───►│ WATCH  │────────────────────────────┤
    │    └───┬────┘                            │
    │        │ Candidate ≥ WARNING             │
    │        │ for 2 consecutive runs          │
    │        ▼                                 │
    │    ┌─────────┐                           │
    ├───►│ WARNING │───────────────────────────┤
    │    └───┬─────┘                           │
    │        │ Candidate ≥ SEVERE              │
    │        │ for 2 consecutive runs          │
    │        ▼                                 │
    │    ┌─────────┐                           │
    └───►│ SEVERE  │───────────────────────────┘
         └─────────┘
```

### 4.1 Transition Rules
1. **Candidate Assessment**:
   - Evaluate days $d \in [0 .. \text{horizonDays}]$ (default 7 days).
   - If any day has $\text{share}_{20} \ge 0.30 \implies \text{Candidate} = \text{SEVERE}$.
   - Else if any day has $\text{share}_5 \ge 0.30 \implies \text{Candidate} = \text{WARNING}$.
   - Else if any day has $\text{share}_2 \ge 0.30 \implies \text{Candidate} = \text{WATCH}$.
   - Else $\text{Candidate} = \text{NONE}$.

2. **Escalation (Persistence Requirement)**:
   - Escalating from $\text{NONE} \to \text{WATCH}$: Requires **1 run**.
   - Escalating to $\text{WARNING}$: Requires **2 consecutive runs** with $\text{Candidate} \ge \text{WARNING}$.
   - Escalating to $\text{SEVERE}$: Requires **2 consecutive runs** with $\text{Candidate} \ge \text{SEVERE}$.
   - *Demo Note*: When a simulation scenario is triggered via `POST /api/v1/simulate/scenario`, it forces the state machine into that level immediately to allow instant interactive testing.

3. **De-escalation (Hysteresis Requirement)**:
   - If $\text{Candidate} < \text{Current}$, it requires **2 consecutive runs** of lower candidate status before stepping down **one level at a time**.
   - Moving from $\text{WATCH} \to \text{NONE}$ emits an "All Clear" notification.

4. **Cooldown Policy**:
   - Repeated notifications for the exact same severity are suppressed for **12 hours** unless severity escalates.

5. **Stale Data Guard**:
   - If $\text{now} - \text{dataAsOf} > 36\text{ hours}$, the state machine flags the state as `isStale = true` and **refuses to escalate**. It holds the current level until fresh data arrives or expires.

---

## 5. Verification Unit Tests

Every implementation of the rules engine domain must pass these unit test cases:
1. **Exceedance Calculation**: Given 50 members where exactly 25 exceed $85 \text{ m}^3/\text{s}$, verify $\text{share}_5 = 0.500$.
2. **Boundary Condition**: 14 of 50 members exceed $\implies 0.280$ (does not trigger at $0.30$); 15 of 50 members exceed $\implies 0.300$ (triggers).
3. **Escalation Resistance**: Single spike with candidate `WARNING` while current is `NONE` stays at `NONE` until second run confirms it.
4. **Hysteresis Stepdown**: Decreasing from `SEVERE` to `NONE` steps through `WARNING` $\to$ `WATCH` $\to$ `NONE` rather than jumping directly to `NONE`.

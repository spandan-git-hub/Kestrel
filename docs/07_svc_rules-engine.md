# 07 — rules-engine (MVP, extended P3) · Lambda `kestrel-rules-engine` · DynamoDB prefix `kestrel_rules_`

Tasks **P1-19 … P1-24**, **P3-11 … P3-14**. Core logic shared by all hazards. It turns *signals* into *decisions* using explicit, tested rules and a **severity state machine**. Pure domain code; thin Lambda shell.

## 1. Isolation requirement
Three **separate Lambda functions** (`kestrel-rules-engine-flood`, `kestrel-rules-engine-heat`, `kestrel-rules-engine-aq`), each with its own SQS trigger (`kestrel-rules-flood`, `kestrel-rules-heat`, `kestrel-rules-aq`). A slow/poison AQ message (Lambda timeout, DLQ) must never delay flood processing (guaranteed by separate Lambda execution environments). Separate Lambda concurrency limits (reservedConcurrency) per hazard for bulkhead.

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|---|
| `kestrel_rules_rule_config` | `hazard` | `locationId` (or `DEFAULT`) | params (map: thresholds, persistence, cooldown, etc.), updatedBy, updatedAt |
| `kestrel_rules_alert_state` | `locationId#hazard` | — | currentSeverity, candidateSeverity, candidateRuns, activeAlertId, lastTransitionAt, lastAlertAt, lastSignalAt, dataAsOf, metrics (map), provenance, version |
| `kestrel_rules_alert_history` | `alertId` | `createdAt` | locationId, hazard, fromSeverity, toSeverity, msgType, dataAsOf, metrics (map) |
| `kestrel_rules_outbox` | `eventId` | — | topicSuffix, routingKey, payload, createdAt, publishedAt (TTL), attempts |
| `kestrel_rules_inbox` | `eventId` | `consumerName` | processedAt, ttl |

## 3. Domain model (package `domain`, no Spring, no AWS SDK)
- `Severity` ordered enum; `HazardRule<S>` interface: `Assessment assess(S signal, RuleParams p)` → `{severity, metrics, notes}`.
- `SeverityStateMachine.step(State s, Assessment a, Instant now, Params p) → Transition{newState, emit:Optional<AlertDraft>}`.
- All time via passed-in `Instant now` (testable).

### 3.1 FloodRule (MVP)
Input `FloodSignal`. Rules:
1. If `snapStatus != VERIFIED` → severity `NONE`, note `UNVERIFIED_POINT`.
2. Evaluate days within `horizonDays` (default 7, ≤ signal horizon). For each level in {20-yr→SEVERE, 5-yr→WARNING, 2-yr→WATCH} check **any day** with `share_k ≥ shareThreshold` (default `0.30`, GloFAS signal convention). Candidate = highest level satisfied.
3. Metrics: `firstExceedanceDate` per level, `peakShare` per level, `leadTimeHours` (first exceedance − `dataAsOf`), `threshold` label.
Defaults configurable per location (DynamoDB `rule_config`): `shareThreshold, horizonDays, persistenceRuns{WATCH:1,WARNING:2,SEVERE:2}, deescalationRuns:2, cooldown:PT12H, maxDataAge:PT36H`.

### 3.2 State machine
State per (location,hazard) stored in DynamoDB `kestrel_rules_alert_state`: `current`, `candidate`, `candidateRuns`.
- **Escalation** to level L > current: require candidate ≥ L for `persistenceRuns[L]` consecutive signals (a "run" = one new signal whose `dataAsOf` is newer than the previous one; duplicates ignored via DynamoDB conditional update). Then transition, `msgType = current==NONE ? ALERT : UPDATE`.
- **De-escalation**: candidate < current for `deescalationRuns` consecutive runs → step down one level at a time (UPDATE); reaching `NONE` emits `CANCEL` ("all clear").
- **Cooldown:** no new notification for the same (location,hazard,severity) within `cooldown` unless severity increases.
- **Stale guard:** if `now − dataAsOf > maxDataAge`, **never escalate**; keep state; mark `metrics.stale=true`. Existing alerts expire at `validUntil` (= last supporting `dataAsOf` + `validityHours`, default 48 h) → emit `CANCEL` on expiry via EventBridge-triggered expiry Lambda.
- **Idempotency:** DynamoDB inbox by `eventId`; also ignore signals with `dataAsOf ≤ state.dataAsOf` (DynamoDB conditional write).
- Output `AlertEvent(stage=DECISION, provenance=MODEL_ONLY, autoSend = severity ≥ autoSend.minSeverity (default WARNING, per-location override))`, `alertId` stable per episode (new UUID when leaving NONE; reused for UPDATE/CANCEL until NONE).
Table-driven tests cover every transition (see §8).

### 3.3 HeatRule (Phase 3) — `⚠ VERIFY-06` first
(Same structure as in original; no changes to domain logic — only infrastructure layer changes.)
Inputs `HeatSignal.days[]` (tmax, normalTmax, departure) and `regionType`. IMD-style criteria:
- **Gate:** `tmax ≥ 40°C` (PLAINS), `≥ 37°C` (COASTAL), `≥ 30°C` (HILLY).
- **Heat-wave day:** gate met AND (`departure ≥ 4.5` OR `tmax ≥ 45`). **Severe heat-wave day:** gate met AND (`departure > 6.4` OR `tmax ≥ 47`).
- **Persistence:** WATCH = heat-wave day on ≥1 forecast day; WARNING = heat-wave days on ≥2 **consecutive** days; SEVERE = severe-heat-wave days on ≥2 consecutive days.
- Output is a **model-based advisory**, never "IMD declaration".

### 3.4 AqiCalculator + AqRule (Phase 3) — `⚠ VERIFY-05` first
`AqiCalculator` (pure): CPCB sub-index by linear interpolation; overall = max sub-index; **requires ≥3 pollutants and at least one of PM2.5/PM10**. Averaging windows: 24 h for PM10, PM2.5, NO₂, SO₂; 8 h for O₃, CO. **Mandatory acceptance tests: PM2.5 = 45 → 75; PM2.5 = 150 → 323.**
`AqRule`: category → severity (`Poor→WATCH, Very Poor→WARNING, Severe→SEVERE`); if `observed` differs from model category by ≥2 categories → `crossCheck=DISAGREE`, `autoSend=false`.

## 4. Messaging
- SQS triggers: `kestrel-rules-flood` SQS → `kestrel-rules-engine-flood` Lambda; `kestrel-rules-heat` SQS → `kestrel-rules-engine-heat` Lambda; `kestrel-rules-aq` SQS → `kestrel-rules-engine-aq` Lambda.
- Publishes `AlertEvent` via DynamoDB outbox → SNS `kestrel-alerts` with routing metadata `stage=DECISION, hazard=<h>`.
- Expiry: separate EventBridge Scheduler rule triggers `kestrel-rules-expiry` Lambda every 5 min; queries DynamoDB for expired alert states (`validUntil < now`); emits CANCEL via outbox.

## 5. REST API
`GET /api/v1/status?hazard=&locationId=` (list of current states + provenance + metrics + dataAsOf + dataAgeSeconds) · `GET /api/v1/status/{locationId}` · `GET /api/v1/status/history?locationId=&hazard=&from=` (DynamoDB query on `kestrel_rules_alert_history`) · ADMIN: `GET|PUT /api/v1/rules/config/{hazard}[/{locationId}]` (validated, audit-logged in DynamoDB), `POST /api/v1/rules/dry-run` (signal JSON → assessment+transition without persisting). In Phase 2, the **fused** status (provenance/evidence) comes from `signal-fusion`; the dashboard prefers `GET /api/v1/fusion/status` when available.

## 6. Config (SSM Parameter Store / environment)
`RULES_DEFAULTS_FLOOD_*` (mirroring §3 params per hazard), `RULES_AUTO_SEND_MIN_SEVERITY=WARNING`, `RULES_VALIDITY_HOURS=48`, `TABLE_PREFIX`, `SNS_TOPIC_ARN_ALERTS`.

## 7. Metrics (CloudWatch)
`kestrel.alert.decisions.total{hazard,severity,msgType}`, `kestrel.rules.eval.seconds{hazard}`, `kestrel.rules.stale_signals.total{hazard}`, `kestrel.rules.state{hazard,severity}` gauge.

## 8. Tests
- Table-driven **state-machine** tests (≥30 cases): first alert; persistence satisfied/not; flapping; escalation skip-levels; hysteresis; cooldown; stale data never escalates; expiry CANCEL; duplicate/out-of-order signals.
- FloodRule: threshold edges (`0.299 vs 0.30`), horizon edges, unverified point.
- Heat: boundaries 40/45/47 and departure 4.5/6.4; 2-consecutive-day rule (gap day resets).
- AQI: worked examples (45→75, 150→323), every breakpoint, <3 pollutants, CO µg→mg conversion.
- LocalStack IT: isolation test — set `reservedConcurrency=0` on `kestrel-rules-engine-aq` and confirm flood signal → decision latency unaffected (processed by separate Lambda).
- Contract test: emitted `AlertEvent` validates against JSON schema.

## 9. Task breakdown
P1-19 DynamoDB tables/entities · P1-20 domain: FloodRule + state machine · P1-21 SQS-triggered Lambda handlers (isolated per hazard) + idempotency · P1-22 DynamoDB outbox emission + expiry EventBridge Lambda · P1-23 status/rules-config/dry-run APIs · P1-24 tests green + README/Lambda handler · P3-11 HeatRule + tests · P3-12 AqiCalculator + tests · P3-13 AqRule + cross-check · P3-14 heat/aq SQS triggers wired + config + docs.

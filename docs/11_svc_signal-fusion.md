# 11 — signal-fusion (Phase 2) · Lambda `kestrel-signal-fusion` · DynamoDB prefix `kestrel_fusion_`

Tasks **P2-11 … P2-15**. The logic that earns trust: merges **model decision**, **community reports** and **official alerts** into one status per (location, hazard) with a provenance label. Evolves independently of rules.

## 1. Inputs / outputs
In: SQS `kestrel-fusion-decision` (`AlertEvent` stage DECISION), `kestrel-fusion-report` (`ReportAccepted/Updated`), `kestrel-fusion-official` (`OfficialAlertSignal`). Out: `AlertEvent` stage FUSED (SNS `kestrel-alerts` with stage=FUSED) and a read model in DynamoDB.
**Phase-2 cut-over:** set `FANOUT_INPUT_STAGE=FUSED` (env var only). Also `alert-composer` (P3) will consume FUSED events.

## 2. Data model (DynamoDB tables)

| Table | PK | SK | Key attributes |
|---|---|---|
| `kestrel_fusion_fused_status` | `locationId#hazard` | — | severity, provenance, modelSeverity, officialSeverity, alertId, validUntil (TTL after grace), dataAsOf, evidence (map: modelAlertId, reportIds list, officialAlertIds list), updatedAt, version |
| `kestrel_fusion_report_cache` | `reportId` | — | locationId, category, status, lat, lon, observedAt, ttl (expiresAt) |
| `kestrel_fusion_official_cache` | `officialAlertId` | — | capIdentifier, locationIds (list), hazard, mappedSeverity, expires (TTL), msgType |
| `kestrel_fusion_fusion_log` | `locationId#hazard` | `createdAt` | inputs (map), output (map) (for explainability) |
| `kestrel_fusion_outbox` | `eventId` | — | topicSuffix, payload, createdAt, publishedAt (TTL), attempts |
| `kestrel_fusion_inbox` | `eventId` | `consumerName` | processedAt, ttl |

## 3. Fusion rules (all thresholds configurable; log as D-09/D-16)
Per (location, hazard) on every relevant input:
1. **Model severity** `M` from latest DECISION (NONE if cancelled/expired).
2. **Report corroboration** `R`: count of **distinct reporterKeys** with `status ∈ {PENDING, VERIFIED}` (REJECTED excluded) in matching categories (FLOOD: `WATER_RISING, ROAD_BLOCKED`; HEAT: `EXTREME_HEAT`; AQ: `BAD_AIR`) within `fusion.reports.radiusKm` (default 10) of the location and `fusion.reports.window` (default 6 h). Corroborated if `R ≥ fusion.reports.minCount` (default 3) **or** ≥1 VERIFIED report.
3. **Official** `O`: active (not expired/cancelled) official alerts matching location+hazard; `officialSeverity = max(mappedSeverity)`.
4. **Output severity** `S = max(M, O)`. **Reports never raise `S`** (D-16, `fusion.reports.canEscalate=false`).
5. **Provenance**: `OFFICIAL_CONFIRMED` if `O ≥ WATCH` and |O − M| ≤ 1 level (or M=NONE and O≥WATCH → relayed official alert); else `MODEL_PLUS_REPORTS` if `M ≥ WATCH` and corroborated; else `MODEL_ONLY`. If official exists but is **lower** than the model by ≥2 levels → provenance stays model-based and `evidence.note="official feed shows lower level"` (visible to coordinator; supports trust).
6. **Emit** `AlertEvent(FUSED)` only when `(S, provenance)` changes or the underlying episode changes (`msgType` ALERT/UPDATE/CANCEL; `alertId` = model episode id, or a new id for official-only episodes). A provenance **upgrade** (e.g. MODEL_ONLY → OFFICIAL_CONFIRMED) at severity ≥ WARNING emits one UPDATE (subject to fanout cooldown).
7. `autoSend`: inherited from decision; official-relay episodes follow `fusion.official.autoSend` (default true for ≥WARNING, since SACHET already considered it authoritative) — and the message must be labelled "Official alert from … via SACHET".
8. Expire/cancel: EventBridge Scheduler rule every 5 min triggers `kestrel-fusion-expiry` Lambda; re-evaluates when DynamoDB cache items expire (TTL-based).
9. Each evaluation writes `kestrel_fusion_fusion_log` item for explainability and audit.

## 4. REST
`GET /api/v1/fusion/status?hazard=&locationId=` (COORDINATOR) → `{severity, provenance, evidence{modelAlert,reportCount,verifiedCount,officialAlerts[]}, dataAsOf, dataAgeSeconds, validUntil}` · `GET /api/v1/fusion/status/{locationId}/explain` (recent `fusion_log`).

## 5. Resilience
Missing report/official inputs degrade gracefully to MODEL_ONLY. If `fusion.decision` stops arriving (rules down) keep last status but mark `stale`. Idempotent consumers; out-of-order protection via event timestamps/`dataAsOf`.

## 6. Tests
Truth table for provenance (≥24 cases incl. official lower than model; M=NONE + official; reports corroborating but REJECTED; expiry); emission only on change; no escalation from reports; idempotency; explain log; contract test for FUSED `AlertEvent`; fanout rebinding E2E (flood scenario → FUSED → delivery).

## 7. Task breakdown
P2-11 DynamoDB tables + caches + SQS consumers · P2-12 fusion engine (pure domain) + truth-table tests · P2-13 emission via DynamoDB outbox/SNS + expiry EventBridge Lambda · P2-14 status/explain APIs · P2-15 fanout rebinding E2E + README/Lambda/SAM.

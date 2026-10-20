# 20 — Testing Strategy & Acceptance Criteria

Acceptance criteria below come from the project's own plan (Part 2 "satisfactory output"). Targets marked *(own target)* are **our** choices, not external standards — adjust after measurement and record in the Decision Log.

## 1. Test pyramid
| Level | Tools | Scope |
|---|---|
| Unit | JUnit 5, AssertJ | `domain` pure logic (AQI, heat, flood exceedance, Gumbel, state machine, fusion truth table, template rendering) — ≥80% coverage |
| Integration | **Testcontainers (LocalStack)** for DynamoDB/SQS/SNS, WireMock | repos, outbox/inbox, consumers, HTTP clients, DynamoDB rate limiters |
| Contract | JSON Schema validation | every event producer + consumer against `common-events` schemas; OpenAPI snapshot per service |
| Architecture | ArchUnit | layering, no cross-service deps, no `now()` in domain |
| E2E (backend) | LocalStack + Lambda local invoke + WireMock scenarios | signal → decision → delivery |
| Frontend | Vitest/RTL/MSW, Playwright, axe, Lighthouse CI | see docs 17–19 |
| Resilience / chaos | scripts in `tools/chaos/` (Lambda reserved-concurrency=0 to kill a service) | fault isolation |
| Load | `tools/loadtest/` | 50,000 simulated subscribers |

## 2. Mandatory acceptance tests

### A. Logic correctness (exact)
- **AQI:** PM2.5 = 45 µg/m³ → AQI 75; PM2.5 = 150 µg/m³ → AQI 323 (CPCB worked examples). Every breakpoint boundary; overall = max sub-index; <3 pollutants or no PM → insufficient data; CO µg/m³→mg/m³.
- **Heat:** boundary tests at 40, 45, 47 °C per region type; departure 4.5 / 6.4 boundaries; **2-consecutive-day rule** (a gap resets); WATCH/WARNING/SEVERE mapping.
- **Flood exceedance:** 25 of 51 members above the 20-yr threshold → share ≈ 0.490; 30% signal threshold edge (0.299 vs 0.300); both 50- and 51-member inputs; Gumbel thresholds vs hand-computed values.
- **State machine:** persistence, hysteresis, cooldown, stale-data no-escalation, expiry CANCEL, idempotent replays.

### B. Provenance & trust
- Every `AlertEvent`, message, push, Telegram text, CAP XML and UI status includes a provenance label; automated test scans all rendered templates (3 languages × hazards × severities) for the mandatory provenance sentence.
- No UI/message path can present MODEL_ONLY/MODEL_PLUS_REPORTS as "official".
- False-alarm control: persistence + cooldown verified by replaying a flapping synthetic signal (expect ≤ 1 alert per episode).

### C. Reliability (what justifies microservices)
1. **Fault isolation test** (`tools/chaos/kill-aq.sh`): set `kestrel-rules-engine-aq` Lambda `ReservedConcurrentExecutions=0` (or make AQ upstream return 500/hang) → inject `severe` flood scenario in WireMock mock upstream → **flood alert delivered** (MOCK + Telegram-mock) with decision latency within *(own target)* 5 s p95 of baseline. Repeat with `kestrel-heat-ingest`, `kestrel-cap-ingest`, `kestrel-report-intake` reserved concurrency = 0. Record results in checklist.
2. **Isolation inside rules-engine:** set `kestrel-rules-engine-aq` SQS trigger `Enabled: false`; flood decision latency unaffected.
3. **SQS/SNS outage simulation** during a broadcast: replay messages from DLQ; verify no lost alerts, no duplicate deliveries (assert counts by unique `deliveryId`).
4. **Registry down:** flood alert still fans out (DynamoDB replica), ingestion continues.
5. **DynamoDB throttling:** simulate via LocalStack reduced capacity; fanout continues with backoff; recovery verified.
6. **Open-Meteo flood down for > 36 h (simulated clock):** feed DOWN in DynamoDB, no escalations, existing alerts expire correctly via EventBridge expiry Lambda, dashboard shows stale banner + data age.

### D. Scale & delivery speed
- **50,000 simulated subscribers** (registry loadtest seeding, `MOCK` channel): one SEVERE broadcast → record time-to-first (TTFD) and time-to-last (TTLD) delivery, zero loss, zero duplicates. *(own target)*: TTLD < 60 s; document actual.
- **Free-channel reality check:** Telegram bucket test proves rate ≤ configured (25/s) → 50,000 ≈ 33 min at 25/s (≈28 min at 30/s); severe-first ordering verified with mixed priorities. Document honestly in README (free channels cannot reach under-a-minute).
- 200 req/s burst of public reports does not degrade flood path or fanout (separate Lambda, DynamoDB table).
- Lambda metrics: Lambda invocation count + duration in CloudWatch; confirm MVP stack stays within Lambda + DynamoDB free-tier limits (1M req/month + 400k GB-s/month).

### E. Forecast quality (measured, not promised) — Phase 3
Backtest on logged alerts + imported observed events: report POD, FAR (and CSI) by lead bucket with sample sizes; show "insufficient sample" below n=5; no claims beyond measured values.

### F. Security & privacy
Auth matrix tests (401/403/scope); JWT tampering; SQLi/XSS payloads in reports/notes; XXE payload on CAP parser rejected; rate limits; secrets absent from repo/logs; subscriber erase removes addresses everywhere (registry + fanout replica) within seconds *(own target)*; consent required; CORS allow-list.

### G. UX / accessibility
Severity never colour-only (automated snapshot checks); axe zero serious/critical on key pages; Lighthouse PWA ≥ 90, Performance ≥ 80 (mobile profile); initial JS ≤ 200 KB gzip; keyboard-only completion of subscribe + broadcast; hi/mr render without truncation.

### H. Real-world value (pilot, Phase 4)
With ≥1 real coordinator: measure time from threshold crossing → broadcast, delivery success, alert-before-worsening (compare alert timestamp with reported conditions), false-alarm complaints, "was this useful". Report as observed, not as general effectiveness (the Bihar evidence — +2.37 alerts/household, +46.5% alerts before water arrived — came from a different system and must not be assumed).

## 3. E2E scenario library (`tools/wiremock-upstream/scenarios`)
`calm` · `flood-watch` · `flood-warning` · `flood-severe` · `flood-flapping` · `flood-stale` · `heat-wave-2day` · `heat-severe` · `aq-poor` · `aq-very-poor-disagree` (model vs OpenAQ) · `sachet-flood-official` · `sachet-update-cancel` · `telegram-429` · `telegram-403`. Each scenario has expected outcomes documented in a markdown table next to fixtures and asserted by the E2E suite.

## 4. CI gates
PR: backend `verify`, frontend lint/type/test/build, bundle budget, OpenAPI/event-schema diff check. Nightly: chaos + load smoke (reduced N). Release: full acceptance run, results appended to `checklist.md`.

# 18 — Coordinator Dashboard (React) — Tasks P1-37…P1-41 (MVP), P2-20…P2-24, P3-27…P3-31

Route `/coordinator/*` (guarded: role COORDINATOR/ADMIN). Goal: a reviewer instantly sees **provenance, uncertainty and the broadcast flow**. Mobile-usable (coordinators in the field) and desktop-friendly.

## 1. Pages & layout
`/coordinator/login` · `/coordinator` (Overview: map + side panel) · `/coordinator/points/:locationId` · `/coordinator/reports` (P2) · `/coordinator/broadcast` · `/coordinator/history` · `/coordinator/feeds` · `/coordinator/evaluation` (P3, ADMIN) · `/coordinator/settings` (templates, rules — ADMIN, P3).
Shell: top bar (hazard toggle, language switcher, user menu), `StaleBanner`, bottom tab bar on mobile (Map · Reports · Broadcast · History · Feeds).

## 2. Overview — Hazard map (MVP: flood; P3: heat, AQ via hazard toggle)
- `MapView` with monitored points coloured **and shaped** by severity (from `fusion/status` if available else `status`); legend; hazard toggle writes `map.hazard`.
- Data: `useGetLocationsQuery`, `useGetStatusQuery({hazard})` (poll 60 s), flood summary (`/flood/points`), P2: report counts.
- Marker tap → side panel/bottom sheet: name, `SeverityBadge`, `ProvenanceBadge`, `DataAge`, key metric ("peak 42% of members above 5-yr level; first exceedance 23 Oct"), buttons: **Details**, **Broadcast**.
- Held-for-approval alerts: top "Needs your decision (n)" chip → list with **Approve & send** / **Dismiss** (uses `alerts` APIs; confirmation dialog).
- Unverified snap points shown hollow with "location not verified" note.

## 3. Point detail
- `ForecastBandChart` (min–max, p25–p75, median) with **2-, 5-, 20-year threshold lines**, per-day exceedance chips (share2/5/20 %), "time to first exceedance" for each level, thresholds source (`GUMBEL_FIT`/`GLOFAS_CSV`) and **data age** with last run time.
- Plain-language caveat block (always shown): "Model forecast at ~5 km resolution; the river shown may not be the one you expect. Guidance only — confirm locally. Not an official warning."
- P2: provenance evidence list (model run, N community reports with links, official alerts with authority/expiry) and "why this status" (fusion explain).
- P3: heat tab (tmax vs normal, departure) and AQ tab (CPCB sub-indices, category, model vs observed station).

## 4. Reports inbox (P2)
List + map overlay; filters (category, status, time); badges; `NEED_HELP` pinned on top; actions **Verify / Reject (reason)** (optimistic update with rollback); shows reporter-agnostic info only; each report shows distance to nearest monitored point and age; "Create broadcast from this" prefill. Poll 30 s; unread counter.

## 5. Broadcast composer (one-tap flow — core demo)
Steps (single page, progressive): (1) **Select** location(s) in scope, hazard, severity (prefilled when launched from an alert/point) → (2) **Preview**: per language × channel text (tabs), recipient count by channel/language, **estimated send time** (e.g. "Telegram: ~28 min for 50,000; severe-first"), provenance label locked, optional coordinator note (≤ 200 chars) → (3) **Send** (big primary button, confirm dialog summarizing recipients) with `Idempotency-Key`; disabled while submitting; shows live progress (poll `deliveries/summary`) with sent/failed/queued counts and time since send.
- **WhatsApp share** button: opens `https://wa.me/?text=<encoded text>` (language picker first); fallback copy-to-clipboard. `⚠ VERIFY-12` test link format on Android/iOS/desktop.
- Guard rails: must preview before send; cannot choose OFFICIAL_CONFIRMED manually; warns if status is MODEL_ONLY and severity SEVERE ("no local confirmation yet"); shows stale-data warning if data age > threshold; blocks when recipient count = 0.
- Draft state in `broadcastDraft` slice; survives accidental navigation.

## 6. History
Paginated list of broadcasts + automatic alerts: time, location, hazard, severity, provenance, recipients, delivered %, failures; row → detail (per-channel counts, TTFD/TTLD, trace link P3).

## 7. Feed & system status
Cards per feed (Open-Meteo flood/weather/AQ, OpenAQ, SACHET): `FeedState` icon+text, last success, **data age**, consecutive failures; queue depth per channel, Telegram bucket, replica lag, DLQ count (from `fanout/queue-status`, `platform/health` via CloudFront).

## 8. Evaluation (P3, ADMIN)
KPIs, backtest runs (POD/FAR by lead bucket with n and "insufficient sample" states), alert traces, feedback usefulness. Always shows the caveat: results measure *this* system on *logged/imported* events and may not generalize.

## 9. Settings (P3, ADMIN)
Rule parameters per hazard/location (validated forms, dry-run preview), snap-candidates tool for flood points (map with candidate pins; mark VERIFIED), template editor with per-language preview and "needs review" flags.

## 10. UX rules (apply everywhere)
Severity = colour + icon + text; plain language; i18n for all strings; **data age on every data card**; skeletons not spinners; error states with retry; optimistic UI only where safe (moderation); touch targets ≥ 44 px; keyboard operable; `aria-live="polite"` for new alerts/reports; no auto-playing sounds; avoid horizontal scroll.

## 11. Tests
Component tests for each page with MSW (states: loading, empty, stale, error, 403 scope); broadcast flow test (preview required, idempotency key reused on retry, double-click safe); Playwright e2e: login → see flood WARNING on map → open detail → preview → send → progress reaches 100% (mock channel). axe has zero serious violations.

## 12. Task breakdown
**MVP:** P1-37 login + shell + guards + refresh · P1-38 hazard map (flood) + side panel + held alerts · P1-39 point detail + chart · P1-40 broadcast composer + WhatsApp share · P1-41 history + feed/system status.
**P2:** P2-20 provenance badges/evidence in map & detail · P2-21 reports inbox + moderation · P2-22 official alerts panel · P2-23 broadcast-from-report/official · P2-24 platform health via CloudFront + `kestrel-platform-health` Lambda.
**P3:** P3-27 hazard toggle heat/AQ + detail tabs · P3-28 evaluation page · P3-29 settings (rules, snap tool) · P3-30 template editor · P3-31 email digest preferences & polish.

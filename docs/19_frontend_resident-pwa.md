# 19 — Resident PWA page — Tasks P1-42…P1-47 (MVP), P2-25

Route `/` (public). Audience: residents on low-end Android over slow networks, often in Hindi/Marathi. Everything must work in **≤ 3 taps** and read at a glance.

## 1. Screens
1. **Home / Status** (`/`, `/s/:locationCode`): current status for the chosen area in plain language.
2. **Subscribe wizard** (`/subscribe`): location → hazards → language → channels → consent.
3. **Report** (`/report`, P2): category, note, location.
4. **My alerts** (`/me`): manage subscription, connect Telegram, enable push, unsubscribe/erase.
5. **About / Sources / Privacy** (`/about`): how it works, data sources, limits, consent text, attribution.

## 2. Home / Status
- Location chooser: geolocation button ("Use my location" → `locations/nearest`) **or** searchable list; remembered in `resident` slice/localStorage.
- Big status card: `SeverityBadge` + one-sentence summary ("Flooding is likely near the river in the next 3 days.") + **What to do** (3 bullets max, from alert text keys) + `ProvenanceBadge` + `DataAge` + validity time.
- "No alert" state is explicit and reassuring but includes: "No alert does not mean no risk. Follow local authorities and official warnings."
- Multiple hazards: tabs/segmented (Flood · Heat · Air) — only those enabled for the location (P3).
- P2: "N community reports nearby in last 6 h" (public count).
- Footer: **"Guidance from a model/community — not an official warning"** unless provenance is OFFICIAL_CONFIRMED (then "Official alert from {authority} via SACHET").
- **Offline:** shows last known status (cached) with "Last updated {age}" and an offline banner; never presents cached data as current.
- Skeleton loading; page < 100 KB JS in the first route chunk where feasible (Leaflet **not** loaded here).

## 3. Subscribe wizard
Steps (each one screen, progress indicator, back/next, state in `resident.draft`):
1. Location(s) (1–5; nearest suggestion).
2. Hazards (checkboxes limited to enabled hazards; plain explanation per hazard).
3. Language (en/hi/mr, native names) — switches UI immediately.
4. Alert level: "Warnings only (recommended)" vs "Watches and warnings" → `minSeverity` WARNING/WATCH, with one-line explanation of alert fatigue.
5. Channels: **Telegram** (button → `telegram-link` → opens deep link; poll `subscriptions/me` until ACTIVE; show success), **Browser notifications** (explain, then permission), or "Just check this page". Show iOS guidance if needed.
6. **Consent** (required, unticked by default): plain-language text (versioned) stating what is stored (chosen location, hazards, language, channel address), why, retention, how to unsubscribe/erase, and that alerts are not official warnings. Submit disabled until ticked. Link to Privacy.
On success: store `managementToken` in localStorage (`resident.subscriberToken`), show confirmation + "How to stop alerts".
Errors: 429 friendly retry message; validation errors inline.

## 4. Report (P2)
Category (large icon buttons: Water rising · Road blocked · Need help · Extreme heat · Bad air · Other) → optional water level (ankle/knee/waist/above) → optional note ≤ 280 chars (counter) → location (auto geolocation with confirm on mini-map or pick nearest area) → submit with `Idempotency-Key`. Confirmation: "Thanks — your report helps your coordinator. Reports are not emergency calls." For `NEED_HELP` show emergency-number guidance **before** submit and after. Handle offline: queue the report locally (IndexedDB via small helper) and send when online with original `observedAt`; show queued state. Anti-abuse messaging on 429.

## 5. My alerts
View/edit subscription (locations, hazards, language, level), channel status chips (ACTIVE/PENDING/BLOCKED with fix hints: "Telegram stopped—tap to reconnect"), disable a channel, **Unsubscribe & delete my data** (confirm; calls `DELETE /subscriptions/me`; clears local token). Send test notification button (rate-limited; P2 optional).

## 6. Accessibility & language
Large type (≥16 px base), high contrast, icons + text, screen-reader labels, focus management between wizard steps, `lang` attribute follows selected language, no text embedded in images. All strings in `resident` and `alerts` namespaces. hi/mr flagged for native review (`⚠ VERIFY-18`).

## 7. Push & Telegram specifics
- Web push: only after explicit tap; handle `denied` state with instructions; re-subscribe on `pushsubscriptionchange`; service worker shows notification title `"{severity icon} {hazard} – {location}"`, body short text, click opens `/s/{code}`. `⚠ VERIFY-11`.
- Telegram: deep link `https://t.me/<bot>?start=<token>` (bot username from env); instruct user to press **Start**; confirm via polling (max 2 min, 3 s interval, backoff).

## 8. Tests
Component/flow tests (MSW): wizard happy path, consent gating, nearest-location, language switch, Telegram link polling, push permission denied path, offline last-known status with data age, unsubscribe & erase. e2e (Playwright, mobile viewport 360×640, throttled network): subscribe → receive status → change language. Lighthouse PWA/perf budgets; axe zero serious.

## 9. Task breakdown
P1-42 status home + location chooser · P1-43 subscribe wizard + consent + token handling · P1-44 Telegram connect + web push enablement + my-alerts management · P1-45 offline cache + data-age/stale handling + install prompt · P1-46 en/hi/mr strings complete + parity check · P1-47 a11y + performance pass (axe, Lighthouse, bundle budget) · P2-25 report flow (incl. offline queue) + nearby-reports count.

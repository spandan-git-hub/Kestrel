# 17 — Frontend Foundation (React + TypeScript + Redux Toolkit)

Tasks **P0-15 … P0-20**. One PWA, two audiences (resident / coordinator), mobile-first, light for slow connections. Hosted as static files on **AWS Amplify Hosting** (first 12 months) → **Cloudflare Pages** (permanent, after month 12 per D-23). A git-reconnect at month 12 is all that’s needed (no rewrite).

## 1. Stack (locked)
React 18+, TypeScript `strict`, Vite, **Redux Toolkit + RTK Query**, React Router (data router), Tailwind CSS, react-i18next, Leaflet + react-leaflet, vite-plugin-pwa (Workbox, `injectManifest`), Vitest + React Testing Library + MSW, Playwright, ESLint (typescript-eslint, react-hooks, jsx-a11y), Prettier, `openapi-typescript`. **No MUI/Ant/Chakra, no moment, no lodash-full, no charting library** (hand-rolled SVG).

## 2. Structure
```
frontend/
├─ index.html  vite.config.ts  tailwind.config.ts  tsconfig.json  .env.example  public/_redirects  public/icons/
├─ src/
│  ├─ main.tsx  App.tsx  router.tsx
│  ├─ app/            store.ts  hooks.ts (useAppDispatch/useAppSelector)  listenerMiddleware.ts
│  ├─ api/            baseApi.ts  registryApi.ts floodApi.ts rulesApi.ts fanoutApi.ts reportsApi.ts fusionApi.ts officialApi.ts heatApi.ts aqApi.ts composerApi.ts auditApi.ts  types/ (generated)
│  ├─ features/       auth/ ui/ map/ broadcast/ resident/ reports/ status/ feeds/ history/ evaluation/
│  ├─ components/     SeverityBadge ProvenanceBadge DataAge StaleBanner HazardIcon LanguageSwitcher ErrorBoundary Skeleton Toast ConfirmDialog AttributionFooter ForecastBandChart MapView
│  ├─ pages/          resident/*  coordinator/*
│  ├─ i18n/           index.ts  locales/{en,hi,mr}/*.json
│  ├─ pwa/            sw.ts  push.ts  installPrompt.ts
│  ├─ mocks/          handlers.ts  browser.ts  server.ts  fixtures/
│  ├─ styles/         tokens.css  index.css
│  └─ utils/          format.ts (dates in location tz) geo.ts a11y.ts env.ts
└─ e2e/               playwright specs
```

## 3. Redux design
- `store.ts`: `configureStore({ reducer: { [baseApi.reducerPath]: baseApi.reducer, auth, ui, map, broadcastDraft, resident }, middleware: gDM => gDM().concat(baseApi.middleware, listenerMiddleware.middleware) })`. Typed hooks.
- **Slices (client state only):**
  - `auth`: `{ accessToken|null, user:{id,email,role,locationIds}|null, status }`. Token **in memory**; refresh via `POST /auth/refresh` (httpOnly cookie). `listenerMiddleware` triggers silent refresh before expiry; logout clears cache via `baseApi.util.resetApiState()`.
  - `ui`: `{ language, theme, offline:boolean, toasts[], installPromptAvailable }` (language persisted in localStorage).
  - `map`: `{ hazard: 'FLOOD'|'HEAT'|'AIR_QUALITY', selectedLocationId|null, viewport, showReports:boolean }`.
  - `broadcastDraft`: `{ step, locationIds, hazard, severity, provenance, note, channels, previewId|null, idempotencyKey }` (new `crypto.randomUUID()` per draft; reset after send).
  - `resident`: `{ subscriberToken|null (persisted localStorage), draft:{locationIds,hazards,language,minSeverity,consent}, telegramLink|null }`.
- **RTK Query:** single `baseApi = createApi({ baseQuery: fetchBaseQuery({ baseUrl: env.API_BASE, prepareHeaders: attach Bearer or X-Subscriber-Token, credentials:'include' }) wrapped with reauth (401 → refresh once → retry), tagTypes: [...], endpoints: () => ({}) })`; each domain file uses `baseApi.injectEndpoints`. Use `providesTags/invalidatesTags` correctly; `pollingInterval` for live views (status 60 s, reports 30 s, queue-status 10 s, feed-status 60 s) with `skipPollingIfUnfocused: true`; `keepUnusedDataFor` tuned; `transformErrorResponse` → typed `ProblemDetail`.
- **Never** copy server data into slices; derive with memoized selectors (`createSelector`) / `selectFromResult`.
- Selectors joining multi-service data by `locationId`: `selectMapPoints` (locations ⨝ status/fusion ⨝ flood summary ⨝ reports counts).
- Errors: global listener maps `ProblemDetail` to toasts (localized); network failure sets `ui.offline`.

## 4. Environment
`VITE_API_BASE_URL`, `VITE_USE_MSW` (true/false), `VITE_VAPID_PUBLIC_KEY` (or fetched from `/api/v1/push/vapid-public-key`), `VITE_TELEGRAM_BOT` (display only), `VITE_APP_VERSION`. Dev: Vite proxy `/api → https://<CloudFront-dev-domain>` or LocalStack + Lambda local invoke.

## 5. Design system primitives (build first, P0-17)
| Component | Spec |
|---|---|
| `SeverityBadge` | props `{severity}`; **colour + icon + text** (NONE=green ✓ "No alert", WATCH=yellow ◔ "Watch", WARNING=orange ▲ "Warning", SEVERE=red ⬣ "Severe"); WCAG AA contrast on light/dark; `role="status"`; localized |
| `ProvenanceBadge` | `{provenance}` → "Model only" / "Model + reports" / "Official-confirmed (SACHET)"; tooltip explains; never shown as "official" unless OFFICIAL_CONFIRMED |
| `DataAge` | `{asOf, staleAfter}` → "Updated 3 h ago" (relative, localized); turns warning style + icon when stale; optional absolute time on hover/long-press |
| `StaleBanner` | page-level banner when any feed DEGRADED/DOWN |
| `ForecastBandChart` | SVG: x=days, min–max band, p25–p75 band, median line, horizontal threshold lines (2/5/20-yr, labelled), "first exceedance" marker, keyboard-focusable points with `aria` text summary, responsive, ≤ 150 lines |
| `MapView` | Leaflet wrapper; markers coloured by severity **plus** shape/icon; marker clustering off by default (≤ 200 points); lazy-loaded; attribution control on |
| `AttributionFooter` | exact strings in doc 21 |
Tokens in `tokens.css` (CSS variables), dark mode via `prefers-color-scheme`.

## 6. i18n
`en`, `hi`, `mr`; namespaces `common, resident, coordinator, alerts`; ICU-style plurals via i18next; dates/times via `Intl.DateTimeFormat` with location timezone; numbers via `Intl.NumberFormat`. Fallback `en`. Lint rule/test: **no string literals in JSX** (use `t()`); script `npm run i18n:check` ensures key parity across languages. hi/mr drafts carry `// NEEDS NATIVE REVIEW` metadata file `locales/review-status.json` (`⚠ VERIFY-18`).

## 7. PWA
- Manifest (name, short_name, icons 192/512 + maskable, `display: standalone`, theme colours, `start_url: /`).
- Service worker (Workbox `injectManifest`): precache app shell; runtime cache: GET `locations` (StaleWhileRevalidate), last status responses (NetworkFirst, 3 s timeout, 24 h) so the resident page shows **last known status with data age** offline; never cache auth/POST; push + `notificationclick` handlers (open `/s/{code}`); `skipWaiting` with update toast.
- Push: `pwa/push.ts` → request permission **only after user gesture + explanation screen**, `registration.pushManager.subscribe({userVisibleOnly:true, applicationServerKey})`, POST to registry. Handle iOS: show "Add to Home Screen first" guidance when `!standalone && isIOS` (`⚠ VERIFY-11`).
- Lighthouse CI budget in `frontend.yml`.

## 7b. Performance budget
Initial JS ≤ 200 KB gzip (route-level `React.lazy`; Leaflet and coordinator code only loaded on demand), images as SVG/WebP, fonts system stack, no layout shift on skeletons, list virtualization for > 100 rows.

## 8. MSW
`mocks/handlers.ts` implements every endpoint used (per docs 05–16) with realistic fixtures (incl. stale data, DOWN feeds, held alerts, 429s). `VITE_USE_MSW=true` runs the whole app without backend. Vitest uses the same handlers.

## 9. Testing
Unit/component (Vitest + RTL): badges (never colour-only), `DataAge` stale logic, chart a11y text, reducers, listener middleware (refresh, reset on logout), RTK Query hooks with MSW (tags/invalidation), i18n key parity. E2E (Playwright, MSW or compose backend): see docs 18/19. `axe` checks in CI for key pages.

## 10. Task breakdown
P0-15 Vite/TS/Tailwind/ESLint/Prettier scaffold + CI scripts · P0-16 store, `baseApi` + reauth, auth/ui slices, env, router skeleton · P0-17 design-system primitives + tests · P0-18 i18n scaffolding + parity script · P0-19 PWA shell (manifest, SW, install, offline banner) · P0-20 MSW + frontend CI workflow + bundle budget.

# 04 — Frontend UI (React + TypeScript + Redux Toolkit)

This document specifies the frontend architecture, Redux Toolkit store design, RTK Query microservices integration, UI design system, Leaflet hazard map, and core application views for the **Kestrel Prototype**.

---

## 1. Technology Stack & Directory Structure

- **Framework**: React 18+ with TypeScript in `strict` mode.
- **Build Tool**: Vite (with reverse proxy to Gateway on port 8080).
- **State Management**: **Redux Toolkit (RTK)** + **RTK Query** (`@reduxjs/toolkit`, `react-redux`).
- **Styling**: Tailwind CSS with sleek dark theme palette (`#0b0f19` background).
- **Mapping**: Leaflet + `react-leaflet` with OpenStreetMap tiles.
- **Icons**: Lucide React.
- **Localization**: `react-i18next` (`en`, `hi`, `mr`).

### Frontend Directory Layout
```
frontend/src/
├─ app/
│  ├─ store.ts               # Central Redux store configuration
│  └─ hooks.ts               # Typed useAppDispatch & useAppSelector
├─ api/                      # RTK Query API layer
│  ├─ baseApi.ts             # createApi with baseUrl '/api/v1'
│  ├─ registryApi.ts         # Injected endpoints for subscriber-registry
│  ├─ floodApi.ts            # Injected endpoints for flood-ingest
│  ├─ rulesApi.ts            # Injected endpoints for rules-engine
│  └─ fanoutApi.ts           # Injected endpoints for alert-fanout
├─ features/                 # Redux client slices
│  ├─ auth/authSlice.ts      # Active role (COORDINATOR vs RESIDENT)
│  ├─ ui/uiSlice.ts          # Language ('en'|'hi'|'mr'), toasts, simulationMode
│  ├─ map/mapSlice.ts        # selectedLocationId, activeHazard, viewport
│  ├─ broadcast/draftSlice.ts# Broadcast modal draft, preview, notes
│  └─ resident/residentSlice.ts# Ward selection, subscription draft
├─ components/               # Design primitives
│  ├─ SeverityBadge.tsx      # Color + Icon + Text badge
│  ├─ ProvenanceBadge.tsx    # MODEL_ONLY / MODEL_PLUS_REPORTS / OFFICIAL
│  ├─ DataAge.tsx            # Freshness indicator
│  ├─ StaleBanner.tsx        # Stale data warning
│  ├─ ForecastBandChart.tsx  # Hand-rolled SVG 10-day ensemble spread
│  └─ MapView.tsx            # Leaflet map with pulsing severity markers
├─ views/                    # Primary screens
│  ├─ CoordinatorDashboard.tsx
│  ├─ ResidentView.tsx
│  ├─ SimulationBar.tsx      # Sticky demo scenario control bar
│  ├─ BroadcastModal.tsx     # 1-Tap broadcast & WhatsApp trigger
│  └─ LiveDispatchDrawer.tsx # Real-time sent alerts feed
└─ i18n/                     # Translation dictionaries (en, hi, mr)
```

---

## 2. Redux Toolkit Architecture

Server state and client state are strictly separated:
- **Server Cache**: Managed entirely by **RTK Query** with automatic deduplication, caching, and polling.
- **Client State**: Managed via dedicated **Redux Slices**.

### 2.1 Central Store Configuration (`src/app/store.ts`)
```ts
import { configureStore } from '@reduxjs/toolkit';
import { baseApi } from '../api/baseApi';
import authReducer from '../features/auth/authSlice';
import uiReducer from '../features/ui/uiSlice';
import mapReducer from '../features/map/mapSlice';
import broadcastDraftReducer from '../features/broadcast/draftSlice';
import residentReducer from '../features/resident/residentSlice';

export const store = configureStore({
  reducer: {
    [baseApi.reducerPath]: baseApi.reducer,
    auth: authReducer,
    ui: uiReducer,
    map: mapReducer,
    broadcastDraft: broadcastDraftReducer,
    resident: residentReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(baseApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

### 2.2 Typed Redux Hooks (`src/app/hooks.ts`)
```ts
import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux';
import type { RootState, AppDispatch } from './store';

export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
```

---

## 3. RTK Query Microservices API Layer

### 3.1 Base API Definition (`src/api/baseApi.ts`)
```ts
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export const baseApi = createApi({
  reducerPath: 'kestrelApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/v1' }),
  tagTypes: ['Locations', 'AlertStatus', 'Forecast', 'RecentDispatches'],
  endpoints: () => ({}),
});
```

### 3.2 Injected Endpoints per Microservice

#### `rulesApi.ts` (Rules Engine)
```ts
import { baseApi } from './baseApi';

export const rulesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAlertStatuses: builder.query<AlertStatusDto[], void>({
      query: () => '/rules/status',
      providesTags: ['AlertStatus'],
      // Poll every 5 seconds for real-time hazard updates
      pollingInterval: 5000,
    }),
    getForecastByLocation: builder.query<ForecastDto, string>({
      query: (locationId) => `/rules/forecast/${locationId}`,
      providesTags: (result, error, id) => [{ type: 'Forecast', id }],
    }),
  }),
});

export const { useGetAlertStatusesQuery, useGetForecastByLocationQuery } = rulesApi;
```

#### `floodApi.ts` (Flood Ingest Simulation)
```ts
import { baseApi } from './baseApi';

export const floodApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    simulateScenario: builder.mutation<SimulationResponse, { scenario: string; locationId: string }>({
      query: (body) => ({
        url: '/flood/simulate',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['AlertStatus', 'Forecast'],
    }),
  }),
});

export const { useSimulateScenarioMutation } = floodApi;
```

#### `fanoutApi.ts` (Alert Fanout & WhatsApp)
```ts
import { baseApi } from './baseApi';

export const fanoutApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getRecentDispatches: builder.query<DispatchLogDto[], void>({
      query: () => '/fanout/recent',
      providesTags: ['RecentDispatches'],
      pollingInterval: 3000,
    }),
    previewBroadcast: builder.mutation<BroadcastPreviewDto, BroadcastRequestDto>({
      query: (body) => ({
        url: '/fanout/broadcast/preview',
        method: 'POST',
        body,
      }),
    }),
    sendBroadcast: builder.mutation<BroadcastResponseDto, BroadcastRequestDto>({
      query: (body) => ({
        url: '/fanout/broadcast',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['RecentDispatches', 'AlertStatus'],
    }),
    submitGroundReport: builder.mutation<ReportResponseDto, GroundReportDto>({
      query: (body) => ({
        url: '/fanout/reports',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['AlertStatus'],
    }),
  }),
});

export const {
  useGetRecentDispatchesQuery,
  usePreviewBroadcastMutation,
  useSendBroadcastMutation,
  useSubmitGroundReportMutation,
} = fanoutApi;
```

---

## 4. Design System Primitives

### 4.1 `SeverityBadge`
Never color alone; always **Color + Icon + Text**:
- `NONE`: Green badge with `ShieldCheck` icon ("Normal / Safe")
- `WATCH`: Amber badge with `AlertTriangle` icon ("Watch: 2-Yr Risk")
- `WARNING`: Orange badge with `AlertCircle` icon ("Warning: 5-Yr Flood")
- `SEVERE`: Red badge with `Flame` icon ("Severe: 20-Yr Inundation") with CSS animated pulse.

### 4.2 `ProvenanceBadge`
- `MODEL_ONLY`: Blue badge ("Model Guidance Only")
- `MODEL_PLUS_REPORTS`: Purple badge with community icon ("Model + Ground Reports")
- `OFFICIAL_CONFIRMED`: Green badge with checkmark ("Coordinator Confirmed")

### 4.3 `ForecastBandChart` (Lightweight SVG)
- Draws shaded 25th–75th percentile ensemble spread.
- Solid central polyline for ensemble median discharge ($m^3/s$).
- Dotted horizontal benchmark lines for 2-year (yellow), 5-year (orange), and 20-year (red) return period thresholds.

---

## 5. Primary Application Views

### 5.1 Interactive Leaflet Hazard Map (`MapView.tsx`)
- Centered on India coordinates: `[21.5, 82.0]` with zoom level `5`.
- Markers rendered for all 4 pilot locations from `registryApi`.
- Pulsing animated CSS ripple (`animate-ping`) on any marker whose active severity is `WARNING` or `SEVERE`.
- Clicking a marker dispatches `mapSlice.actions.setSelectedLocation(locationId)`.

### 5.2 Sticky Demo Simulation Bar (`SimulationBar.tsx`)
Sticky top pill bar allowing instant scenario injection via `useSimulateScenarioMutation`:
- `[🟢 Calm (Safe)]`
- `[🟡 Simulate Watch (2-Yr)]`
- `[🟠 Simulate Warning (5-Yr)]`
- `[🔴 Simulate Severe (20-Yr)]`
- `[🌐 Sync Live Open-Meteo]`

### 5.3 Coordinator Broadcast & WhatsApp Modal (`BroadcastModal.tsx`)
- Preview alert in English, Hindi, and Marathi tabs.
- Shows total active recipient count from `fanoutApi`.
- **"Share to WhatsApp" Button**: Opens `whatsAppShareUrl` (`https://wa.me/?text=...`) in a new tab with pre-formatted bilingual alert text and disclaimer.
- **"Send Broadcast" Button**: Dispatches alert to Telegram Bot API and Mock queue.

### 5.4 Resident Public View (`ResidentView.tsx`)
- Ward selector dropdown.
- Large status card in selected language (`en`, `hi`, `mr`).
- "Report Ground Situation" 2-click submission modal (triggers `useSubmitGroundReportMutation`, upgrading location provenance to `MODEL_PLUS_REPORTS`).
- "Subscribe for Alerts" modal.

### 5.5 Live Dispatch Feed (`LiveDispatchDrawer.tsx`)
- Collapsible bottom drawer showing sent alerts in real time from `useGetRecentDispatchesQuery`.

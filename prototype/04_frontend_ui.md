# 04 — Frontend UI & User Experience

This document specifies the React component hierarchy, visual design system, Leaflet map integration, Coordinator Dashboard, Resident Public View, and interactive components for the **Kestrel Prototype**.

---

## 1. Design System & Aesthetics

The prototype frontend delivers a **modern, high-impact aesthetic** designed to clearly communicate critical hazard risk at a glance.

### 1.1 Color Palette & Theme
- **Background**: Deep slate `bg-[#0b0f19]`
- **Card Surfaces**: Translucent slate `bg-[#131d31]/80` with glassmorphic backdrop blur `backdrop-blur-md` and border `border-slate-800`
- **Severity Color Tokens**:
  - `NONE`: Emerald (`emerald-500`, badge `bg-emerald-950/60 text-emerald-300 border-emerald-800`)
  - `WATCH`: Amber (`amber-400`, badge `bg-amber-950/60 text-amber-300 border-amber-800`)
  - `WARNING`: Orange (`orange-500`, badge `bg-orange-950/60 text-orange-300 border-orange-800`)
  - `SEVERE`: Rose/Red (`rose-600`, badge `bg-rose-950/60 text-rose-300 border-rose-800` with pulsing glow)
- **Typography**: Inter / system font with crisp weight hierarchy.

---

## 2. Layout & Shell Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│ TOP HEADER                                                             │
│  [Logo: Kestrel Prototype]       [View: Coordinator | Resident] [EN|HI|MR]
├────────────────────────────────────────────────────────────────────────┤
│ STICKY SIMULATION CONTROL BAR                                          │
│  [🟢 Calm]  [🟡 2-Yr Watch]  [🟠 5-Yr Warning]  [🔴 20-Yr Severe] [🌐 Live]│
├──────────────────────────────────┬─────────────────────────────────────┤
│ LEFT: INTERACTIVE LEAFLET MAP    │ RIGHT: LOCATION DETAILS & ACTIONS   │
│                                  │                                     │
│  - Map centered on India         │  - Selected Ward Header & River     │
│  - 4 Pilot Markers with pulses   │  - SeverityBadge + ProvenanceBadge  │
│  - Marker clicks select location │  - DataAge Freshness Indicator      │
│                                  │  - 10-Day SVG Forecast Band Chart   │
│                                  │  - [Broadcast Alert / WhatsApp] btn │
├──────────────────────────────────┴─────────────────────────────────────┤
│ BOTTOM: LIVE DISPATCH ACTIVITY FEED (Collapsible Drawer)               │
│  [Telegram] -> Chat 9812: ⚠️ केस्ट्रेल पूर इशारा... [Just now]           │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Interactive Leaflet Hazard Map (`HazardMap.tsx`)

- Centered on India coordinates: `[21.5, 82.0]` with zoom level `5`.
- Tiles: OpenStreetMap standard tiles (`https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`).
- Renders custom SVG `divIcon` markers for all 4 pilot locations:
  - Circle marker colored by active severity.
  - Active `WARNING` and `SEVERE` locations feature an animated CSS pulsing ring (`animate-ping`).
  - Clicking any marker selects that location and scrolls the detail card into view.

---

## 4. Key Reusable Components

### 4.1 `SeverityBadge`
Never color alone; always **Icon + Text + Color**:
```tsx
export function SeverityBadge({ severity }: { severity: Severity }) {
  const configs = {
    NONE: { icon: ShieldCheck, text: "Normal / Safe", cls: "bg-emerald-950 text-emerald-400 border-emerald-800" },
    WATCH: { icon: AlertTriangle, text: "Watch (2-Yr Risk)", cls: "bg-amber-950 text-amber-300 border-amber-800" },
    WARNING: { icon: AlertCircle, text: "Warning (5-Yr Flood)", cls: "bg-orange-950 text-orange-300 border-orange-800" },
    SEVERE: { icon: Flame, text: "Severe (20-Yr Inundation)", cls: "bg-rose-950 text-rose-300 border-rose-800 animate-pulse" }
  };
  const c = configs[severity];
  const Icon = c.icon;
  return (
    <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border ${c.cls}`}>
      <Icon className="w-3.5 h-3.5" />
      {c.text}
    </span>
  );
}
```

### 4.2 `ProvenanceBadge`
Clear indicator of whether the alert is pure model guidance, verified with reports, or confirmed:
- `MODEL_ONLY`: `"Model Guidance Only"` (Blue badge)
- `MODEL_PLUS_REPORTS`: `"Model + Ground Reports"` (Purple badge with community icon)
- `OFFICIAL_CONFIRMED`: `"Coordinator Confirmed"` (Emerald badge with checkmark)

### 4.3 `ForecastBandChart` (Lightweight SVG)
Displays the 10-day ensemble forecast spread without heavy third-party chart libraries:
- Shaded polygon representing the 25th–75th percentile ensemble band.
- Solid central polyline for the ensemble median.
- Dotted reference lines across the chart representing the 2-year (yellow), 5-year (orange), and 20-year (red) return period thresholds.
- Days on X-axis; discharge ($m^3/s$) on Y-axis.

---

## 5. Coordinator One-Tap Broadcast Modal (`BroadcastModal.tsx`)

When the coordinator clicks **"Broadcast Alert"**:
1. Modal opens with live recipient count (e.g. `42 Active Subscribers`).
2. Language tabs allow previewing the exact message that will be dispatched in English, Hindi, and Marathi.
3. Action Buttons:
   - **"Send to Subscribers"**: Calls `POST /api/v1/broadcasts`, dispatches to Telegram Bot & Mock Queue, logs event.
   - **"Share to WhatsApp"**: Generates bilingual text, launches `window.open(whatsappUrl, '_blank')`.

---

## 6. Resident Public View (`ResidentView.tsx`)

Switched via top-bar toggle:
1. **Ward Selector Dropdown**: Resident chooses their local ward (e.g. `Kurla Ward L, Mumbai`).
2. **Current Hazard Status Card**:
   - Large colored alert status in chosen language.
   - Plain-language protective instructions ("Avoid riverbanks", "Move to upper floors").
   - Data age badge ("Updated 12 mins ago").
3. **"Subscribe for Alerts" Button**: Opens modal to enter Telegram chat ID or test alias.
4. **"Report Ground Situation" Button**:
   - 2-click submission:
     1. Select category: `Water rising rapidly` or `Road blocked by water`.
     2. Click "Submit Ground Report".
   - Shows confirmation: *"Thank you. Your report has been forwarded to the local coordinator."*
   - Backend immediately updates location provenance to `MODEL_PLUS_REPORTS`.

---

## 7. Live Dispatch Activity Drawer (`LiveDispatchDrawer.tsx`)

- Sticky footer drawer that displays the last 50 sent messages.
- Polled every 3 seconds from `GET /api/v1/dispatch/recent`.
- Each card shows: Channel Badge (`TELEGRAM` or `MOCK`), Recipient, Timestamp, and Message text.
- Allows immediate verification during demos that alerts are successfully leaving the system.

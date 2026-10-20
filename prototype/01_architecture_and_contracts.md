# 01 — Prototype Architecture & Data Contracts

This document specifies the system architecture, in-process event model, database entities, and complete REST API contracts for the **Kestrel Prototype**.

---

## 1. System Architecture

The prototype consolidates Kestrel's 12 cloud microservices into a **clean, modular, two-tier architecture** that can be started locally with zero cloud or Docker setup:

```
┌────────────────────────────────────────────────────────────────────────┐
│ REACT VITE FRONTEND (Port 5173)                                        │
│  - Interactive Leaflet Hazard Map (Pulse markers for 4 pilot wards)     │
│  - Sticky Simulation Control Bar (Instant scenario switching)          │
│  - Coordinator View (Hazard Cards, SVG Forecast Chart, 1-Tap Broadcast)│
│  - Resident View (Ward Status in EN/HI/MR, 2-Click Ground Reports)     │
│  - Live Dispatch Feed Drawer & WhatsApp Forward Link Generator         │
└──────────────────────────────────▲─────────────────────────────────────┘
                                   │ HTTP REST (Vite Proxy: /api -> 8080)
┌──────────────────────────────────▼─────────────────────────────────────┐
│ SPRING BOOT 3.4 PROTOTYPE BACKEND (Port 8080)                           │
│                                                                        │
│  [REST Controllers] (/api/v1/** & /api/v1/simulate/**)                 │
│         │                                                              │
│         ├──► [Ingest & Simulator] ──► Open-Meteo API / Synthetic Mock  │
│         │           │                                                  │
│         │           ▼ (Spring Event: FloodSignalEvent)                 │
│         ├──► [Rules Engine] ──► Exceedance & Severity State Machine    │
│         │           │                                                  │
│         │           ▼ (Spring Event: AlertTransitionEvent)             │
│         ├──► [Alert Dispatcher] ──► Telegram Bot API / In-Memory Mock  │
│         │                                                              │
│         └──► [Registry & Reports] ──► Ground Reports & Subscriptions   │
│                     │                                                  │
│  [Persistence Layer]                                                   │
│   Embedded H2 Database (Auto-seeded with Indian Pilot Locations)       │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Differences from Full Production Architecture
1. **Compute**: Runs as a single Spring Boot service on JVM 21 rather than 12 individual AWS Lambda functions.
2. **Messaging**: In-process Spring `ApplicationEventPublisher` replaces external AWS SQS queues and SNS topics.
3. **Database**: In-memory / file-backed H2 with Spring Data JPA replaces DynamoDB with outbox/inbox transaction relays.
4. **Auth**: Coordinator mode is toggleable directly in the UI without needing AWS Cognito user pools.
5. **Live + Simulation Toggle**: Built-in scenario switcher (`CALM`, `WATCH`, `WARNING`, `SEVERE`, `LIVE`) enables instant demo execution on demand.

---

## 2. Canonical Enums

```java
public enum Hazard {
    FLOOD,
    HEAT,
    AIR_QUALITY
}

public enum Severity {
    NONE,
    WATCH,
    WARNING,
    SEVERE
}

public enum Provenance {
    MODEL_ONLY,           // Auto-computed from Open-Meteo ensemble forecast
    MODEL_PLUS_REPORTS,   // Corroborated by resident ground reports
    OFFICIAL_CONFIRMED    // Officially confirmed or manual coordinator broadcast
}

public enum Channel {
    TELEGRAM,
    MOCK,
    WEB_PUSH
}

public enum Language {
    en,   // English
    hi,   // Hindi (हिंदी)
    mr    // Marathi (मराठी)
}

public enum ReportCategory {
    WATER_RISING,
    ROAD_BLOCKED,
    NEED_HELP,
    OTHER
}

public enum SimulationScenario {
    LIVE_OPEN_METEO,
    CALM,
    WATCH_2YR,
    WARNING_5YR,
    SEVERE_20YR
}
```

---

## 3. Database Entities (JPA / H2)

### 3.1 `LocationEntity`
Pre-seeded monitored points across India.
- `id`: `String` (PK, e.g. `"mumbai-kurla"`)
- `name`: `String` (e.g. `"Kurla Ward L, Mumbai"`)
- `state`: `String` (e.g. `"Maharashtra"`)
- `latitude`: `Double` (e.g. `19.0726`)
- `longitude`: `Double` (e.g. `72.8797`)
- `riverName`: `String` (e.g. `"Mithi River"`)
- `threshold2yr`: `Double` (m³/s return level for 2-year flood)
- `threshold5yr`: `Double` (m³/s return level for 5-year flood)
- `threshold20yr`: `Double` (m³/s return level for 20-year flood)
- `primaryLanguage`: `Language` (e.g. `mr` or `hi`)

### 3.2 `SubscriptionEntity`
Resident notification subscriptions.
- `id`: `Long` (PK, auto-generated)
- `token`: `String` (UUID used for unsubscribe / management)
- `locationId`: `String`
- `channel`: `Channel` (`TELEGRAM` or `MOCK`)
- `recipientIdentifier`: `String` (e.g. Telegram chat ID or resident alias)
- `language`: `Language` (`en`, `hi`, `mr`)
- `minSeverity`: `Severity` (default `WARNING`)
- `active`: `Boolean`
- `createdAt`: `Instant`

### 3.3 `AlertStateEntity`
Current state machine status per location.
- `locationId`: `String` (PK)
- `hazard`: `Hazard`
- `currentSeverity`: `Severity` (`NONE`, `WATCH`, `WARNING`, `SEVERE`)
- `candidateSeverity`: `Severity`
- `candidateRuns`: `Integer`
- `activeAlertId`: `String` (UUID of current active alert episode)
- `provenance`: `Provenance`
- `lastSignalAt`: `Instant`
- `lastTransitionAt`: `Instant`
- `firstExceedanceDate`: `LocalDate`
- `peakShare`: `Double`
- `isStale`: `Boolean`

### 3.4 `CommunityReportEntity`
Ground truth reports from residents.
- `id`: `Long` (PK, auto-generated)
- `locationId`: `String`
- `category`: `ReportCategory`
- `description`: `String`
- `reporterToken`: `String`
- `createdAt`: `Instant`

### 3.5 `DeliveryLogEntity`
Log of alerts dispatched via Telegram or Mock channel.
- `id`: `Long` (PK, auto-generated)
- `alertId`: `String`
- `locationId`: `String`
- `channel`: `Channel`
- `recipient`: `String`
- `language`: `Language`
- `severity`: `Severity`
- `message`: `String` (full rendered text)
- `sentAt`: `Instant`

---

## 4. In-Process Decoupled Event Flow (Spring Events)

```java
// 1. Ingest publishes signal
public record FloodSignalEvent(
    String locationId,
    Instant dataAsOf,
    boolean isSimulated,
    List<ForecastDayStats> days
) {}

// 2. Rules engine evaluates signal and publishes transition if severity changes
public record AlertTransitionEvent(
    String alertId,
    String locationId,
    Hazard hazard,
    Severity fromSeverity,
    Severity toSeverity,
    Provenance provenance,
    Instant timestamp,
    String firstExceedanceDate
) {}

// 3. Resident submits ground report
public record CommunityReportSubmittedEvent(
    Long reportId,
    String locationId,
    ReportCategory category,
    Instant timestamp
) {}

// 4. Coordinator triggers broadcast
public record BroadcastRequestedEvent(
    String locationId,
    Hazard hazard,
    Severity severity,
    String customNote,
    String coordinatorId
) {}
```

---

## 5. Complete REST API Specifications

### 5.1 Public / Coordinator Shared Endpoints

#### `GET /api/v1/locations`
Returns all monitored pilot locations with basic metadata.
**Response `200 OK`**:
```json
[
  {
    "id": "mumbai-kurla",
    "name": "Kurla Ward L, Mumbai",
    "state": "Maharashtra",
    "latitude": 19.0726,
    "longitude": 72.8797,
    "riverName": "Mithi River",
    "primaryLanguage": "mr",
    "thresholds": {
      "twoYear": 45.0,
      "fiveYear": 85.0,
      "twentyYear": 140.0
    }
  }
]
```

#### `GET /api/v1/status`
Returns the current active hazard state, severity, provenance, and data freshness for all locations.
**Response `200 OK`**:
```json
[
  {
    "locationId": "mumbai-kurla",
    "locationName": "Kurla Ward L, Mumbai",
    "hazard": "FLOOD",
    "severity": "WARNING",
    "provenance": "MODEL_PLUS_REPORTS",
    "activeAlertId": "a89b7b90-87ef-42d8-971c-43f9a7c36a44",
    "firstExceedanceDate": "2026-10-23",
    "peakExceedanceShare": 0.62,
    "dataAsOf": "2026-10-21T06:30:00Z",
    "dataAgeSeconds": 1800,
    "isStale": false,
    "communityReportCount": 3,
    "lastTransitionAt": "2026-10-21T06:35:10Z"
  }
]
```

#### `GET /api/v1/forecast/{locationId}`
Returns 10-day ensemble forecast breakdown used by the SVG range chart.
**Response `200 OK`**:
```json
{
  "locationId": "mumbai-kurla",
  "dataAsOf": "2026-10-21T06:30:00Z",
  "thresholds": { "rp2": 45.0, "rp5": 85.0, "rp20": 140.0 },
  "days": [
    {
      "date": "2026-10-21",
      "qMin": 12.4,
      "qP25": 18.2,
      "qMedian": 24.5,
      "qP75": 31.0,
      "qMax": 42.1,
      "share2yr": 0.0,
      "share5yr": 0.0,
      "share20yr": 0.0
    },
    {
      "date": "2026-10-22",
      "qMin": 38.0,
      "qP25": 54.0,
      "qMedian": 76.5,
      "qP75": 92.0,
      "qMax": 118.0,
      "share2yr": 0.85,
      "share5yr": 0.42,
      "share20yr": 0.0
    }
  ]
}
```

---

### 5.2 Coordinator Broadcast & WhatsApp Endpoints

#### `POST /api/v1/broadcasts/preview`
Pre-generates multilingual alert previews, recipient count, and pre-formatted WhatsApp share link before sending.
**Request**:
```json
{
  "locationId": "mumbai-kurla",
  "hazard": "FLOOD",
  "severity": "WARNING",
  "customNote": "Water level rising near Kurla railway bridge."
}
```
**Response `200 OK`**:
```json
{
  "locationId": "mumbai-kurla",
  "locationName": "Kurla Ward L, Mumbai",
  "recipientCount": 42,
  "previews": {
    "en": "⚠️ KESTREL FLOOD WARNING: Kurla Ward L, Mumbai\nExpected high river discharge over the next 48 hours. Water level rising near Kurla railway bridge.\nSafety: Avoid low-lying riverbanks. Move valuables to higher ground.\n[Model Guidance + Ground Reports — Confirm Locally]",
    "hi": "⚠️ केस्ट्रेल बाढ़ चेतावनी: कुर्ला वार्ड एल, मुंबई\nअगले 48 घंटों में उच्च जलस्तर की संभावना। कुर्ला रेलवे ब्रिज के पास पानी बढ़ रहा है।\nसुरक्षा सलाह: निचले इलाकों से दूर रहें। सुरक्षित स्थानों पर जाएं।\n[मॉडल पूर्वानुमान + स्थानीय रिपोर्ट — स्थानीय पुष्टि करें]",
    "mr": "⚠️ केस्ट्रेल पूर इशारा: कुर्ला वार्ड एल, मुंबई\nपुढील ४८ तासांत मिठी नदीची पातळी वाढण्याची शक्यता. कुर्ला रेल्वे पुलाजवळ पाणी वाढत आहे.\nसुरक्षा: सखल भागातील नागरिकांनी सुरक्षित स्थळी स्थलांतर करावे.\n[मॉडेल अंदाज + स्थानिक अहवाल — स्थानिक पातळीवर खात्री करा]"
  },
  "whatsAppShareUrl": "https://wa.me/?text=%E2%9A%A0%EF%B8%8F%20KESTREL%20FLOOD%20WARNING%3A%20Kurla%20Ward%20L...%0A%0A%E2%9A%A0%EF%B8%8F%20%E0%A4%95%E0%A5%87%E0%A4%B8%E0%A5%8D%E0%A4%A4%E0%A5%8D%E0%A4%B0%E0%A5%87%E0%A4%B2%20%E0%A4%AA%E0%A5%82%E0%A4%B0%20%E0%A4%87%E0%A4%B6%E0%A4%BE%E0%A4%B0%E0%A4%BE..."
}
```

#### `POST /api/v1/broadcasts`
Executes an immediate broadcast to all active subscribers via Telegram / Mock channel and logs entries.
**Request**: Same as preview.
**Response `201 Created`**:
```json
{
  "broadcastId": "b1823901-2821-4f11-9310-891230198210",
  "dispatchedCount": 42,
  "channels": ["TELEGRAM", "MOCK"],
  "provenance": "OFFICIAL_CONFIRMED",
  "sentAt": "2026-10-21T06:40:00Z"
}
```

---

### 5.3 Resident Public Endpoints

#### `POST /api/v1/public/subscriptions`
Subscribe for alerts.
**Request**:
```json
{
  "locationId": "mumbai-kurla",
  "channel": "TELEGRAM",
  "recipientIdentifier": "123456789",
  "language": "mr",
  "minSeverity": "WARNING"
}
```
**Response `201 Created`**:
```json
{
  "token": "4fa81720-72a3-41bb-b68f-9a109a28c39e",
  "status": "ACTIVE",
  "message": "Subscribed successfully"
}
```

#### `POST /api/v1/public/reports`
Submits a 2-click ground report from a resident.
**Request**:
```json
{
  "locationId": "mumbai-kurla",
  "category": "WATER_RISING",
  "description": "Water overflowing storm drain onto LBS Marg."
}
```
**Response `201 Created`**:
```json
{
  "reportId": 14,
  "locationId": "mumbai-kurla",
  "newProvenance": "MODEL_PLUS_REPORTS",
  "message": "Report received and added to coordinator dashboard."
}
```

---

### 5.4 Simulation & Testing Endpoints

#### `POST /api/v1/simulate/scenario`
Forces the backend into a predefined state for interactive demos.
**Request**:
```json
{
  "scenario": "WARNING_5YR",
  "locationId": "mumbai-kurla"
}
```
**Response `200 OK`**:
```json
{
  "scenario": "WARNING_5YR",
  "locationId": "mumbai-kurla",
  "resultingSeverity": "WARNING",
  "provenance": "MODEL_ONLY",
  "message": "Injected synthetic 5-year flood spike: 65% members exceeding 5-yr threshold."
}
```

#### `POST /api/v1/simulate/reset`
Resets all states, clears reports and delivery logs, and reverts to live Open-Meteo or calm baseline.
**Response `200 OK`**:
```json
{
  "status": "RESET_SUCCESSFUL",
  "activeSeverity": "NONE"
}
```

#### `GET /api/v1/dispatch/recent`
Returns the last 50 sent alerts for inspection in the frontend Live Dispatch Feed.
**Response `200 OK`**:
```json
[
  {
    "id": 102,
    "alertId": "a89b7b90-87ef-42d8-971c-43f9a7c36a44",
    "locationName": "Kurla Ward L, Mumbai",
    "channel": "TELEGRAM",
    "recipient": "Chat: 98127391",
    "language": "mr",
    "severity": "WARNING",
    "message": "⚠️ केस्ट्रेल पूर इशारा: मिठी नदीची पातळी वाढण्याची शक्यता...",
    "sentAt": "2026-10-21T06:40:02Z"
  }
]
```

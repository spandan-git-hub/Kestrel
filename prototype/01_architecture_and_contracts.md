# 01 — Microservices Architecture & Data Contracts

This document specifies the microservices topology, Spring Cloud Gateway routing, database isolation, inter-service event contracts, and complete REST APIs for the **Kestrel Prototype**.

---

## 1. Microservices System Topology

The prototype implements a **true microservices architecture** using Spring Boot 3.4+ and Spring Cloud 2024.0.x:

```
                            REACT 18 + TYPESCRIPT + REDUX TOOLKIT (Port 5173)
                              [Coordinator Dashboard | Resident PWA | Sim Bar]
                                                   │
                                                   │ HTTP / REST
                                                   ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ SPRING CLOUD API GATEWAY (Port 8080)                                                        │
│  - Unified entry point for frontend: http://localhost:8080/api/v1/**                        │
│  - CORS support for http://localhost:5173                                                   │
│  - Routes:                                                                                  │
│    • /api/v1/registry/** ──► subscriber-registry (Port 8081)                                │
│    • /api/v1/flood/**    ──► flood-ingest        (Port 8082)                                │
│    • /api/v1/rules/**    ──► rules-engine        (Port 8083)                                │
│    • /api/v1/fanout/**   ──► alert-fanout        (Port 8084)                                │
└──────────────┬───────────────────┬───────────────────┬───────────────────┬──────────────────┘
               │                   │                   │                   │
               ▼                   ▼                   ▼                   ▼
┌───────────────────────┐ ┌──────────────────┐ ┌─────────────────┐ ┌──────────────────────┐
│ subscriber-registry   │ │ flood-ingest     │ │ rules-engine    │ │ alert-fanout         │
│ (Port 8081)           │ │ (Port 8082)      │ │ (Port 8083)     │ │ (Port 8084)          │
│                       │ │                  │ │                 │ │                      │
│ - Locations catalog   │ │ - Open-Meteo     │ │ - Exceedance    │ │ - Multilingual       │
│ - Resident subscribers│ │   ensemble pull  │ │   rules ($T_k$) │ │   templates (EN/HI/MR│
│ - Consent records     │ │ - 5-mode scenario│ │ - Severity state│ │ - Telegram Bot API   │
│ - Emits:              │ │   simulator      │ │   machine       │ │ - In-memory mock feed│
│   LocationUpserted    │ │ - Emits:         │ │ - Emits:        │ │ - WhatsApp URL link  │
│                       │ │   FloodSignal    │ │   AlertEvent    │ │ - Ground reports     │
└──────────────┬────────┘ └────────┬─────────┘ └────────┬────────┘ └──────────┬───────────┘
               │                   │                    │                     │
       ┌───────▼───────┐   ┌───────▼───────┐    ┌───────▼───────┐     ┌───────▼───────┐
       │ H2 Database:  │   │ H2 Database:  │    │ H2 Database:  │     │ H2 Database:  │
       │ kestrel_      │   │ kestrel_      │    │ kestrel_      │     │ kestrel_      │
       │ registry_db   │   │ flood_db      │    │ rules_db      │     │ fanout_db     │
       └───────────────┘   └───────────────┘    └───────────────┘     └───────────────┘
               │                   │                    │                     │
               │                   └───────────┐        │                     │
               ▼ LocationUpserted              ▼        ▼ AlertEvent          │
         ═════════════════════════════════════════════════════════════════════╪══════════════
                            INTER-SERVICE ASYNCHRONOUS EVENT BUS
```

---

## 2. Port Allocation & Gateway Routing

| Service Name | Port | Base Path | Database URL |
|---|---|---|---|
| **`api-gateway`** | `8080` | `/` | *None (stateless routing)* |
| **`subscriber-registry`** | `8081` | `/api/v1/registry` | `jdbc:h2:mem:kestrel_registry_db` |
| **`flood-ingest`** | `8082` | `/api/v1/flood` | `jdbc:h2:mem:kestrel_flood_db` |
| **`rules-engine`** | `8083` | `/api/v1/rules` | `jdbc:h2:mem:kestrel_rules_db` |
| **`alert-fanout`** | `8084` | `/api/v1/fanout` | `jdbc:h2:mem:kestrel_fanout_db` |
| **`frontend`** | `5173` | `/` | *Browser LocalStorage (client only)* |

### Spring Cloud Gateway `application.yml` Route Definitions
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "http://localhost:5173"
            allowedMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
            allowedHeaders: "*"
      routes:
        - id: subscriber-registry-route
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/registry/**
        - id: flood-ingest-route
          uri: http://localhost:8082
          predicates:
            - Path=/api/v1/flood/**
        - id: rules-engine-route
          uri: http://localhost:8083
          predicates:
            - Path=/api/v1/rules/**
        - id: alert-fanout-route
          uri: http://localhost:8084
          predicates:
            - Path=/api/v1/fanout/**
```

---

## 3. Database Isolation & Event-Carried State Transfer

To ensure strict microservices compliance:
1. **Zero Cross-Service Database Queries**: No service ever accesses another service's database schema or JDBC URL.
2. **Event-Carried State Transfer**: When `subscriber-registry` upserts a monitored location, it publishes `LocationUpsertedEvent`.
3. Downstream services (`flood-ingest` and `alert-fanout`) maintain their own **local read-replica tables** (`MonitoredPointReplica`) populated by these events or seeded via bootstrap CSV (`infra/seed/locations.csv`).

---

## 4. Shared Event Contracts (`libs/common-events`)

All events share a standardized envelope matching the production specification:

```java
public record EventEnvelope<T>(
    String eventId,
    String eventType,
    int schemaVersion,
    Instant occurredAt,
    String producer,
    String correlationId,
    T payload
) {}
```

### 4.1 Canonical Enums
```java
public enum Hazard { FLOOD, HEAT, AIR_QUALITY }
public enum Severity { NONE, WATCH, WARNING, SEVERE }
public enum Provenance { MODEL_ONLY, MODEL_PLUS_REPORTS, OFFICIAL_CONFIRMED }
public enum Channel { TELEGRAM, MOCK, WEB_PUSH }
public enum Language { en, hi, mr }
public enum ReportCategory { WATER_RISING, ROAD_BLOCKED, NEED_HELP, OTHER }
public enum SimulationScenario { LIVE_OPEN_METEO, CALM, WATCH_2YR, WARNING_5YR, SEVERE_20YR }
```

### 4.2 Event Payloads

#### `LocationUpsertedPayload` (Published by `subscriber-registry`)
```java
public record LocationUpsertedPayload(
    String locationId,
    String name,
    String state,
    double latitude,
    double longitude,
    String riverName,
    double threshold2yr,
    double threshold5yr,
    double threshold20yr,
    Language primaryLanguage,
    boolean active
) {}
```

#### `FloodSignalPayload` (Published by `flood-ingest`)
```java
public record FloodSignalPayload(
    String locationId,
    Instant dataAsOf,
    boolean isSimulated,
    int memberCount,
    List<ForecastDayStats> days
) {
    public record ForecastDayStats(
        LocalDate date,
        double qMin,
        double qP25,
        double qMedian,
        double qP75,
        double qMax,
        double share2yr,
        double share5yr,
        double share20yr
    ) {}
}
```

#### `AlertEventPayload` (Published by `rules-engine`)
```java
public record AlertEventPayload(
    String alertId,
    String locationId,
    Hazard hazard,
    String stage,             // "DECISION"
    Severity fromSeverity,
    Severity toSeverity,
    Provenance provenance,
    Instant timestamp,
    LocalDate firstExceedanceDate,
    double peakShare,
    boolean autoSend
) {}
```

---

## 5. Complete REST API Specifications

All endpoints are reachable through the API Gateway at `http://localhost:8080`.

### 5.1 `subscriber-registry` Endpoints (`/api/v1/registry/**`)

#### `GET /api/v1/registry/locations`
Returns all monitored locations.
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
    "thresholds": { "twoYear": 45.0, "fiveYear": 85.0, "twentyYear": 140.0 }
  }
]
```

#### `POST /api/v1/registry/subscriptions`
Subscribes a resident.
- **Request**:
  ```json
  {
    "locationId": "mumbai-kurla",
    "channel": "TELEGRAM",
    "recipientIdentifier": "98127391",
    "language": "mr",
    "minSeverity": "WARNING"
  }
  ```
- **Response `201 Created`**:
  ```json
  {
    "token": "4fa81720-72a3-41bb-b68f-9a109a28c39e",
    "status": "ACTIVE",
    "message": "Subscription registered."
  }
  ```

---

### 5.2 `flood-ingest` Endpoints (`/api/v1/flood/**`)

#### `POST /api/v1/flood/simulate`
Injects a deterministic scenario for instant demonstration.
- **Request**:
  ```json
  {
    "scenario": "WARNING_5YR",
    "locationId": "mumbai-kurla"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "scenario": "WARNING_5YR",
    "locationId": "mumbai-kurla",
    "message": "Injected synthetic 5-year flood spike: 68% ensemble members exceeding 5-yr threshold."
  }
  ```

#### `POST /api/v1/flood/sync-live`
Pulls live Open-Meteo flood data for all locations immediately.

---

### 5.3 `rules-engine` Endpoints (`/api/v1/rules/**`)

#### `GET /api/v1/rules/status`
Returns active hazard status for all locations (polled by Redux RTK Query).
```json
[
  {
    "locationId": "mumbai-kurla",
    "locationName": "Kurla Ward L, Mumbai",
    "hazard": "FLOOD",
    "severity": "WARNING",
    "provenance": "MODEL_ONLY",
    "activeAlertId": "a89b7b90-87ef-42d8-971c-43f9a7c36a44",
    "firstExceedanceDate": "2026-10-24",
    "peakExceedanceShare": 0.68,
    "dataAsOf": "2026-10-22T17:00:00Z",
    "dataAgeSeconds": 300,
    "isStale": false,
    "lastTransitionAt": "2026-10-22T17:01:10Z"
  }
]
```

#### `GET /api/v1/rules/forecast/{locationId}`
Returns 10-day forecast quantiles for the SVG band chart.
```json
{
  "locationId": "mumbai-kurla",
  "thresholds": { "rp2": 45.0, "rp5": 85.0, "rp20": 140.0 },
  "days": [
    {
      "date": "2026-10-22",
      "qMin": 15.0,
      "qP25": 22.0,
      "qMedian": 35.0,
      "qP75": 48.0,
      "qMax": 65.0,
      "share2yr": 0.35,
      "share5yr": 0.05,
      "share20yr": 0.0
    }
  ]
}
```

---

### 5.4 `alert-fanout` Endpoints (`/api/v1/fanout/**`)

#### `POST /api/v1/fanout/broadcast/preview`
Generates multilingual previews and the client-side WhatsApp link.
- **Request**:
  ```json
  {
    "locationId": "mumbai-kurla",
    "hazard": "FLOOD",
    "severity": "WARNING",
    "customNote": "Water rising near Kurla railway bridge."
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "locationId": "mumbai-kurla",
    "recipientCount": 42,
    "previews": {
      "en": "⚠️ KESTREL FLOOD WARNING: Kurla Ward L, Mumbai\nExpected high river discharge over next 48 hours...\n[Model Guidance — Confirm Locally]",
      "hi": "⚠️ केस्ट्रेल बाढ़ चेतावनी: कुर्ला वार्ड एल, मुंबई\nअगले 48 घंटों में उच्च जलस्तर की संभावना...\n[मॉडल पूर्वानुमान — स्थानीय पुष्टि करें]",
      "mr": "⚠️ केस्ट्रेल पूर इशारा: कुर्ला वार्ड एल, मुंबई\nपुढील ४८ तासांत मिठी नदीची पातळी वाढण्याची शक्यता...\n[मॉडेल अंदाज — स्थानिक पातळीवर खात्री करा]"
    },
    "whatsAppShareUrl": "https://wa.me/?text=%E2%9A%A0%EF%B8%8F%20KESTREL%20FLOOD%20WARNING..."
  }
  ```

#### `POST /api/v1/fanout/broadcast`
Dispatches alert to Telegram Bot API and Mock queue.

#### `GET /api/v1/fanout/recent`
Returns last 50 sent messages for the Live Dispatch Feed in the frontend.

#### `POST /api/v1/fanout/reports`
Submits a resident ground report; upgrades location provenance to `MODEL_PLUS_REPORTS`.
- **Request**:
  ```json
  {
    "locationId": "mumbai-kurla",
    "category": "WATER_RISING",
    "description": "Drain overflowing onto CST Road."
  }
  ```
- **Response `201 Created`**:
  ```json
  {
    "reportId": 105,
    "locationId": "mumbai-kurla",
    "newProvenance": "MODEL_PLUS_REPORTS",
    "message": "Ground report recorded. Provenance upgraded."
  }
  ```

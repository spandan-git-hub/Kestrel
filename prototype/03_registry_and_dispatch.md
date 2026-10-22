# 03 — Registry & Fanout Microservices (`subscriber-registry` & `alert-fanout`)

This document details the architecture, data models, multilingual templates, WhatsApp share generator, and delivery pipelines for the **`subscriber-registry`** (Port 8081) and **`alert-fanout`** (Port 8084) Spring Boot microservices.

---

## 1. `subscriber-registry` Microservice (Port 8081)

The `subscriber-registry` microservice owns the source of truth for all monitored locations, resident subscriptions, and coordinator credentials.

### 1.1 Service Structure
- **Package**: `com.kestrel.subscriberregistry`
- **Port**: `8081`
- **Database**: H2 isolated instance (`jdbc:h2:mem:kestrel_registry_db`)
- **Key Entities**:
  - `LocationEntity`: Monitored coordinates, river hints, return-period thresholds ($T_2, T_5, T_{20}$), and primary regional language.
  - `SubscriptionEntity`: Resident channel handles (Telegram chat ID, mock alias), preferred language, minSeverity filter, and UUID token.

### 1.2 Seeded Indian Pilot Locations
On startup, if the database is empty, `subscriber-registry` seeds 4 diverse Indian flood risk points:

```java
public static final List<LocationEntity> SEED_LOCATIONS = List.of(
    new LocationEntity("mumbai-kurla", "Kurla Ward L, Mumbai", "Maharashtra",
        19.0726, 72.8797, "Mithi River", 45.0, 85.0, 140.0, Language.mr, true),
    new LocationEntity("patna-ward12", "Ward 12, Patna", "Bihar",
        25.6127, 85.1589, "Ganges River", 38000.0, 48000.0, 62000.0, Language.hi, true),
    new LocationEntity("guwahati-river", "Riverside, Guwahati", "Assam",
        26.1856, 91.7478, "Brahmaputra River", 32000.0, 42000.0, 55000.0, Language.en, true),
    new LocationEntity("cuttack-delta", "Cuttack Municipal Corp", "Odisha",
        20.4625, 85.8830, "Mahanadi River", 18000.0, 26000.0, 35000.0, Language.en, true)
);
```
After seeding, it publishes `LocationUpsertedEvent` for each location so other microservices can build their local replicas.

### 1.3 Subscription Management API
- `POST /api/v1/registry/subscriptions`: Creates a subscription with an opaque UUID token (e.g. `4fa81720-72a3-41bb-b68f-9a109a28c39e`).
- `GET /api/v1/registry/subscriptions/{token}`: Fetches subscription preferences.
- `DELETE /api/v1/registry/subscriptions/{token}`: One-call GDPR/DPDP compliant unsubscribe and data erasure.

---

## 2. `alert-fanout` Microservice (Port 8084)

The `alert-fanout` microservice consumes `AlertEvent` decisions, determines eligible recipients, renders multilingual alert messages, and dispatches via Telegram, Mock queue, and WhatsApp.

### 2.1 Service Structure
- **Package**: `com.kestrel.alertfanout`
- **Port**: `8084`
- **Database**: H2 isolated instance (`jdbc:h2:mem:kestrel_fanout_db`)
- **Key Entities**:
  - `LocationReplica`: Local replica populated via `LocationUpsertedEvent`.
  - `DeliveryLog`: Audit record of every alert sent (channel, recipient, language, timestamp, excerpt).
  - `CommunityReport`: Resident ground truth reports.

### 2.2 Multilingual Alert Template Engine (`AlertTemplateComposer`)
Alerts are dynamically assembled from 4 standard components:
1. **Header**: Severity badge + Hazard + Location Name
2. **Impact & Horizon**: Forecast peak timing and estimated rise
3. **Actionable Advice**: Clear, non-technical safety instructions
4. **Mandatory Provenance Note**: Explicit disclaimer regarding guidance vs official warning

#### WARNING Level (5-Year Flood) Templates
- **English (`en`)**:
  ```text
  ⚠️ KESTREL FLOOD WARNING: {locationName}
  River discharge is forecast to exceed dangerous levels within the next 48 hours (Peak expected on {peakDate}).
  Action: Move livestock and valuables to upper floors. Avoid walking or driving near river embankments.
  Provenance: {provenanceLabel} — Not an official warning. Confirm locally.
  ```
- **Hindi (`hi`)**:
  ```text
  ⚠️ केस्ट्रेल बाढ़ चेतावनी: {locationName}
  अगले 48 घंटों में नदी का जलस्तर खतरनाक सीमा को पार करने का अनुमान है (अधिकतम बहाव: {peakDate})।
  सुरक्षा निर्देश: पशुओं और कीमती सामान को ऊंचे स्थानों पर पहुंचाएं। नदी तटों और जलभराव वाले रास्तों पर जाने से बचें।
  स्रोत: {provenanceLabel} — यह आधिकारिक चेतावनी नहीं है। स्थानीय प्रशासन के निर्देशों का पालन करें।
  ```
- **Marathi (`mr`)**:
  ```text
  ⚠️ केस्ट्रेल पूर इशारा: {locationName}
  पुढील ४८ तासांत नदीची पाणीपातळी धोकादायक पातळी ओलांडण्याची शक्यता आहे (अपेक्षित सर्वोच्च पातळी: {peakDate}).
  सुरक्षा सूचना: जनावरे व महत्त्वाची सामग्री सुरक्षित किंवा उंच जागी हलवा. नदीकाठच्या भागात जाणे टाळा.
  माहिती स्रोत: {provenanceLabel} — हा अधिकृत इशारा नाही. स्थानिक पातळीवर खात्री करा.
  ```

### 2.3 WhatsApp Share Link Generator
Generates a URL-encoded string formatted for Indian community WhatsApp groups:
```
https://wa.me/?text={urlencoded_message}
```
Payload includes bilingual text (English + Regional language) + provenance disclaimer:
```text
🚨 KESTREL FLOOD WARNING / पूर इशारा
📍 Location: Kurla Ward L, Mumbai

English:
River discharge is forecast to exceed dangerous levels within 48h. Avoid riverbanks and move to higher ground.

मराठी:
पुढील ४८ तासांत नदीची पातळी वाढण्याची शक्यता. सखल भागातील नागरिकांनी सुरक्षित स्थळी स्थलांतर करावे.

ℹ️ Provenance: Model Guidance + Local Reports (Confirm locally)
⏰ Sent: 22 Oct 2026, 17:30 IST
```

### 2.4 Delivery Channels
1. **Telegram Bot API**:
   If `kestrel.telegram.bot-token` is configured, dispatches HTTP POST to `https://api.telegram.org/bot<TOKEN>/sendMessage`.
2. **In-Memory Mock Dispatch Queue**:
   All sent alerts are recorded in a thread-safe circular buffer (last 100 items), exposed via `GET /api/v1/fanout/recent`. The frontend Live Dispatch Feed polls this endpoint every 3 seconds.

### 2.5 Community Ground Reports & Provenance Upgrade
Residents submit ground observations via `POST /api/v1/fanout/reports`:
- Categories: `WATER_RISING`, `ROAD_BLOCKED`, `NEED_HELP`, `OTHER`.
- On receipt, the report is saved and active location provenance is automatically upgraded from `MODEL_ONLY` to **`MODEL_PLUS_REPORTS`**.
- The updated provenance immediately reflects across the Coordinator Dashboard and Resident View.

# 03 — Registry, Multilingual Alerting & Dispatch

This document details the location catalog, resident subscriptions, multilingual alert templates (English, Hindi, Marathi), WhatsApp share link generation, and dual-channel dispatching for the **Kestrel Prototype**.

---

## 1. Seeded Indian Pilot Locations

The prototype comes pre-seeded with 4 diverse Indian pilot locations representing urban, riverine, deltaic, and northeastern flood risk profiles:

| ID | Name | State | Lat, Lon | River / Waterbody | 2-Yr (m³/s) | 5-Yr (m³/s) | 20-Yr (m³/s) | Primary Lang |
|---|---|---|---|---|---|---|---|---|
| `mumbai-kurla` | Kurla Ward L, Mumbai | Maharashtra | `19.0726, 72.8797` | Mithi River | 45.0 | 85.0 | 140.0 | Marathi (`mr`) |
| `patna-ward12` | Ward 12, Patna | Bihar | `25.6127, 85.1589` | Ganges River | 38,000.0 | 48,000.0 | 62,000.0 | Hindi (`hi`) |
| `guwahati-river`| Riverside, Guwahati | Assam | `26.1856, 91.7478` | Brahmaputra River | 32,000.0 | 42,000.0 | 55,000.0 | English (`en`) |
| `cuttack-delta` | Cuttack Municipal Corp | Odisha | `20.4625, 85.8830` | Mahanadi River | 18,000.0 | 26,000.0 | 35,000.0 | English (`en`) |

These locations are seeded on backend startup if the database table is empty.

---

## 2. Resident Subscriptions

Residents can opt-in to alerts without personal identifiable information (PII). Only a channel handle (Telegram chat ID or browser identifier) and preferences are stored.

### Subscription Schema
- `id`: Auto-generated long
- `token`: UUID string (e.g. `9a81c7e2-4112-4e89-b1d2-098e721a64bc`) returned to user for unsubscribe
- `locationId`: Targeted ward ID
- `channel`: `TELEGRAM` or `MOCK`
- `recipientIdentifier`: Telegram chat ID (e.g. `"98127391"`) or dummy resident name
- `language`: `en`, `hi`, or `mr`
- `minSeverity`: Filter threshold (`WATCH`, `WARNING`, or `SEVERE`; default `WARNING`)
- `active`: Boolean

---

## 3. Multilingual Alert Templates

Every alert consists of 4 parts:
1. **Header**: Severity badge + Hazard + Location Name
2. **Impact & Horizon**: Forecast peak timing and estimated water rise
3. **Actionable Safety Advice**: Clear, non-technical instructions
4. **Mandatory Provenance Disclaimer**: Explicit guidance note

### 3.1 Flood Templates by Language & Severity

#### WARNING Level (5-Year Flood Risk)
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

#### SEVERE Level (20-Year Extreme Flood Risk)
- **English (`en`)**:
  ```text
  🚨 KESTREL CRITICAL FLOOD ALERT: {locationName}
  Severe, life-threatening inundation forecast within 24 hours.
  Action: Immediate evacuation of low-lying flood zones advised. Follow local disaster management directives.
  Provenance: {provenanceLabel} — Emergency model signal.
  ```
- **Hindi (`hi`)**:
  ```text
  🚨 केस्ट्रेल गंभीर बाढ़ चेतावनी: {locationName}
  अगले 24 घंटों में अत्यधिक जलभराव और भीषण बाढ़ की आशंका।
  सुरक्षा निर्देश: निचले इलाकों से तुरंत सुरक्षित स्थानों पर निकलें। स्थानीय आपदा प्रबंधन के निर्देशों का पालन करें।
  स्रोत: {provenanceLabel} — आपातकालीन मॉडल संकेत।
  ```
- **Marathi (`mr`)**:
  ```text
  🚨 केस्ट्रेल अतिदक्षतेचा पूर इशारा: {locationName}
  पुढील २४ तासांत अत्यंत भीषण पूरस्थिती निर्माण होण्याची शक्यता.
  सुरक्षा सूचना: सखल भागातील नागरिकांनी त्वरित सुरक्षित ठिकाणी स्थलांतर करावे. प्रशासनाच्या सूचनांचे पालन करा.
  माहिती स्रोत: {provenanceLabel} — आपत्कालीन मॉडेल इशारा.
  ```

### 3.2 Provenance Text Mapping

| Provenance Enum | English Display | Hindi Display | Marathi Display |
|---|---|---|---|
| `MODEL_ONLY` | `[Open-Meteo Ensemble Model Guidance]` | `[ओपन-मेटियो मॉडल पूर्वानुमान]` | `[मॉडेल अंदाज]` |
| `MODEL_PLUS_REPORTS` | `[Model Guidance + Resident Ground Reports]` | `[मॉडल + स्थानीय जमीनी रिपोर्ट]` | `[मॉडेल + स्थानिक नागरिकांचे अहवाल]` |
| `OFFICIAL_CONFIRMED` | `[Coordinator Confirmed Broadcast]` | `[संयोजक द्वारा सत्यापित प्रसारण]` | `[समन्वयक पडताळणी केलेले प्रसारण]` |

---

## 4. WhatsApp Share Link Generator

Because community WhatsApp groups are the primary communication channel in India, the prototype provides a one-click **WhatsApp Share Link Generator**:

### URL Format
```
https://wa.me/?text={urlencoded_message}
```

### Pre-formatted WhatsApp Broadcast Payload
The generated WhatsApp message includes a clean bilingual format (English + the local language of the ward):
```text
🚨 KESTREL FLOOD WARNING / पूर इशारा
📍 Location: Kurla Ward L, Mumbai

English:
River discharge is forecast to exceed dangerous levels within the next 48 hours. Avoid riverbanks and move to higher ground.

मराठी:
पुढील ४८ तासांत नदीची पातळी वाढण्याची शक्यता. सखल भागातील नागरिकांनी सुरक्षित स्थळी स्थलांतर करावे.

ℹ️ Provenance: Model Guidance + Local Reports (Confirm locally)
⏰ Sent: 21 Oct 2026, 06:40 IST
```
When clicked by the coordinator, this link automatically opens WhatsApp (mobile app or WhatsApp Web) with the text pre-filled into the message composer.

---

## 5. Dual-Channel Alert Dispatcher

The backend contains a unified `AlertDispatcher` that handles both automated threshold alerts and coordinator manual broadcasts:

```
                  ┌─────────────────────────────────────────┐
                  │          AlertDispatcher                │
                  └──────────────────┬──────────────────────┘
                                     │
                 ┌───────────────────┴───────────────────┐
                 ▼                                       ▼
     ┌───────────────────────┐               ┌───────────────────────┐
     │ TelegramSenderService │               │ MockDispatchQueue     │
     │  (Live Bot API)       │               │  (In-Memory Ring      │
     └───────────────────────┘               │   Buffer for UI Feed) │
                                             └───────────────────────┘
```

### 5.1 Telegram Bot Integration
- If `kestrel.telegram.bot-token` is set in `application.yml` or environment, the service dispatches HTTP `POST` to:
  `https://api.telegram.org/bot<TOKEN>/sendMessage` with JSON:
  ```json
  {
    "chat_id": "<subscriber-chat-id>",
    "text": "<rendered-alert-message>",
    "parse_mode": "HTML"
  }
  ```
- If the token is empty, it skips the live HTTP call and logs an info message.

### 5.2 In-Memory Mock Dispatch Queue
- **Every alert dispatched** is simultaneously written to an in-memory observable ring buffer (max 100 items).
- The frontend polls `GET /api/v1/dispatch/recent` every 3 seconds to display real-time activity in the **Live Dispatch Feed**.
- This guarantees full functional demonstration even without internet access or Telegram credentials.

---

## 6. Community Ground Reports & Provenance Upgrade

Residents can submit quick reports via `POST /api/v1/public/reports`:
- Categories: `WATER_RISING`, `ROAD_BLOCKED`, `NEED_HELP`, `OTHER`.
- When a report is received for a location with an active `WATCH`, `WARNING`, or `SEVERE` status:
  1. The report is saved to `CommunityReportEntity`.
  2. The active `AlertStateEntity.provenance` is upgraded from `MODEL_ONLY` to **`MODEL_PLUS_REPORTS`**.
  3. The coordinator dashboard highlights the updated provenance badge with an unread ground report indicator.

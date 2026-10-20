# 21 — Ethics, Consent, Compliance & Attribution

Applies to **every** task. Violations block a gate.

## 1. Conduct rules (from the project plan)
1. **Label every alert with its source** (model / community / official).
2. **Never imply it is an official warning.** Kestrel is guidance with local confirmation. SACHET/IMD/CWC/NDMA remain the authority.
3. **Opt-in consent** before any alert; **minimal data**; easy withdrawal + erasure.
4. **Do not overclaim**: coarse resolution (≈5 km flood river cell; ≈45 km air-quality model outside Europe), models can pick the wrong river, and no accuracy claim without backtests.
5. **Trust over reach**: low false-alarm ratio is a design goal (persistence, cooldown, human approval for WATCH, corroboration labelling).
6. Never block users from official information; always provide the "follow local authorities / official warnings" line.

## 2. Mandatory disclaimers (keys; exact English defaults)
- `disclaimer.model`: "Model forecast guidance – not an official warning. Please confirm locally."
- `disclaimer.modelReports`: "Model forecast + community reports – not an official warning. Please confirm locally."
- `disclaimer.official`: "Official alert from {authority} via SACHET."
- `disclaimer.noAlert`: "No alert does not mean no risk. Follow local authorities and official warnings."
- `disclaimer.resolution.flood`: "Flood guidance uses a ~5 km river model and may not match your exact river or street."
- `disclaimer.resolution.aq`: "Air quality is estimated from a regional model (~45 km); local conditions may differ."
- `disclaimer.heat`: "Heat advisory is model-based and is not an IMD declaration."
- `disclaimer.reports`: "Reports are shared by residents and are not emergency calls. In an emergency contact local authorities."
Translations to hi/mr must be natively reviewed before real users (`⚠ VERIFY-18`).

## 3. Attribution (required; shown in UI footer/About, API docs, CAP `parameter`/`web`)
- "Weather and flood data by **Open-Meteo.com**" (CC BY 4.0 — verify current wording & link) (`⚠ VERIFY-16`)
- "Air quality: **CAMS** (Copernicus Atmosphere Monitoring Service) via Open-Meteo"
- "Observed air quality: **OpenAQ**" (+ underlying provider attribution if required)
- "Flood thresholds: derived from Open-Meteo/GloFAS reanalysis history" or "GloFAS (Copernicus Emergency Management Service)" when the CSV source is used
- "Official alerts: **SACHET / NDMA**"
- "Map data © **OpenStreetMap** contributors" (tile usage policy: low volume, attribution; use a suitable provider for production load — `⚠ VERIFY-15`)
Open-Meteo free tier is **non-commercial** with no uptime guarantee — keep the project within those terms.

## 4. Privacy & data protection (`⚠ VERIFY-13` — before ANY real user signs up)
Source docs state India's data-protection rules were **not checked**. Before real users: read the Digital Personal Data Protection Act, 2023 and its rules; confirm obligations around consent notice, purpose limitation, retention, erasure, children's data, and data-breach handling; record conclusions in the Verification Register (this is engineering diligence, **not legal advice** — consider asking a qualified professional).
Design defaults already implemented regardless:
- Collected: chosen locations, hazards, language, min severity, channel address (Telegram chat id / push endpoint / optional email), consent record (version, timestamp, hashed IP). **Not collected**: name, phone number, precise location history, photos.
- Reports: coordinates rounded ~110 m; no raw IP/UA stored (salted hash only); retention default 90 days.
- Consent text is versioned; changes require re-consent flow.
- Erasure: `DELETE /subscriptions/me` removes addresses in registry and propagates to fanout replica; consent log anonymized after retention.
- Logs/metrics contain no addresses, tokens or free text notes.
- Subprocessors: Telegram, push services (browser vendors), SMTP provider, **AWS** (Lambda, DynamoDB, SQS, SNS, Cognito, CloudFront, S3, EventBridge, CloudWatch), Amplify Hosting (first 12 months), Cloudflare Pages (after month 12), OpenStreetMap tile provider — list them in the Privacy page.

## 5. Safety-of-messaging checks (automated where possible)
- Templates cannot drop provenance sentence.
- No message uses fear-mongering/imperative absolutes beyond approved action texts.
- `NEED_HELP` reports never auto-forwarded as official; UI says responders are not guaranteed.
- Coordinator can't select OFFICIAL_CONFIRMED manually.
- Preview mandatory before broadcast; idempotent send; audit trail of who sent what.

## 6. Open-data etiquette
Respect rate limits and budgets, cache aggressively, descriptive `User-Agent` with contact, back off on 429, never call OpenAQ/third-party keys from browsers.

## 7. Documentation obligations
README "Limitations" section (resolution, wrong-river risk, free-channel throughput, no uptime guarantees, unproven real-world impact until a pilot), plus "What is real vs simulated" (real: hazard data and alert logic; simulated: subscriber volume, mock sender for load tests).

export type Severity = 'NONE' | 'WATCH' | 'WARNING' | 'SEVERE';
export type Hazard = 'FLOOD' | 'HEAT' | 'AIR_QUALITY';
export type RegionType = 'PLAINS' | 'COASTAL' | 'HILLY';
export type Provenance = 'MODEL_ONLY' | 'MODEL_PLUS_REPORTS' | 'OFFICIAL_CONFIRMED';
export type SupportedLanguage = 'en' | 'hi' | 'mr';
export type DeliveryChannel = 'TELEGRAM' | 'WEB_PUSH' | 'WHATSAPP';

export interface LocationDto {
  id: string;
  name: string;
  state: string;
  latitude: number;
  longitude: number;
  riverBasin: string;
  regionType?: RegionType;
  threshold2yr: number;
  threshold5yr: number;
  threshold20yr: number;
  primaryLanguage: SupportedLanguage;
  active: boolean;
  createdAt?: string;
}

export interface CreateSubscriptionRequest {
  locationId: string;
  channel: DeliveryChannel;
  recipientHandle?: string;
  recipientIdentifier?: string;
  preferredLanguage?: SupportedLanguage;
  language?: SupportedLanguage;
  minSeverity: Severity;
  hazards?: Hazard[];
  consentGiven?: boolean;
  consentVersion?: string;
}

export interface SubscriptionResponse {
  token: string;
  locationId: string;
  channel: DeliveryChannel;
  recipientHandle: string;
  recipientIdentifier?: string;
  preferredLanguage: SupportedLanguage;
  minSeverity: Severity;
  hazards?: Hazard[];
  status?: string;
  active: boolean;
  createdAt: string;
}

export interface ForecastDayStats {
  date: string;
  // Normalized discharge values (m³/s)
  medianDischarge?: number;
  p25Discharge?: number;
  p75Discharge?: number;
  maxDischarge?: number;
  minDischarge?: number;
  p10Discharge?: number;
  p90Discharge?: number;

  // Backend wire format values (FloodSignalPayload.ForecastDayStats)
  qMin?: number;
  qP10?: number;
  qP25?: number;
  qMedian?: number;
  qP75?: number;
  qP90?: number;
  qMax?: number;
  share2yr?: number;
  share5yr?: number;
  share20yr?: number;

  // Exceedance flags & ratios
  exceeds2yr?: boolean;
  exceeds5yr?: boolean;
  exceeds20yr?: boolean;
  exceedanceShare?: number;
}

export function normalizeForecastDay(
  day: Partial<ForecastDayStats>,
  thresholds?: { rp2: number; rp5: number; rp20: number }
): ForecastDayStats {
  const median = day.medianDischarge ?? day.qMedian ?? 0;
  const p25 = day.p25Discharge ?? day.qP25 ?? 0;
  const p75 = day.p75Discharge ?? day.qP75 ?? 0;
  const max = day.maxDischarge ?? day.qMax ?? 0;
  const min = day.minDischarge ?? day.qMin ?? 0;

  // Derive p10 and p90 for 10-day ensemble spread if not provided directly
  const p10 = day.p10Discharge ?? day.qP10 ?? Math.max(0, min + (p25 - min) * 0.4);
  const p90 = day.p90Discharge ?? day.qP90 ?? (p75 + (max - p75) * 0.6);

  const share2yr = day.share2yr ?? (day.exceeds2yr ? 1 : 0);
  const share5yr = day.share5yr ?? (day.exceeds5yr ? 1 : 0);
  const share20yr = day.share20yr ?? (day.exceeds20yr ? 1 : 0);
  const exceedanceShare =
    day.exceedanceShare ?? Math.max(share2yr, share5yr, share20yr, 0);

  const rp2 = thresholds?.rp2 ?? 40;
  const rp5 = thresholds?.rp5 ?? 85;
  const rp20 = thresholds?.rp20 ?? 180;

  return {
    ...day,
    date: day.date || new Date().toISOString().split('T')[0],
    medianDischarge: median,
    p25Discharge: p25,
    p75Discharge: p75,
    maxDischarge: max,
    minDischarge: min,
    p10Discharge: p10,
    p90Discharge: p90,
    qMedian: median,
    qP25: p25,
    qP75: p75,
    qMax: max,
    qMin: min,
    qP10: p10,
    qP90: p90,
    share2yr,
    share5yr,
    share20yr,
    exceeds2yr: day.exceeds2yr ?? (share2yr >= 0.3 || median >= rp2),
    exceeds5yr: day.exceeds5yr ?? (share5yr >= 0.3 || median >= rp5),
    exceeds20yr: day.exceeds20yr ?? (share20yr >= 0.3 || median >= rp20),
    exceedanceShare,
  };
}

export interface LocationAlertStatusResponse {
  locationId: string;
  locationName: string;
  hazard: Hazard;
  severity: Severity;
  provenance: Provenance;
  activeAlertId?: string | null;
  firstExceedanceDate?: string | null;
  peakExceedanceShare?: number | null;
  dataAsOf?: string | null;
  dataAgeSeconds?: number | null;
  isStale: boolean;
  lastTransitionAt?: string | null;
}

export interface LocationForecastResponse {
  locationId: string;
  memberCount?: number;
  dataAsOf?: string;
  isSimulated?: boolean;
  thresholds: {
    rp2: number;
    rp5: number;
    rp20: number;
  };
  days: ForecastDayStats[];
}

export function normalizeLocationForecastResponse(
  resp?: LocationForecastResponse | null
): LocationForecastResponse | null {
  if (!resp) return null;
  const thresholds = resp.thresholds || { rp2: 40, rp5: 85, rp20: 180 };
  const days = (resp.days || []).map((d) => normalizeForecastDay(d, thresholds));
  return {
    ...resp,
    thresholds,
    days,
  };
}

export interface SyncLiveResponse {
  message: string;
  syncedLocationsCount: number;
}

export interface SyncLiveLocationResponse {
  locationId: string;
  memberCount: number;
  success: boolean;
  message: string;
}

export interface BroadcastPreviewRequest {
  locationId: string;
  hazard: string;
  severity: string;
  provenance?: Provenance;
  customNote?: string;
}

export interface BroadcastPreviewResponse {
  locationId: string;
  locationName: string;
  hazard: Hazard;
  severity: Severity;
  provenance: Provenance;
  englishMessage: string;
  hindiMessage: string;
  marathiMessage: string;
  whatsAppShareUrl: string;
  estimatedRecipients: number;
}

export interface BroadcastRequest {
  locationId: string;
  hazard: string;
  severity: string;
  provenance?: Provenance;
  customNote?: string;
}

export interface BroadcastResponse {
  broadcastId: string;
  locationId: string;
  locationName: string;
  severity: Severity;
  provenance: Provenance;
  totalAttempted: number;
  totalSuccessful: number;
  whatsAppShareUrl: string;
  dispatchedAt: string;
}

export interface DispatchItem {
  dispatchId: string;
  timestamp: string;
  locationId: string;
  locationName: string;
  hazard: string;
  severity: Severity;
  provenance: Provenance;
  channel: DeliveryChannel;
  recipient: string;
  language: SupportedLanguage;
  messageExcerpt: string;
  status: string;
}

export type GroundReportCategory = 'WATER_RISING' | 'ROAD_BLOCKED' | 'NEED_HELP' | 'OTHER';

export interface CreateReportRequest {
  locationId: string;
  reporterName?: string;
  category: GroundReportCategory;
  description: string;
  waterLevelCm?: number;
}

export interface ReportResponse {
  reportId: string;
  locationId: string;
  category: string;
  description: string;
  reportedAt: string;
  provenanceUpgradedTo: Provenance;
  message: string;
}

export interface CommunityReportDto {
  id: string;
  locationId: string;
  reporterName: string;
  category: GroundReportCategory;
  description: string;
  waterLevelCm?: number;
  reportedAt: string;
}

// ==================== EXTREME HEAT TYPES ====================

export interface HeatPointSummary {
  locationId: string;
  name: string;
  state: string;
  latitude: number;
  longitude: number;
  regionType: RegionType;
  currentTmax: number | null;
  currentApparentMax: number | null;
  normalTmax: number | null;
  departure: number | null;
  dataAsOf: string | null;
  ageHours: number | null;
  isStale: boolean;
}

export interface HeatForecastDayItem {
  date: string;
  tmax: number;
  apparentMax: number;
  humidityMax: number;
  normalTmax: number;
  departure: number;
  gateMet: boolean;
  isHeatWave: boolean;
  isSevereHeatWave: boolean;
}

export interface HeatForecastDetails {
  locationId: string;
  regionType: RegionType;
  dataAsOf: string;
  ageHours: number;
  isStale: boolean;
  days: HeatForecastDayItem[];
}

export interface HeatSyncResponse {
  message: string;
  syncedLocationsCount: number;
  locationIds: string[];
  success: boolean;
}

// ==================== AIR QUALITY (CPCB AQI) TYPES ====================

export type AqiCategory =
  | 'GOOD'
  | 'SATISFACTORY'
  | 'MODERATE'
  | 'POOR'
  | 'VERY_POOR'
  | 'SEVERE';

export type Pollutant = 'PM25' | 'PM10' | 'NO2' | 'SO2' | 'O3' | 'CO';

export interface AqPointSummary {
  locationId: string;
  name: string;
  state: string;
  latitude: number;
  longitude: number;
  pm25_24h: number | null;
  pm10_24h: number | null;
  no2_24h: number | null;
  so2_24h: number | null;
  o3_8h: number | null;
  co_8h_mgm3: number | null;
  validHours: number | null;
  aqi: number | null;
  category: AqiCategory | null;
  dominantPollutant: Pollutant | null;
  severity: Severity;
  subIndices: Record<string, number>;
  dataAsOf: string | null;
  ageHours: number | null;
  isStale: boolean;
}

export interface AqForecastDayItem {
  date: string;
  pm25: number;
  pm10: number;
  no2: number;
  so2: number;
  o3: number;
  co: number;
  projectedAqi: number | null;
  projectedCategory: AqiCategory | null;
  dominantPollutant: Pollutant | null;
  severity: Severity;
  subIndices: Record<string, number>;
}

export interface AqForecastDetails {
  locationId: string;
  dataAsOf: string;
  ageHours: number;
  isStale: boolean;
  days: AqForecastDayItem[];
}

export interface AqSyncResponse {
  message: string;
  syncedLocationsCount: number;
  locationIds: string[];
  success: boolean;
}


export type Severity = 'NONE' | 'WATCH' | 'WARNING' | 'SEVERE';
export type Hazard = 'FLOOD' | 'HEAT' | 'AIR_QUALITY';
export type Provenance = 'MODEL_ONLY' | 'MODEL_PLUS_REPORTS' | 'OFFICIAL_CONFIRMED';
export type SupportedLanguage = 'en' | 'hi' | 'mr';
export type DeliveryChannel = 'TELEGRAM' | 'WHATSAPP' | 'MOCK' | 'SMS' | 'PUSH';

export interface LocationDto {
  id: string;
  name: string;
  state: string;
  latitude: number;
  longitude: number;
  riverBasin: string;
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
  recipientHandle: string;
  preferredLanguage: SupportedLanguage;
  minSeverity: Severity;
}

export interface SubscriptionResponse {
  token: string;
  locationId: string;
  channel: DeliveryChannel;
  recipientHandle: string;
  preferredLanguage: SupportedLanguage;
  minSeverity: Severity;
  active: boolean;
  createdAt: string;
}

export interface ForecastDayStats {
  date: string;
  medianDischarge: number;
  p25Discharge: number;
  p75Discharge: number;
  maxDischarge: number;
  exceeds2yr: boolean;
  exceeds5yr: boolean;
  exceeds20yr: boolean;
  exceedanceShare: number;
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
  thresholds: {
    rp2: number;
    rp5: number;
    rp20: number;
  };
  days: ForecastDayStats[];
}

export type ScenarioType =
  | 'CALM'
  | 'WATCH_2YR'
  | 'WARNING_5YR'
  | 'SEVERE_20YR'
  | 'LIVE_OPEN_METEO';

export interface SimulationRequest {
  locationId: string;
  scenario: ScenarioType;
}

export interface SimulationResponse {
  scenario: string;
  locationId: string;
  message: string;
}

export interface SyncLiveResponse {
  message: string;
  syncedLocationsCount: number;
}

export interface BroadcastPreviewRequest {
  locationId: string;
  hazard: string;
  severity: string;
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

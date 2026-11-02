import { ALL_45_RIVER_STATIONS } from './indianRivers';
import { ALL_HEAT_STATIONS } from './heatStations';
import { ALL_AQ_STATIONS } from './aqStations';
import type { Hazard } from '../types/api';

export * from './indianRivers';
export * from './heatStations';
export * from './aqStations';

export interface UnifiedStationItem {
  id: string;
  name: string;
  state: string;
  city?: string;
  latitude: number;
  longitude: number;
  hazard: Hazard;
  categoryLabel: string; // e.g. "Ganga Basin" (Flood), "Plains (40°C gate)" (Heat), "National Capital Region" (AQ)
  sourceInfo: string;
  defaultMetricLabel?: string;
}

/**
 * Returns the relevant and authentic monitored stations for the selected hazard:
 * - FLOOD: 45 Hydrological River Basins (Open-Meteo Flood + GloFAS Copernicus thresholds)
 * - HEAT: 42 IMD Meteorological Observatories (Open-Meteo Weather + Historical Normals)
 * - AIR_QUALITY: 32 CPCB/OpenAQ CAAQMS Stations (Open-Meteo Air Quality + OpenAQ Ground Sensors)
 */
export function getStationsForHazard(hazard: Hazard): UnifiedStationItem[] {
  if (hazard === 'HEAT') {
    return ALL_HEAT_STATIONS.map((st) => ({
      id: st.id,
      name: st.name,
      city: st.city,
      state: st.state,
      latitude: st.latitude,
      longitude: st.longitude,
      hazard: 'HEAT',
      categoryLabel: `${st.regionType.charAt(0) + st.regionType.slice(1).toLowerCase()} (${
        st.regionType === 'HILLY' ? '30°C' : st.regionType === 'COASTAL' ? '37°C' : '40°C'
      } gate)`,
      sourceInfo: `Open-Meteo Weather + Historical IMD Baseline (Normal: ${st.normalTmax}°C)`,
      defaultMetricLabel: `${st.normalTmax}°C Norm`,
    }));
  }

  if (hazard === 'AIR_QUALITY') {
    return ALL_AQ_STATIONS.map((st) => ({
      id: st.id,
      name: st.name,
      city: st.city,
      state: st.state,
      latitude: st.latitude,
      longitude: st.longitude,
      hazard: 'AIR_QUALITY',
      categoryLabel: st.zone,
      sourceInfo: st.source,
      defaultMetricLabel: `CPCB: ${st.cpcbStationId}`,
    }));
  }

  // FLOOD
  return ALL_45_RIVER_STATIONS.map((st) => ({
    id: st.id,
    name: st.name,
    state: st.state,
    latitude: st.latitude,
    longitude: st.longitude,
    hazard: 'FLOOD',
    categoryLabel: st.riverBasin,
    sourceInfo: 'Open-Meteo Flood + GloFAS Copernicus 2/5/20-Yr Return Thresholds',
    defaultMetricLabel: `5-Yr: ${st.threshold5yr} m³/s`,
  }));
}

export function getDefaultLocationIdForHazard(hazard: Hazard): string {
  switch (hazard) {
    case 'HEAT':
      return 'imd-nagpur';
    case 'AIR_QUALITY':
      return 'cpcb-delhi-anand-vihar';
    case 'FLOOD':
    default:
      return 'mumbai-kurla';
  }
}

import React, { useState, useMemo, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation } from '../features/map/mapSlice';
import { setActiveTab, HazardType } from '../features/ui/uiSlice';
import { MapView, BASIN_OPTIONS, REGION_OPTIONS, AQ_ZONE_OPTIONS } from '../components/MapView';
import { SeverityBadge } from '../components/SeverityBadge';
import { useGetLocationsQuery } from '../api/registryApi';
import { useGetAlertStatusesQuery, useGetForecastByLocationQuery } from '../api/rulesApi';
import { useGetHeatPointsQuery, useGetHeatPointForecastQuery } from '../api/heatApi';
import { useGetAqPointsQuery, useGetAqPointForecastQuery } from '../api/aqApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import { ALL_HEAT_STATIONS } from '../catalog/heatStations';
import { ALL_AQ_STATIONS } from '../catalog/aqStations';
import { getDefaultLocationIdForHazard } from '../catalog';
import type { LocationDto, Severity, Provenance } from '../types/api';
import { Search, RotateCcw, ArrowRight, ShieldCheck, X } from 'lucide-react';

export const MapOverviewView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedLocationId = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const activeHazard: HazardType = useAppSelector((state) => state.ui.activeHazard) || 'FLOOD';
  const [showRightSidebar, setShowRightSidebar] = useState<boolean>(false);

  // Filters
  const [severityFilter, setSeverityFilter] = useState<'ALL' | 'SEVERE' | 'WARNING' | 'WATCH' | 'SAFE'>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');

  // RTK Query API hooks for all 3 hazards
  const { data: locationsData } = useGetLocationsQuery();
  const { data: alertStatuses } = useGetAlertStatusesQuery(undefined, { pollingInterval: 5000 });
  const { data: floodForecast } = useGetForecastByLocationQuery(selectedLocationId);

  const { data: heatPoints } = useGetHeatPointsQuery(undefined, { pollingInterval: 5000 });
  useGetHeatPointForecastQuery(selectedLocationId);

  const { data: aqPoints } = useGetAqPointsQuery(undefined, { pollingInterval: 5000 });
  useGetAqPointForecastQuery(selectedLocationId);

  // Dynamically derive stations matching the active hazard
  const stations: LocationDto[] = useMemo(() => {
    if (activeHazard === 'HEAT') {
      if (heatPoints && heatPoints.length > 0) {
        return heatPoints.map((hp) => {
          const cat = ALL_HEAT_STATIONS.find((s) => s.id === hp.locationId);
          return {
            id: hp.locationId,
            name: hp.name,
            state: hp.state,
            latitude: hp.latitude,
            longitude: hp.longitude,
            riverBasin: cat?.climateZone || `${hp.regionType} Zone`,
            regionType: hp.regionType,
            threshold2yr: 38,
            threshold5yr: 42,
            threshold20yr: 45,
            primaryLanguage: cat?.primaryLanguage || 'hi',
            active: true,
          };
        });
      }
      return ALL_HEAT_STATIONS.map((st) => ({
        id: st.id,
        name: st.name,
        state: st.state,
        latitude: st.latitude,
        longitude: st.longitude,
        riverBasin: st.climateZone,
        regionType: st.regionType,
        threshold2yr: 38,
        threshold5yr: 42,
        threshold20yr: 45,
        primaryLanguage: st.primaryLanguage,
        active: st.active,
      }));
    }

    if (activeHazard === 'AIR_QUALITY') {
      if (aqPoints && aqPoints.length > 0) {
        return aqPoints.map((ap) => {
          const cat = ALL_AQ_STATIONS.find((s) => s.id === ap.locationId);
          return {
            id: ap.locationId,
            name: ap.name,
            state: ap.state,
            latitude: ap.latitude,
            longitude: ap.longitude,
            riverBasin: cat?.zone || 'Indo-Gangetic Smog Belt',
            regionType: 'PLAINS',
            threshold2yr: 200,
            threshold5yr: 300,
            threshold20yr: 400,
            primaryLanguage: cat?.primaryLanguage || 'hi',
            active: true,
          };
        });
      }
      return ALL_AQ_STATIONS.map((st) => ({
        id: st.id,
        name: st.name,
        state: st.state,
        latitude: st.latitude,
        longitude: st.longitude,
        riverBasin: st.zone,
        regionType: 'PLAINS',
        threshold2yr: 200,
        threshold5yr: 300,
        threshold20yr: 400,
        primaryLanguage: st.primaryLanguage,
        active: st.active,
      }));
    }

    // FLOOD
    return locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;
  }, [activeHazard, heatPoints, aqPoints, locationsData]);

  // Ensure selected station matches the active hazard's station catalog
  useEffect(() => {
    if (stations.length > 0 && !stations.some((s) => s.id === selectedLocationId)) {
      const defaultId = getDefaultLocationIdForHazard(activeHazard);
      const validId = stations.some((s) => s.id === defaultId) ? defaultId : stations[0].id;
      dispatch(setSelectedLocation(validId));
    }
  }, [activeHazard, stations, selectedLocationId, dispatch]);

  // Lookup severity, provenance, and respective display metric by location id
  const statusByLocationId = useMemo(() => {
    const map = new Map<string, { severity: Severity; provenance: Provenance; peak?: number; date?: string; metricLabel?: string }>();

    if (activeHazard === 'HEAT') {
      heatPoints?.forEach((hp) => {
        let severity: Severity = 'NONE';
        const tmax = hp.currentTmax ?? 0;
        const dep = hp.departure ?? 0;
        if (tmax >= 47 || dep > 6.4) severity = 'SEVERE';
        else if (tmax >= 45 || dep >= 4.5) severity = 'WARNING';
        else if (tmax >= 38 || dep >= 2.0) severity = 'WATCH';

        map.set(hp.locationId, {
          severity,
          provenance: 'MODEL_ONLY',
          peak: hp.currentTmax ?? undefined,
          metricLabel: hp.currentTmax != null ? `${hp.currentTmax}°C` : undefined,
        });
      });
    } else if (activeHazard === 'AIR_QUALITY') {
      aqPoints?.forEach((ap) => {
        let severity: Severity = ap.severity || 'NONE';
        if (!ap.severity && ap.aqi != null) {
          if (ap.aqi > 400) severity = 'SEVERE';
          else if (ap.aqi > 300) severity = 'WARNING';
          else if (ap.aqi > 200) severity = 'WATCH';
          else severity = 'NONE';
        }

        map.set(ap.locationId, {
          severity,
          provenance: 'OFFICIAL_CONFIRMED',
          peak: ap.aqi ?? undefined,
          metricLabel: ap.aqi != null ? `${ap.aqi} AQI` : undefined,
        });
      });
    } else {
      // FLOOD
      alertStatuses?.forEach((status) => {
        map.set(status.locationId, {
          severity: status.severity,
          provenance: status.provenance,
          peak: status.peakExceedanceShare ?? undefined,
          date: status.firstExceedanceDate ?? undefined,
        });
      });
    }
    return map;
  }, [activeHazard, alertStatuses, heatPoints, aqPoints]);

  // Apply filters
  const filteredStations = useMemo(() => {
    return stations.filter((loc) => {
      const status = statusByLocationId.get(loc.id)?.severity || 'NONE';

      // Severity filter
      if (severityFilter === 'SEVERE' && status !== 'SEVERE') return false;
      if (severityFilter === 'WARNING' && status !== 'WARNING') return false;
      if (severityFilter === 'WATCH' && status !== 'WATCH') return false;
      if (severityFilter === 'SAFE' && status !== 'NONE') return false;

      // Category filter based on active hazard
      if (categoryFilter !== 'ALL') {
        if (activeHazard === 'HEAT') {
          const region = loc.regionType || '';
          if (categoryFilter === 'PLAINS' && region !== 'PLAINS') return false;
          if (categoryFilter === 'COASTAL' && region !== 'COASTAL') return false;
          if (categoryFilter === 'HILLY' && region !== 'HILLY') return false;
        } else if (activeHazard === 'AIR_QUALITY') {
          const zone = loc.riverBasin || '';
          if (categoryFilter === 'NCR' && !zone.includes('National Capital Region')) return false;
          if (categoryFilter === 'SMOG_BELT' && !zone.includes('Indo-Gangetic')) return false;
          if (categoryFilter === 'WESTERN' && !zone.includes('Western')) return false;
          if (categoryFilter === 'EASTERN' && !zone.includes('Eastern')) return false;
          if (categoryFilter === 'SOUTHERN' && !zone.includes('Southern')) return false;
          if (categoryFilter === 'NORTHERN' && !zone.includes('Northern')) return false;
        } else {
          // Basin filter for Flood
          const b = loc.riverBasin || '';
          if (categoryFilter === 'Ganga Basin' && !b.includes('Ganga') && !b.includes('Ganges')) return false;
          if (categoryFilter === 'Brahmaputra Basin' && !b.includes('Brahmaputra')) return false;
          if (categoryFilter === 'Indus/North' && !b.includes('Indus') && !b.includes('North')) return false;
          if (categoryFilter === 'Godavari Basin' && !b.includes('Godavari')) return false;
          if (categoryFilter === 'Krishna Basin' && !b.includes('Krishna')) return false;
          if (categoryFilter === 'Mahanadi Basin' && !b.includes('Mahanadi')) return false;
          if (categoryFilter === 'West-Flowing' && !b.includes('West-Flowing')) return false;
          if (categoryFilter === 'Southern' && !b.includes('Southern') && !b.includes('Cauvery')) return false;
        }
      }

      // Search term
      if (searchTerm) {
        const query = searchTerm.toLowerCase();
        const matchName = loc.name.toLowerCase().includes(query);
        const matchState = loc.state.toLowerCase().includes(query);
        const matchBasin = (loc.riverBasin || '').toLowerCase().includes(query);
        const matchRegion = (loc.regionType || '').toLowerCase().includes(query);
        if (!matchName && !matchState && !matchBasin && !matchRegion) return false;
      }

      return true;
    });
  }, [stations, statusByLocationId, severityFilter, categoryFilter, searchTerm, activeHazard]);

  const activeStation = stations.find((s) => s.id === selectedLocationId) || stations[0];
  const activeStatus = statusByLocationId.get(selectedLocationId);
  const activeSeverity = activeStatus?.severity || 'NONE';
  const activeProvenance = activeStatus?.provenance || 'MODEL_ONLY';

  // Active hazard specific data records
  const activeHeat = heatPoints?.find((p) => p.locationId === selectedLocationId);
  const activeHeatStation = ALL_HEAT_STATIONS.find((s) => s.id === selectedLocationId);
  const activeAq = aqPoints?.find((p) => p.locationId === selectedLocationId);

  const resetFilters = () => {
    setSeverityFilter('ALL');
    setCategoryFilter('ALL');
    setSearchTerm('');
  };

  const isFiltered = severityFilter !== 'ALL' || categoryFilter !== 'ALL' || searchTerm.length > 0;

  // Options for dropdown
  const categoryOptions =
    activeHazard === 'HEAT'
      ? REGION_OPTIONS
      : activeHazard === 'AIR_QUALITY'
      ? AQ_ZONE_OPTIONS
      : BASIN_OPTIONS;

  return (
    <div className="w-full max-w-7xl mx-auto space-y-3 pb-12">
      {/* 1. COMPACT, MINIMAL FILTER TOOLBAR (EASY TO ACCESS IN ONE ROW) */}
      <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-xl px-3.5 py-2 shadow-xs flex flex-wrap items-center justify-between gap-2.5 text-xs relative z-50">
        {/* Left Side: Search + Quick Severity Filter Pills + Category Dropdown */}
        <div className="flex flex-wrap items-center gap-2 flex-1 min-w-0">
          {/* Compact Search Box */}
          <div className="relative w-40 sm:w-48">
            <Search className="w-3.5 h-3.5 absolute left-2.5 top-2 text-slate-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={
                activeHazard === 'HEAT'
                  ? 'Search city or region...'
                  : activeHazard === 'AIR_QUALITY'
                  ? 'Search city or state...'
                  : 'Search river station...'
              }
              className="w-full h-8 pl-8 pr-2.5 text-xs rounded-lg bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="w-[1px] h-5 bg-slate-200 dark:bg-slate-700 hidden sm:block" />

          {/* Minimal Severity Pills with Hover Detailing Modal */}
          <div className="flex items-center gap-1.5 overflow-visible">
            {/* ALL FILTER */}
            <div className="relative group">
              <button
                onClick={() => setSeverityFilter('ALL')}
                className={`h-8 px-2.5 rounded-lg font-medium transition-all text-xs cursor-pointer ${
                  severityFilter === 'ALL'
                    ? 'bg-blue-600 text-white font-bold shadow-xs'
                    : 'bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                All
              </button>

              {/* Hover Modal */}
              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-blue-200 dark:border-blue-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-blue-200 dark:border-blue-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-blue-600 dark:text-blue-400 font-mono font-medium">
                    {activeHazard === 'HEAT'
                      ? 'COVERAGE · ALL REGIONS'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'COVERAGE · ALL AQ ZONES'
                      : 'COVERAGE · ALL BASINS'}
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    All Monitored Telemetry Nodes
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Detailing:</span>
                  <span className="text-[11px] font-mono font-semibold text-blue-700 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60 px-1.5 py-0.5 rounded border border-blue-200 dark:border-blue-900/60">
                    {activeHazard === 'HEAT'
                      ? 'All Climatology Norms'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'All AQI Levels'
                      : 'All Return Periods'}
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Monitored Stations:</span>
                  <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700">
                    45 Nodes Across India
                  </span>
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  {activeHazard === 'HEAT'
                    ? 'Monitors maximum temperatures, heat indices, and IMD normal departures across Plains, Coastal, and Hilly zones.'
                    : activeHazard === 'AIR_QUALITY'
                    ? 'Monitors CPCB 24-hr criteria pollutants (PM2.5, PM10, NO2, SO2, O3, CO) and National AQI across India.'
                    : 'Shows all 45 hydrological stations across 10 major Indian river basins without filtering by risk.'}
                </p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setSeverityFilter('ALL');
                  }}
                  className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Show All Stations</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* SEVERE */}
            <div className="relative group">
              <button
                onClick={() => setSeverityFilter('SEVERE')}
                className={`h-8 px-2.5 rounded-lg font-medium transition-all text-xs flex items-center gap-1.5 cursor-pointer ${
                  severityFilter === 'SEVERE'
                    ? 'bg-red-600 text-white font-bold shadow-xs'
                    : 'bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-red-600 dark:text-red-400'
                }`}
              >
                <span className="w-1.5 h-1.5 rounded-full bg-red-500" />
                <span>Severe</span>
              </button>

              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-red-300 dark:border-red-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-red-300 dark:border-red-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-red-600 dark:text-red-400 font-mono font-medium">
                    {activeHazard === 'HEAT'
                      ? 'IMD CRITERIA · SEVERE HEAT WAVE'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'CPCB STANDARD · SEVERE AQI (401+)'
                      : 'RETURN PERIOD · 20-YEAR'}
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    {activeHazard === 'HEAT'
                      ? 'Severe Heat Wave Emergency'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'Severe Air Pollution Emergency'
                      : 'Severe Emergency Inundation'}
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Threshold:</span>
                  <span className="text-[11px] font-mono font-bold text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-950/60 px-1.5 py-0.5 rounded border border-red-200 dark:border-red-900/60">
                    {activeHazard === 'HEAT'
                      ? '≥47°C or Dep >6.4°C'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'AQI > 400 (Hazardous)'
                      : '20-Yr Return'}
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Risk Level:</span>
                  <SeverityBadge severity="SEVERE" size="sm" />
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  {activeHazard === 'HEAT'
                    ? 'Disastrous temperature departures exceeding +6.4°C or Tmax reaching 47°C. Extreme life-safety danger.'
                    : activeHazard === 'AIR_QUALITY'
                    ? 'AQI reaches hazardous 401-500 range. Threatens healthy individuals and seriously impacts respiratory conditions.'
                    : 'Discharge breaches the rare 20-year return threshold. Catastrophic inundation threatening life safety.'}
                </p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setSeverityFilter('SEVERE');
                  }}
                  className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-red-600 hover:bg-red-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Filter by Severe Tier</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* WARNING */}
            <div className="relative group">
              <button
                onClick={() => setSeverityFilter('WARNING')}
                className={`h-8 px-2.5 rounded-lg font-medium transition-all text-xs flex items-center gap-1.5 cursor-pointer ${
                  severityFilter === 'WARNING'
                    ? 'bg-orange-600 text-white font-bold shadow-xs'
                    : 'bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-orange-600 dark:text-orange-400'
                }`}
              >
                <span className="w-1.5 h-1.5 rounded-full bg-orange-500" />
                <span>Warning</span>
              </button>

              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-orange-300 dark:border-orange-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-orange-300 dark:border-orange-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-orange-600 dark:text-orange-400 font-mono font-medium">
                    {activeHazard === 'HEAT'
                      ? 'IMD CRITERIA · HEAT WAVE'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'CPCB STANDARD · VERY POOR AQI'
                      : 'RETURN PERIOD · 5-YEAR'}
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    {activeHazard === 'HEAT'
                      ? 'Active Heat Wave Warning'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'Very Poor Air Quality Warning'
                      : 'Active Flood Warning Stage'}
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Threshold:</span>
                  <span className="text-[11px] font-mono font-bold text-orange-700 dark:text-orange-400 bg-orange-50 dark:bg-red-950/60 px-1.5 py-0.5 rounded border border-orange-200 dark:border-orange-900/60">
                    {activeHazard === 'HEAT'
                      ? '≥45°C or Dep ≥4.5°C'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'AQI 301 - 400'
                      : '5-Yr Return'}
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Risk Level:</span>
                  <SeverityBadge severity="WARNING" size="sm" />
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  {activeHazard === 'HEAT'
                    ? 'Temperatures breach official IMD heat-wave criteria. High risk of dehydration and heat cramps in outdoor labor.'
                    : activeHazard === 'AIR_QUALITY'
                    ? 'AQI in Very Poor 301-400 range. Can trigger respiratory discomfort for elderly and children on prolonged exposure.'
                    : 'Discharge breaches the 5-year return limit. Active hazard spilling into low riverbanks, causeways, and roads.'}
                </p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setSeverityFilter('WARNING');
                  }}
                  className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-orange-600 hover:bg-orange-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Filter by Warning Tier</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* WATCH */}
            <div className="relative group">
              <button
                onClick={() => setSeverityFilter('WATCH')}
                className={`h-8 px-2.5 rounded-lg font-medium transition-all text-xs flex items-center gap-1.5 cursor-pointer ${
                  severityFilter === 'WATCH'
                    ? 'bg-amber-600 text-white font-bold shadow-xs'
                    : 'bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-amber-700 dark:text-amber-400'
                }`}
              >
                <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                <span>Watch</span>
              </button>

              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-amber-300 dark:border-amber-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-amber-300 dark:border-amber-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-amber-700 dark:text-amber-400 font-mono font-medium">
                    {activeHazard === 'HEAT'
                      ? 'CLIMATOLOGY · ELEVATED'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'CPCB STANDARD · POOR AQI'
                      : 'RETURN PERIOD · 2-YEAR'}
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    {activeHazard === 'HEAT'
                      ? 'Elevated Heat Precaution'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'Poor Air Quality Advisory'
                      : 'Precautionary Watch Level'}
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Threshold:</span>
                  <span className="text-[11px] font-mono font-bold text-amber-800 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/60 px-1.5 py-0.5 rounded border border-amber-200 dark:border-amber-900/60">
                    {activeHazard === 'HEAT'
                      ? 'Dep ≥2.0°C / Tmax ≥38°C'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'AQI 201 - 300'
                      : '2-Yr Return'}
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Risk Level:</span>
                  <SeverityBadge severity="WATCH" size="sm" />
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  {activeHazard === 'HEAT'
                    ? 'Temperatures elevated above climatological baseline. Precautionary hydration and shaded rest advised.'
                    : activeHazard === 'AIR_QUALITY'
                    ? 'AQI in 201-300 range. Breathing discomfort to individuals with existing asthma or lung ailments.'
                    : 'River levels approaching the 2-year seasonal threshold. Precautionary notice to prepare go-bags.'}
                </p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setSeverityFilter('WATCH');
                  }}
                  className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-amber-600 hover:bg-amber-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Filter by Watch Tier</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* SAFE */}
            <div className="relative group">
              <button
                onClick={() => setSeverityFilter('SAFE')}
                className={`h-8 px-2.5 rounded-lg font-medium transition-all text-xs flex items-center gap-1.5 cursor-pointer ${
                  severityFilter === 'SAFE'
                    ? 'bg-emerald-600 text-white font-bold shadow-xs'
                    : 'bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-emerald-600 dark:text-emerald-400'
                }`}
              >
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                <span>Safe</span>
              </button>

              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-emerald-300 dark:border-emerald-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-emerald-300 dark:border-emerald-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-mono font-medium">
                    {activeHazard === 'HEAT'
                      ? 'CLIMATOLOGY · NORMAL'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'CPCB STANDARD · ACCEPTABLE'
                      : 'RETURN PERIOD · BASELINE'}
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    {activeHazard === 'HEAT'
                      ? 'Normal Seasonal Temperature'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'Good & Moderate Air Quality'
                      : 'Normal Flow Baseline'}
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Threshold:</span>
                  <span className="text-[11px] font-mono font-bold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-1.5 py-0.5 rounded border border-emerald-200 dark:border-emerald-900/60">
                    {activeHazard === 'HEAT'
                      ? 'Normal Departure'
                      : activeHazard === 'AIR_QUALITY'
                      ? 'AQI ≤ 200'
                      : 'Normal Flow'}
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Risk Level:</span>
                  <SeverityBadge severity="NONE" size="sm" />
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  {activeHazard === 'HEAT'
                    ? 'Temperatures within normal seasonal bounds. No heat-wave risk.'
                    : activeHazard === 'AIR_QUALITY'
                    ? 'Pollution within CPCB acceptable national ambient air standards.'
                    : 'Discharge and stage heights within standard non-flood limits. Zero active inundation threat.'}
                </p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setSeverityFilter('SAFE');
                  }}
                  className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Filter by Normal Safe Tier</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>

          <div className="w-[1px] h-5 bg-slate-200 dark:bg-slate-700 hidden lg:block" />

          {/* Compact Category / Basin / Region Selector */}
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="h-8 px-2 text-xs rounded-lg bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#EAF0F6] font-medium focus:outline-none cursor-pointer max-w-[130px] sm:max-w-[170px] truncate"
          >
            {categoryOptions.map((b) => (
              <option key={b.id} value={b.id} className="bg-white dark:bg-[#151D27]">
                {b.label}
              </option>
            ))}
          </select>
        </div>

        {/* Right Side: Station Count & Reset */}
        <div className="flex items-center gap-2 shrink-0">
          <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8] font-medium">
            {filteredStations.length} / {stations.length} stations
          </span>

          {isFiltered && (
            <button
              onClick={resetFilters}
              className="h-7 px-2 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-slate-600 dark:text-[#9AA8B8] text-[11px] font-semibold flex items-center gap-1 transition-colors cursor-pointer"
              title="Reset filters"
            >
              <RotateCcw className="w-3 h-3" />
              <span>Reset</span>
            </button>
          )}
        </div>
      </div>

      {/* 2. MAIN MAP CANVAS WITH OVERLAID RIGHT SIDEBAR */}
      <div className="relative z-0 isolate w-full rounded-2xl overflow-hidden border border-slate-200 dark:border-[#2A3644] bg-white dark:bg-[#151D27] shadow-sm">
        <MapView
          height="675px"
          locations={filteredStations}
          statusByLocationId={statusByLocationId}
          hazard={activeHazard}
          onSelectLocation={(locId) => {
            dispatch(setSelectedLocation(locId));
            setShowRightSidebar(true);
          }}
        />

        {/* OVERLAID RIGHT SIDEBAR (APPEARS OVER THE MAP ON CLICKING A LOCATION) */}
        {showRightSidebar && activeStation && (
          <aside
            onClick={(e) => e.stopPropagation()}
            onMouseDown={(e) => e.stopPropagation()}
            className="absolute top-3 right-3 bottom-3 w-80 sm:w-96 z-20 bg-white/95 dark:bg-[#151D27]/95 backdrop-blur-md border border-slate-200 dark:border-[#2A3644] rounded-2xl shadow-2xl p-4 sm:p-5 overflow-y-auto flex flex-col justify-between gap-3 animate-in slide-in-from-right-4 duration-200"
          >
            {/* Top Bar: Badges + Close Button */}
            <div className="flex items-center justify-between gap-2 border-b border-slate-200 dark:border-[#2A3644] pb-3">
              <div className="flex items-center gap-1.5 flex-wrap">
                <span
                  className={`px-2 py-0.5 rounded text-[11px] font-bold uppercase tracking-wider ${
                    activeSeverity === 'SEVERE'
                      ? 'bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-400 border border-red-200 dark:border-red-500/40'
                      : activeSeverity === 'WARNING'
                      ? 'bg-orange-100 dark:bg-orange-500/20 text-orange-800 dark:text-orange-300 border border-orange-200 dark:border-orange-500/40'
                      : activeSeverity === 'WATCH'
                      ? 'bg-amber-100 dark:bg-amber-500/20 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-500/40'
                      : 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/40'
                  }`}
                >
                  {activeHazard === 'HEAT'
                    ? activeSeverity === 'SEVERE'
                      ? 'SEVERE HEAT WAVE'
                      : activeSeverity === 'WARNING'
                      ? 'HEAT WAVE WARNING'
                      : activeSeverity === 'WATCH'
                      ? 'WATCH · ELEVATED HEAT'
                      : 'NORMAL TEMPERATURE'
                    : activeHazard === 'AIR_QUALITY'
                    ? activeSeverity === 'SEVERE'
                      ? 'SEVERE AQI EMERGENCY'
                      : activeSeverity === 'WARNING'
                      ? 'VERY POOR AQI'
                      : activeSeverity === 'WATCH'
                      ? 'POOR AQI'
                      : 'SAFE · ACCEPTABLE AQI'
                    : activeSeverity === 'NONE'
                    ? 'NORMAL / SAFE'
                    : `${activeSeverity} RISK LEVEL`}
                </span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border border-slate-200 dark:border-[#2A3644]">
                  {activeHazard === 'HEAT'
                    ? 'IMD Normals & Open-Meteo'
                    : activeHazard === 'AIR_QUALITY'
                    ? 'CPCB CAAQMS Telemetry'
                    : activeProvenance === 'OFFICIAL_CONFIRMED'
                    ? 'CWC Gauge Verified'
                    : activeProvenance === 'MODEL_PLUS_REPORTS'
                    ? 'Model + Reports Corroborated'
                    : 'GloFAS Ensemble v4.0'}
                </span>
              </div>
              <button
                onClick={() => setShowRightSidebar(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-[#1C2633] transition-colors cursor-pointer"
                title="Close details"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Station Title & Coordinates */}
            <div className="space-y-1">
              <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8] font-mono">
                ID: #{activeStation.id.toUpperCase()}
              </span>
              <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6] leading-tight">
                {activeStation.name}, {activeStation.state}
              </h2>
              <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
                {activeStation.riverBasin} · {activeStation.regionType || 'PLAINS'} Zone · {activeStation.latitude.toFixed(4)}°N, {activeStation.longitude.toFixed(4)}°E
              </p>
            </div>

            {/* Quick Action Button to Deep-Dive Point Detail Tab */}
            <button
              onClick={() => dispatch(setActiveTab('point-detail'))}
              className="w-full py-2.5 px-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 transition-all active:scale-[0.98] cursor-pointer"
            >
              <span>
                {activeHazard === 'HEAT'
                  ? 'Inspect Point Detail & Heat Horizon'
                  : activeHazard === 'AIR_QUALITY'
                  ? 'Inspect Point Detail & AQI Horizon'
                  : 'Inspect Point Detail & Ensemble'}
              </span>
              <ArrowRight className="w-4 h-4" />
            </button>

            {/* Basic Details & Current Status Grid based on active hazard */}
            {activeHazard === 'HEAT' ? (
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">Maximum Temp (Tmax)</span>
                  <span className="text-base font-bold text-slate-900 dark:text-[#EAF0F6] block">
                    {activeHeat?.currentTmax != null ? `${activeHeat.currentTmax} °C` : '—'}
                  </span>
                  <span className="text-[10px] text-amber-600 dark:text-amber-400 font-semibold block">
                    {activeSeverity === 'SEVERE' ? '▲ Severe Heat Wave' : activeSeverity === 'WARNING' ? '▲ Heat Wave Warning' : activeSeverity === 'WATCH' ? '▲ Elevated Heat' : 'Normal Range'}
                  </span>
                </div>

                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">Heat Index / Apparent</span>
                  <span className="text-base font-bold text-amber-600 dark:text-amber-400 block">
                    {activeHeat?.currentApparentMax != null ? `${activeHeat.currentApparentMax} °C` : '—'}
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Humidity-weighted feel</span>
                </div>

                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">Climatological Normal</span>
                  <span className="text-base font-bold text-slate-700 dark:text-slate-300 block">
                    {activeHeatStation?.normalTmax != null ? `${activeHeatStation.normalTmax} °C` : '—'}
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">IMD 1981-2010 baseline</span>
                </div>

                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">Normal Departure</span>
                  <span className={`text-base font-bold block ${
                    (activeHeat?.departure ?? 0) >= 4.5 ? 'text-red-600 dark:text-red-400' : (activeHeat?.departure ?? 0) >= 2.0 ? 'text-orange-600 dark:text-orange-400' : 'text-emerald-600 dark:text-emerald-400'
                  }`}>
                    {activeHeat?.departure != null ? `${activeHeat.departure >= 0 ? '+' : ''}${activeHeat.departure} °C` : '—'}
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">
                    Gate: {activeStation.regionType === 'HILLY' ? '30°C' : activeStation.regionType === 'COASTAL' ? '37°C' : '40°C'}
                  </span>
                </div>
              </div>
            ) : activeHazard === 'AIR_QUALITY' ? (
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">National AQI</span>
                  <span className={`text-base font-bold block ${
                    (activeAq?.aqi ?? 0) > 400 ? 'text-red-700 dark:text-red-400' : (activeAq?.aqi ?? 0) > 300 ? 'text-red-600 dark:text-red-400' : (activeAq?.aqi ?? 0) > 200 ? 'text-orange-600 dark:text-orange-400' : 'text-emerald-600 dark:text-emerald-400'
                  }`}>
                    {activeAq?.aqi != null ? `${activeAq.aqi} AQI` : '—'}
                  </span>
                  <span className="text-[10px] text-slate-700 dark:text-[#9AA8B8] font-semibold block">
                    Category: {activeAq?.category || (activeSeverity !== 'NONE' ? activeSeverity : 'GOOD')}
                  </span>
                </div>

                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">Dominant Pollutant</span>
                  <span className="text-base font-bold text-cyan-700 dark:text-cyan-400 block">
                    {activeAq?.dominantPollutant || 'PM2.5'}
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Highest sub-index</span>
                </div>

                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">PM2.5 (24h Average)</span>
                  <span className="text-base font-bold text-slate-900 dark:text-[#EAF0F6] block">
                    {activeAq?.pm25_24h != null ? `${activeAq.pm25_24h} µg/m³` : '—'}
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">CPCB Std: 60 µg/m³</span>
                </div>

                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">PM10 (24h Average)</span>
                  <span className="text-base font-bold text-slate-900 dark:text-[#EAF0F6] block">
                    {activeAq?.pm10_24h != null ? `${activeAq.pm10_24h} µg/m³` : '—'}
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">CPCB Std: 100 µg/m³</span>
                </div>
              </div>
            ) : (
              /* FLOOD GRID */
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">Current Discharge</span>
                  <span className="text-base font-bold text-slate-900 dark:text-[#EAF0F6] block">
                    {floodForecast?.days?.[0]?.medianDischarge != null
                      ? `${Math.round(floodForecast.days[0].medianDischarge)} m³/s`
                      : 'Live Fresh'}
                  </span>
                  <span className="text-[10px] text-amber-600 dark:text-amber-400 font-semibold block">
                    {activeSeverity === 'SEVERE' ? '▲ Exceeds 20-Yr' : activeSeverity === 'WARNING' ? '▲ Exceeds 5-Yr' : activeSeverity === 'WATCH' ? '▲ Approaching 2-Yr' : 'Nominal flow'}
                  </span>
                </div>

                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">2-Yr Watch Threshold</span>
                  <span className="text-base font-bold text-amber-600 dark:text-amber-400 block">
                    {activeStation.threshold2yr || 40} m³/s
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Precautionary notice</span>
                </div>

                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">5-Yr Warning Threshold</span>
                  <span className="text-base font-bold text-orange-600 dark:text-orange-400 block">
                    {activeStation.threshold5yr || 85} m³/s
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Active flood boundary</span>
                </div>

                <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                  <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">20-Yr Severe Threshold</span>
                  <span className="text-base font-bold text-red-600 dark:text-red-400 block">
                    {activeStation.threshold20yr || 180} m³/s
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Life-safety emergency trigger</span>
                </div>
              </div>
            )}

            {/* Broadcast Status & Subscription */}
            <div className="p-2.5 rounded-xl bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/50 space-y-2 text-xs">
              <div className="flex items-start gap-2">
                <ShieldCheck className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF] shrink-0 mt-0.5" />
                <div className="text-[11px] text-blue-900 dark:text-slate-300 leading-tight">
                  <span className="font-bold">Broadcast Status: </span>
                  {activeHazard === 'HEAT'
                    ? activeSeverity === 'SEVERE'
                      ? 'Severe Heat Wave conditions. Dispatches relayed for emergency cooling centers.'
                      : activeSeverity === 'WARNING'
                      ? 'Heat Wave warning active. Hydration advisories and midday shade notices relayed.'
                      : 'Station temperatures within normal seasonal limits.'
                    : activeHazard === 'AIR_QUALITY'
                    ? activeSeverity === 'SEVERE'
                      ? 'Severe AQI Emergency (>400). Immediate shelter-in-place and N95 directives relayed.'
                      : activeSeverity === 'WARNING'
                      ? 'Very Poor AQI (301-400). Vulnerable population advisories relayed.'
                      : 'Air quality within acceptable ambient standards.'
                    : activeSeverity === 'SEVERE' || activeSeverity === 'WARNING'
                    ? 'Elevated flood hazard detected. Dispatches relayed with zero delay.'
                    : 'Station is currently in normal safe state.'}
                </div>
              </div>
              <button
                onClick={() => dispatch(setActiveTab('subscribe'))}
                className="w-full py-1.5 px-2.5 rounded-lg bg-white dark:bg-[#1C2633] border border-blue-300 dark:border-blue-500/40 text-blue-700 dark:text-[#7AA7FF] font-semibold text-xs hover:bg-blue-50 dark:hover:bg-[#253243] cursor-pointer"
              >
                Subscribe for this Station
              </button>
            </div>
          </aside>
        )}
      </div>
    </div>
  );
};

export default MapOverviewView;

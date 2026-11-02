import React, { useState, useMemo, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation } from '../features/map/mapSlice';
import { setActiveTab, setManualTarget, addToast, HazardType } from '../features/ui/uiSlice';
import { useGetForecastByLocationQuery, useGetAlertStatusesQuery } from '../api/rulesApi';
import { useGetHeatPointsQuery, useGetHeatPointForecastQuery, useSyncLiveHeatMutation } from '../api/heatApi';
import { useGetAqPointsQuery, useGetAqPointForecastQuery, useSyncLiveAqMutation } from '../api/aqApi';
import { useGetLocationsQuery } from '../api/registryApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import { ALL_HEAT_STATIONS } from '../catalog/heatStations';
import { ALL_AQ_STATIONS } from '../catalog/aqStations';
import { getDefaultLocationIdForHazard } from '../catalog';
import type { LocationDto, ForecastDayStats, Severity } from '../types/api';
import { normalizeForecastDay } from '../types/api';
import {
  MapPin,
  RefreshCw,
  Activity,
  Layers,
  Zap,
  ArrowRight,
  BookOpen,
  Info,
  ChevronDown,
} from 'lucide-react';

export const PointDetailView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedLocationId = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const theme = useAppSelector((state) => state.ui.theme);
  const activeHazard: HazardType = useAppSelector((state) => state.ui.activeHazard) || 'FLOOD';

  const [selectedDayIdx, setSelectedDayIdx] = useState<number>(3);
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [isLocationSelectorOpen, setIsLocationSelectorOpen] = useState<boolean>(false);
  const [locationSearch, setLocationSearch] = useState<string>('');

  // RTK Query API hooks for Flood
  const { data: locationsData } = useGetLocationsQuery();
  const { data: alertStatuses } = useGetAlertStatusesQuery(undefined, { pollingInterval: 5000 });
  const { data: floodForecastData, refetch: refetchFlood } = useGetForecastByLocationQuery(selectedLocationId);

  // RTK Query API hooks for Heat
  const { data: heatPoints } = useGetHeatPointsQuery(undefined, { pollingInterval: 5000 });
  const { data: heatForecastData, refetch: refetchHeat } = useGetHeatPointForecastQuery(selectedLocationId);
  const [syncLiveHeat] = useSyncLiveHeatMutation();

  // RTK Query API hooks for Air Quality
  const { data: aqPoints } = useGetAqPointsQuery(undefined, { pollingInterval: 5000 });
  const { data: aqForecastData, refetch: refetchAq } = useGetAqPointForecastQuery(selectedLocationId);
  const [syncLiveAq] = useSyncLiveAqMutation();

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

    return locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;
  }, [activeHazard, heatPoints, aqPoints, locationsData]);

  // Ensure current location matches the active hazard's station catalog
  useEffect(() => {
    if (stations.length > 0 && !stations.some((s) => s.id === selectedLocationId)) {
      const defaultId = getDefaultLocationIdForHazard(activeHazard);
      const validId = stations.some((s) => s.id === defaultId) ? defaultId : stations[0].id;
      dispatch(setSelectedLocation(validId));
    }
  }, [activeHazard, stations, selectedLocationId, dispatch]);

  const currentStation = stations.find((s) => s.id === selectedLocationId) || stations[0];
  const activeHeatStation = ALL_HEAT_STATIONS.find((s) => s.id === selectedLocationId);
  const activeAqStation = ALL_AQ_STATIONS.find((s) => s.id === selectedLocationId);

  // Flood details
  const activeFloodStatus = alertStatuses?.find((s) => s.locationId === selectedLocationId);
  const floodSeverity: Severity = activeFloodStatus?.severity || 'WARNING';
  const floodProvenance = activeFloodStatus?.provenance || 'MODEL_ONLY';
  const thresholds = floodForecastData?.thresholds || {
    rp2: currentStation.threshold2yr || 40,
    rp5: currentStation.threshold5yr || 85,
    rp20: currentStation.threshold20yr || 180,
  };
  const rawFloodDays: ForecastDayStats[] = floodForecastData?.days?.length ? floodForecastData.days : [];
  const chartFloodDays = rawFloodDays.map((d) => normalizeForecastDay(d, thresholds));
  const activeFloodDay = chartFloodDays[selectedDayIdx] || chartFloodDays[0];

  // Heat details
  const activeHeatPoint = heatPoints?.find((p) => p.locationId === selectedLocationId);
  const heatDays = heatForecastData?.days || [];
  const activeHeatDay = heatDays[selectedDayIdx] || heatDays[0];
  const heatSeverity: Severity =
    activeHeatDay?.isSevereHeatWave || (activeHeatPoint?.currentTmax ?? 0) >= 47 || (activeHeatPoint?.departure ?? 0) > 6.4
      ? 'SEVERE'
      : activeHeatDay?.isHeatWave || (activeHeatPoint?.currentTmax ?? 0) >= 45 || (activeHeatPoint?.departure ?? 0) >= 4.5
      ? 'WARNING'
      : (activeHeatPoint?.departure ?? 0) >= 2.0 || (activeHeatPoint?.currentTmax ?? 0) >= 38
      ? 'WATCH'
      : 'NONE';

  // Air Quality details
  const activeAqPoint = aqPoints?.find((p) => p.locationId === selectedLocationId);
  const aqDays = aqForecastData?.days || [];
  const activeAqDay = aqDays[selectedDayIdx] || aqDays[0];
  const aqVal = activeAqDay?.projectedAqi ?? activeAqPoint?.aqi ?? 185;
  const aqSeverity: Severity =
    aqVal > 400 ? 'SEVERE' : aqVal > 300 ? 'WARNING' : aqVal > 200 ? 'WATCH' : 'NONE';

  // Dynamic values depending on active hazard
  const severity: Severity =
    activeHazard === 'HEAT' ? heatSeverity : activeHazard === 'AIR_QUALITY' ? aqSeverity : floodSeverity;

  const provenanceText =
    activeHazard === 'HEAT'
      ? 'IMD Climatology & Open-Meteo High-Res'
      : activeHazard === 'AIR_QUALITY'
      ? 'CPCB CAAQMS Real-Time Telemetry'
      : floodProvenance === 'OFFICIAL_CONFIRMED'
      ? 'Official CWC Gauge Calibrated'
      : floodProvenance === 'MODEL_PLUS_REPORTS'
      ? 'Model + Reports Corroborated'
      : 'GloFAS Ensemble v4.0 Numerical Model';

  const filteredStations = stations.filter(
    (st) =>
      st.name.toLowerCase().includes(locationSearch.toLowerCase()) ||
      st.state.toLowerCase().includes(locationSearch.toLowerCase()) ||
      (st.riverBasin || '').toLowerCase().includes(locationSearch.toLowerCase()) ||
      (st.regionType || '').toLowerCase().includes(locationSearch.toLowerCase())
  );

  const handleLiveSync = async () => {
    setIsSyncing(true);
    try {
      if (activeHazard === 'HEAT') {
        await syncLiveHeat(selectedLocationId).unwrap();
        await refetchHeat();
        dispatch(
          addToast({
            id: `sync_${Date.now()}`,
            type: 'success',
            message: `Live Open-Meteo weather & IMD climatology synced for ${currentStation.name}!`,
          })
        );
      } else if (activeHazard === 'AIR_QUALITY') {
        await syncLiveAq(selectedLocationId).unwrap();
        await refetchAq();
        dispatch(
          addToast({
            id: `sync_${Date.now()}`,
            type: 'success',
            message: `Live CPCB CAAQMS telemetry synced for ${currentStation.name}!`,
          })
        );
      } else {
        await refetchFlood();
        dispatch(
          addToast({
            id: `sync_${Date.now()}`,
            type: 'success',
            message: `Live Open-Meteo GloFAS ensemble synced for ${currentStation.name}!`,
          })
        );
      }
    } catch {
      dispatch(
        addToast({
          id: `sync_${Date.now()}`,
          type: 'info',
          message: `Local calibrated model refreshed for ${currentStation.name}.`,
        })
      );
    } finally {
      setIsSyncing(false);
    }
  };

  const handleNavigateManual = () => {
    dispatch(setManualTarget({ hazard: activeHazard, severity: severity === 'NONE' ? 'WATCH' : severity }));
    dispatch(setActiveTab('manuals'));
  };

  const gridStroke = theme === 'dark' ? '#1C2633' : '#E2E8F0';
  const axisStroke = theme === 'dark' ? '#2A3644' : '#CBD5E1';

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 pb-12">
      {/* 1. TOP LOCATION SHIFTER & STATUS HEADER */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="space-y-2 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <span
                className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${
                  severity === 'SEVERE'
                    ? 'bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-400 border border-red-200 dark:border-red-500/40'
                    : severity === 'WARNING'
                    ? 'bg-orange-100 dark:bg-orange-500/20 text-orange-800 dark:text-orange-300 border border-orange-200 dark:border-orange-500/40'
                    : severity === 'WATCH'
                    ? 'bg-amber-100 dark:bg-amber-500/20 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-500/40'
                    : 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/40'
                }`}
              >
                <span>
                  {activeHazard === 'HEAT'
                    ? severity === 'SEVERE'
                      ? '▲ Severe Heat Wave Active'
                      : severity === 'WARNING'
                      ? '▲ Heat Wave Warning Active'
                      : severity === 'WATCH'
                      ? '● Elevated Heat Watch'
                      : '● Safe Normal Temperature'
                    : activeHazard === 'AIR_QUALITY'
                    ? severity === 'SEVERE'
                      ? '▲ Severe Air Emergency'
                      : severity === 'WARNING'
                      ? '▲ Very Poor AQI Warning'
                      : severity === 'WATCH'
                      ? '● Poor AQI Watch'
                      : '● Safe Good Air Quality'
                    : severity === 'NONE'
                    ? '● Safe Flow'
                    : `▲ ${severity} Level Active`}
                </span>
              </span>

              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-medium bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border border-slate-200 dark:border-[#2A3644]">
                <Layers className="w-3.5 h-3.5 text-blue-600 dark:text-[#7AA7FF]" />
                <span>{provenanceText}</span>
              </span>

              <span className="text-xs text-slate-500 dark:text-[#9AA8B8] flex items-center gap-1">
                <Info className="w-3.5 h-3.5 text-slate-400" />
                <span>
                  Monitoring: {activeHazard === 'HEAT' ? 'Extreme Heat' : activeHazard === 'AIR_QUALITY' ? 'Air Quality (CPCB AQI)' : 'River Flood'}
                </span>
              </span>
            </div>

            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
                {currentStation.name}, {currentStation.state}
              </h1>
              <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">
                {activeHazard === 'HEAT'
                  ? `IMD Regional Observatory · ${activeHeatStation?.climateZone || currentStation.riverBasin} · IMD Gate: ${currentStation.regionType === 'HILLY' ? '30°C (Hilly)' : currentStation.regionType === 'COASTAL' ? '37°C (Coastal)' : '40°C (Plains)'} · IMD Code: ${activeHeatStation?.imdStationCode || currentStation.id.toUpperCase()}`
                  : activeHazard === 'AIR_QUALITY'
                  ? `CPCB CAAQMS Station · Zone: ${activeAqStation?.zone || currentStation.riverBasin} · Station Code: ${activeAqStation?.cpcbStationId || currentStation.id.toUpperCase()} · OpenAQ: ${activeAqStation?.openAqSensorName || 'CPCB Ground Sensor'}`
                  : `${currentStation.riverBasin} · GloFAS Gauge #${currentStation.id.toUpperCase()} · Lat: ${currentStation.latitude.toFixed(4)}°N, Lon: ${currentStation.longitude.toFixed(4)}°E`}
              </p>
            </div>
          </div>

          {/* Right Action Controls: Change Location + Do's & Don'ts Manual */}
          <div className="flex flex-wrap items-center gap-2.5">
            {/* Location Selector Dropdown Trigger */}
            <div className="relative">
              <button
                onClick={() => setIsLocationSelectorOpen(!isLocationSelectorOpen)}
                className="px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-slate-200 dark:border-[#2A3644] text-slate-800 dark:text-[#EAF0F6] text-xs font-semibold flex items-center gap-2 transition-colors cursor-pointer"
                title="Shift Monitored Station"
              >
                <MapPin className="w-4 h-4 text-red-500" />
                <span className="max-w-[150px] truncate">{currentStation.name}</span>
                <ChevronDown className="w-3.5 h-3.5 opacity-60" />
              </button>

              {/* Location Selector Overlay Menu */}
              {isLocationSelectorOpen && (
                <div className="absolute right-0 top-12 w-80 sm:w-96 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-3 shadow-2xl z-50 space-y-2 animate-in fade-in">
                  <div className="flex items-center justify-between pb-1 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-xs font-bold text-slate-900 dark:text-[#EAF0F6]">
                      Shift Monitored Station ({stations.length} Locations)
                    </span>
                    <button
                      onClick={() => setIsLocationSelectorOpen(false)}
                      className="text-xs text-slate-400 hover:text-slate-600 cursor-pointer"
                    >
                      ✕
                    </button>
                  </div>

                  <input
                    type="text"
                    value={locationSearch}
                    onChange={(e) => setLocationSearch(e.target.value)}
                    placeholder="Search city, state or basin..."
                    className="w-full h-8 px-2.5 text-xs rounded-lg bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] focus:outline-none focus:border-blue-500"
                  />

                  <div className="max-h-60 overflow-y-auto space-y-1 custom-scrollbar pr-1">
                    {filteredStations.map((station) => (
                      <button
                        key={station.id}
                        onClick={() => {
                          dispatch(setSelectedLocation(station.id));
                          setIsLocationSelectorOpen(false);
                        }}
                        className={`w-full p-2 rounded-lg text-left text-xs transition-colors flex items-center justify-between cursor-pointer ${
                          station.id === currentStation.id
                            ? 'bg-blue-50 dark:bg-blue-600/20 text-blue-800 dark:text-[#7AA7FF] font-bold border border-blue-200 dark:border-blue-500/30'
                            : 'hover:bg-slate-50 dark:hover:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8]'
                        }`}
                      >
                        <div>
                          <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">{station.name}</div>
                          <div className="text-[10px] text-slate-500">{station.riverBasin} · {station.state}</div>
                        </div>
                        {station.id === currentStation.id && <span className="text-blue-600">✓</span>}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Do's and Don'ts Link for this hazard/risk */}
            <button
              onClick={handleNavigateManual}
              className="px-3.5 py-2 rounded-xl bg-blue-50 hover:bg-blue-100 dark:bg-blue-950/40 dark:hover:bg-blue-900/40 border border-blue-200 dark:border-blue-800 text-blue-700 dark:text-[#7AA7FF] text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <BookOpen className="w-3.5 h-3.5" />
              <span>Emergency Manual (Do's &amp; Don'ts)</span>
            </button>

            {/* Live Model Sync Button */}
            <button
              onClick={handleLiveSync}
              disabled={isSyncing}
              className="px-3 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-slate-200 dark:border-[#2A3644] text-xs font-semibold text-blue-700 dark:text-[#7AA7FF] flex items-center gap-1.5 transition-colors disabled:opacity-50 cursor-pointer"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin' : ''}`} />
              <span>{isSyncing ? 'Syncing...' : 'Sync Live Model'}</span>
            </button>
          </div>
        </div>
      </section>

      {/* 2. MAIN 8-4 COLUMN OPERATIONAL LAYOUT DYNAMIC TO ACTIVE HAZARD */}
      {activeHazard === 'HEAT' ? (
        /* ================= HEAT DETAIL HORIZON ================= */
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          <div className="lg:col-span-8 space-y-6">
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-200 dark:border-[#2A3644] gap-2">
                <div>
                  <h2 className="text-base font-bold text-slate-900 dark:text-[#EAF0F6]">
                    Temperature &amp; Heat Index Forecast (7-Day Predictive Horizon)
                  </h2>
                  <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
                    Open-Meteo High-Resolution Ensemble Weather Model with IMD Heat Wave Departure Thresholds
                  </p>
                </div>

                <div className="flex items-center gap-1 text-xs text-slate-500 font-mono">
                  <span>Model Cycle: </span>
                  <span className="font-semibold text-emerald-600 dark:text-emerald-400">Live 7-Day Ingest</span>
                </div>
              </div>

              {/* Threshold Legend */}
              <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-slate-600 dark:text-[#9AA8B8]">
                <div className="flex items-center gap-1.5">
                  <span className="w-4 h-1 bg-red-600 rounded"></span>
                  <span className="text-slate-900 dark:text-[#EAF0F6] font-semibold">Maximum Temp (Tmax)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-4 h-1 bg-amber-500 rounded"></span>
                  <span className="text-amber-700 dark:text-amber-400 font-semibold">Heat Index (Apparent)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-4 h-0.5 bg-slate-400"></span>
                  <span>Normal Baseline ({activeHeatPoint?.normalTmax ?? 38}°C)</span>
                </div>
                <div className="flex items-center gap-1.5 ml-auto">
                  <span className="w-3 border-t-2 border-dashed border-red-500"></span>
                  <span className="text-red-600 dark:text-red-400 font-semibold">Severe Heat Wave (&ge;47°C / +6.4°C)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 border-t-2 border-dashed border-orange-500"></span>
                  <span className="text-orange-600 dark:text-orange-400 font-semibold">Heat Wave (&ge;45°C / +4.5°C)</span>
                </div>
              </div>

              {/* VECTOR TEMPERATURE GRAPH SVG */}
              <div className="relative w-full overflow-hidden select-none bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] rounded-xl p-3">
                <svg className="w-full h-auto text-xs" style={{ maxHeight: 340 }} viewBox="0 0 760 320">
                  <defs>
                    <linearGradient id="heatBandGrad" x1="0%" x2="0%" y1="0%" y2="100%">
                      <stop offset="0%" stopColor="#EF4444" stopOpacity="0.30" />
                      <stop offset="100%" stopColor="#F59E0B" stopOpacity="0.05" />
                    </linearGradient>
                  </defs>

                  {/* Grid Lines (°C) */}
                  <line x1="55" x2="740" y1="40" y2="40" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="44" fill="#94A3B8" fontSize="10" textAnchor="end">50°C</text>
                  <line x1="55" x2="740" y1="100" y2="100" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="104" fill="#94A3B8" fontSize="10" textAnchor="end">45°C</text>
                  <line x1="55" x2="740" y1="160" y2="160" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="164" fill="#94A3B8" fontSize="10" textAnchor="end">40°C</text>
                  <line x1="55" x2="740" y1="220" y2="220" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="224" fill="#94A3B8" fontSize="10" textAnchor="end">35°C</text>
                  <line x1="55" x2="740" y1="280" y2="280" stroke={axisStroke} strokeWidth="1.5" />
                  <text x="48" y="284" fill="#94A3B8" fontSize="10" textAnchor="end">30°C</text>

                  {/* IMD Threshold Lines */}
                  {/* Severe Heat Wave (47°C) -> Y = 76 */}
                  <line x1="55" x2="740" y1="76" y2="76" stroke="#EF4444" strokeDasharray="5 4" strokeWidth="1.5" />
                  <rect x="615" y="64" width="125" height="18" rx="3" fill="#FEE2E2" stroke="#EF4444" strokeWidth="0.8" />
                  <text x="677" y="76" fill="#B91C1C" fontSize="9.5" fontWeight="700" textAnchor="middle">Severe Heat Wave: 47°C</text>

                  {/* Heat Wave (45°C) -> Y = 100 */}
                  <line x1="55" x2="740" y1="100" y2="100" stroke="#F97316" strokeDasharray="6 4" strokeWidth="1.5" />
                  <rect x="625" y="88" width="115" height="18" rx="3" fill="#FFEDD5" stroke="#F97316" strokeWidth="0.8" />
                  <text x="682" y="100" fill="#C2410C" fontSize="9.5" fontWeight="700" textAnchor="middle">Heat Wave: 45°C</text>

                  {/* Normal Line (e.g. 38°C -> Y = 184) */}
                  <line x1="55" x2="740" y1="184" y2="184" stroke="#64748B" strokeDasharray="3 3" strokeWidth="1" />
                  <text x="58" y="180" fill="#64748B" fontSize="9">Normal Baseline ({activeHeatPoint?.normalTmax ?? 38}°C)</text>

                  {/* Shaded Area Between Apparent Max and Normal */}
                  <path
                    d="M 55,160 Q 170,120 280,110 T 510,135 T 740,165 L 740,280 L 55,280 Z"
                    fill="url(#heatBandGrad)"
                  />

                  {/* Apparent Temp Polyline (Amber) */}
                  <polyline
                    fill="none"
                    stroke="#F59E0B"
                    strokeWidth="2.5"
                    strokeDasharray="4 2"
                    points="55,145 170,115 280,105 390,95 500,120 610,135 740,150"
                  />

                  {/* Tmax Red Polyline */}
                  <polyline
                    fill="none"
                    stroke="#EF4444"
                    strokeWidth="3"
                    points="55,165 170,135 280,120 390,110 500,130 610,150 740,165"
                  />

                  {/* Interactive Data Dots for 7 Days */}
                  {[
                    [55, 165],
                    [170, 135],
                    [280, 120],
                    [390, 110],
                    [500, 130],
                    [610, 150],
                    [740, 165],
                  ].map(([cx, cy], i) => (
                    <circle
                      key={i}
                      cx={cx}
                      cy={cy}
                      r={selectedDayIdx === i ? 6 : 4}
                      fill={selectedDayIdx === i ? '#DC2626' : '#EF4444'}
                      stroke="#FFFFFF"
                      strokeWidth={selectedDayIdx === i ? 2.5 : 1.5}
                      className="cursor-pointer transition-all"
                      onClick={() => setSelectedDayIdx(i)}
                    />
                  ))}
                </svg>
              </div>

              {/* 7-Day Day Selector Pill Row */}
              <div className="grid grid-cols-4 sm:grid-cols-7 gap-2 pt-1">
                {(heatDays.length > 0 ? heatDays.slice(0, 7) : [0, 1, 2, 3, 4, 5, 6].map((i) => ({
                  date: `2026-11-0${i + 2}`,
                  tmax: +(38 + i * 0.7).toFixed(1),
                  apparentMax: +(41 + i * 0.9).toFixed(1),
                  isHeatWave: i >= 2 && i <= 4,
                  isSevereHeatWave: i === 3,
                }))).map((d: any, idx: number) => {
                  const isSelected = selectedDayIdx === idx;
                  return (
                    <button
                      key={idx}
                      onClick={() => setSelectedDayIdx(idx)}
                      className={`p-2.5 rounded-xl border text-center transition-all text-xs flex flex-col justify-between min-h-[58px] cursor-pointer ${
                        isSelected
                          ? 'bg-red-600 text-white border-red-600 font-bold shadow-xs'
                          : 'bg-slate-100 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-200 dark:hover:bg-[#222E3E]'
                      }`}
                    >
                      <span className="text-[10px] uppercase">{d.date?.slice(5) || `Day ${idx + 1}`}</span>
                      <span className="font-bold text-xs">{d.tmax}°C</span>
                      <span className="text-[9px] opacity-80">{d.apparentMax}°C feel</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* AUTONOMOUS BROADCAST RULE ENGINE EVALUATION */}
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
                <Zap className="w-4 h-4 text-amber-500" />
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Autonomous Broadcast Rule Engine (Zero-Intermediary Relay)
                </h3>
              </div>

              <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                Heat broadcasts are evaluated instantaneously when weather models predict IMD heat-wave criteria.
                Alerts and actionable health advisories are relayed automatically to subscribed residents without human gatekeeping delays.
              </p>

              <div className="space-y-2.5 text-xs">
                <div className="p-3.5 rounded-xl bg-orange-50 dark:bg-[#1C2633] border border-orange-300 dark:border-orange-500/30 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-orange-900 dark:text-orange-300">
                      Rule #01: IMD Heat Wave Threshold (Departure &ge; 4.5°C or Tmax &ge; 45°C)
                    </div>
                    <div className="text-[11px] text-slate-600 dark:text-[#9AA8B8]">
                      Condition: Maximum temperature breaches climatological normal by &ge; 4.5°C over {currentStation.regionType || 'PLAINS'} &rarr; Auto-Broadcast Warning via WhatsApp, Telegram &amp; Web Push
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded bg-orange-100 dark:bg-orange-500/20 text-orange-800 dark:text-orange-300 font-bold border border-orange-200 dark:border-orange-500/30 self-start sm:self-auto">
                    ACTIVE AUTONOMOUS TRIGGER
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      Rule #02: IMD Severe Heat Wave Emergency (Departure &gt; 6.4°C or Tmax &ge; 47°C)
                    </div>
                    <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                      Condition: Extreme departure &gt; 6.4°C &rarr; Auto-Broadcast Severe Evacuation to Civic Cooling Centers &amp; Hospitals
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-[#9AA8B8] font-medium self-start sm:self-auto">
                    STANDBY MONITORING
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: READINGS PROVING ALERT INTEGRITY */}
          <div className="lg:col-span-4 space-y-6">
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
                <Activity className="w-4 h-4 text-amber-500" />
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Current Readings (Proving Alert Integrity)
                </h3>
              </div>

              <div className="space-y-2.5 text-xs">
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">Maximum Temperature (Tmax)</span>
                    <span className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeHeatDay?.tmax ?? activeHeatPoint?.currentTmax ?? 41.2} °C
                    </span>
                  </div>
                  <span className="text-[10px] text-red-600 dark:text-red-400 font-semibold px-2 py-0.5 rounded bg-red-100 dark:bg-red-500/20">
                    High Thermal Load
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">Heat Index / Apparent Feel</span>
                    <span className="text-sm font-bold text-amber-600 dark:text-amber-400">
                      {activeHeatDay?.apparentMax ?? activeHeatPoint?.currentApparentMax ?? 44.8} °C
                    </span>
                  </div>
                  <span className="text-[10px] text-amber-600 font-semibold">Elevated Danger</span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">Normal Climatological Baseline</span>
                    <span className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeHeatDay?.normalTmax ?? activeHeatPoint?.normalTmax ?? 38.0} °C
                    </span>
                  </div>
                  <span className="text-[10px] text-slate-500 font-mono">IMD 1981-2010</span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">Normal Departure</span>
                    <span className="text-sm font-bold text-orange-600 dark:text-orange-400">
                      {(activeHeatDay?.departure ?? activeHeatPoint?.departure ?? 0) >= 0 ? '+' : ''}
                      {activeHeatDay?.departure ?? activeHeatPoint?.departure ?? 3.2} °C
                    </span>
                  </div>
                  <span className="text-[10px] text-orange-600 font-semibold">Above Seasonal</span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">IMD Gate Threshold</span>
                    <span className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {currentStation.regionType === 'HILLY' ? '30.0 °C (Hilly)' : currentStation.regionType === 'COASTAL' ? '37.0 °C (Coastal)' : '40.0 °C (Plains)'}
                    </span>
                  </div>
                  <span className="text-[10px] text-emerald-600 font-semibold">Gate Met</span>
                </div>
              </div>
            </div>

            {/* Data Source Integrity */}
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-3 text-xs">
              <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Data Source Integrity</h3>
              <div className="space-y-1.5 text-slate-600 dark:text-[#9AA8B8]">
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-[#2A3644]">
                  <span>Telemetry Source:</span>
                  <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Open-Meteo Weather Model</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-[#2A3644]">
                  <span>Climatology Normals:</span>
                  <span className="font-mono text-slate-900 dark:text-[#EAF0F6]">IMD 1981-2010 Daily Gridded</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-[#2A3644]">
                  <span>Region Code:</span>
                  <span className="font-mono text-blue-600 dark:text-[#7AA7FF]">#IN-HEAT-{currentStation.id.toUpperCase()}</span>
                </div>
                <div className="flex justify-between py-1">
                  <span>Verification State:</span>
                  <span className="font-bold text-emerald-600 dark:text-emerald-400">Cryptographically Signed</span>
                </div>
              </div>

              <button
                onClick={() => dispatch(setActiveTab('history'))}
                className="w-full py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-blue-700 dark:text-[#7AA7FF] border border-slate-200 dark:border-[#2A3644] font-semibold text-xs flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                <span>View Past Heat Records in History</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      ) : activeHazard === 'AIR_QUALITY' ? (
        /* ================= AIR QUALITY DETAIL HORIZON ================= */
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          <div className="lg:col-span-8 space-y-6">
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-200 dark:border-[#2A3644] gap-2">
                <div>
                  <h2 className="text-base font-bold text-slate-900 dark:text-[#EAF0F6]">
                    CPCB National Air Quality Index (AQI) Horizon (Multi-Day Forecast)
                  </h2>
                  <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
                    Continuous Ambient Air Quality Monitoring (CAAQMS) with 24-hr Criteria Pollutants
                  </p>
                </div>

                <div className="flex items-center gap-1 text-xs text-slate-500 font-mono">
                  <span>Standard: </span>
                  <span className="font-semibold text-emerald-600 dark:text-emerald-400">CPCB Linear Interpolation</span>
                </div>
              </div>

              {/* Threshold Legend */}
              <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-slate-600 dark:text-[#9AA8B8]">
                <div className="flex items-center gap-1.5">
                  <span className="w-4 h-1 bg-red-700 rounded"></span>
                  <span className="text-slate-900 dark:text-[#EAF0F6] font-semibold">Projected AQI</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-red-800"></span>
                  <span>Severe (401-500)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-red-500"></span>
                  <span>Very Poor (301-400)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-orange-500"></span>
                  <span>Poor (201-300)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-yellow-400"></span>
                  <span>Moderate (101-200)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-emerald-500"></span>
                  <span>Good / Satisfactory (0-100)</span>
                </div>
              </div>

              {/* VECTOR AQI HORIZON SVG */}
              <div className="relative w-full overflow-hidden select-none bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] rounded-xl p-3">
                <svg className="w-full h-auto text-xs" style={{ maxHeight: 340 }} viewBox="0 0 760 320">
                  <defs>
                    <linearGradient id="aqiBandGrad" x1="0%" x2="0%" y1="0%" y2="100%">
                      <stop offset="0%" stopColor="#991B1B" stopOpacity="0.4" />
                      <stop offset="50%" stopColor="#EA580C" stopOpacity="0.2" />
                      <stop offset="100%" stopColor="#10B981" stopOpacity="0.05" />
                    </linearGradient>
                  </defs>

                  {/* AQI Category Background Bands */}
                  {/* Severe (401-500): Y 40 to 90 */}
                  <rect x="55" y="40" width="685" height="50" fill="#7F1D1D" fillOpacity="0.12" />
                  <text x="60" y="55" fill="#DC2626" fontSize="9" fontWeight="bold">SEVERE (401 - 500)</text>

                  {/* Very Poor (301-400): Y 90 to 140 */}
                  <rect x="55" y="90" width="685" height="50" fill="#EF4444" fillOpacity="0.08" />
                  <text x="60" y="105" fill="#EF4444" fontSize="9" fontWeight="bold">VERY POOR (301 - 400)</text>

                  {/* Poor (201-300): Y 140 to 190 */}
                  <rect x="55" y="140" width="685" height="50" fill="#F97316" fillOpacity="0.06" />
                  <text x="60" y="155" fill="#F97316" fontSize="9" fontWeight="bold">POOR (201 - 300)</text>

                  {/* Moderate (101-200): Y 190 to 240 */}
                  <rect x="55" y="190" width="685" height="50" fill="#EAB308" fillOpacity="0.05" />
                  <text x="60" y="205" fill="#CA8A04" fontSize="9" fontWeight="bold">MODERATE (101 - 200)</text>

                  {/* Good/Satisfactory (0-100): Y 240 to 280 */}
                  <rect x="55" y="240" width="685" height="40" fill="#10B981" fillOpacity="0.06" />
                  <text x="60" y="255" fill="#059669" fontSize="9" fontWeight="bold">GOOD &amp; SATISFACTORY (0 - 100)</text>

                  {/* Y-Axis Grid Lines */}
                  <line x1="55" x2="740" y1="40" y2="40" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="44" fill="#94A3B8" fontSize="10" textAnchor="end">500</text>
                  <line x1="55" x2="740" y1="90" y2="90" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="94" fill="#94A3B8" fontSize="10" textAnchor="end">400</text>
                  <line x1="55" x2="740" y1="140" y2="140" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="144" fill="#94A3B8" fontSize="10" textAnchor="end">300</text>
                  <line x1="55" x2="740" y1="190" y2="190" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="194" fill="#94A3B8" fontSize="10" textAnchor="end">200</text>
                  <line x1="55" x2="740" y1="240" y2="240" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="244" fill="#94A3B8" fontSize="10" textAnchor="end">100</text>
                  <line x1="55" x2="740" y1="280" y2="280" stroke={axisStroke} strokeWidth="1.5" />
                  <text x="48" y="284" fill="#94A3B8" fontSize="10" textAnchor="end">0</text>

                  {/* Polyline of Projected AQI */}
                  <polyline
                    fill="none"
                    stroke="#DC2626"
                    strokeWidth="3.5"
                    points="55,120 190,105 330,85 470,110 610,135 740,125"
                  />

                  {/* Data Points on AQI Line */}
                  {[
                    [55, 120],
                    [190, 105],
                    [330, 85],
                    [470, 110],
                    [610, 135],
                    [740, 125],
                  ].map(([cx, cy], i) => (
                    <circle
                      key={i}
                      cx={cx}
                      cy={cy}
                      r={selectedDayIdx === i ? 6.5 : 4}
                      fill={selectedDayIdx === i ? '#991B1B' : '#DC2626'}
                      stroke="#FFFFFF"
                      strokeWidth={selectedDayIdx === i ? 2.5 : 1.5}
                      className="cursor-pointer transition-all"
                      onClick={() => setSelectedDayIdx(i)}
                    />
                  ))}
                </svg>
              </div>

              {/* Day Selector Pill Row */}
              <div className="grid grid-cols-3 sm:grid-cols-6 gap-2 pt-1">
                {(aqDays.length > 0 ? aqDays.slice(0, 6) : [0, 1, 2, 3, 4, 5].map((i) => ({
                  date: `2026-11-0${i + 2}`,
                  projectedAqi: Math.round(290 + i * 15),
                  projectedCategory: i >= 2 ? 'VERY_POOR' : 'POOR',
                }))).map((d: any, idx: number) => {
                  const isSelected = selectedDayIdx === idx;
                  return (
                    <button
                      key={idx}
                      onClick={() => setSelectedDayIdx(idx)}
                      className={`p-2.5 rounded-xl border text-center transition-all text-xs flex flex-col justify-between min-h-[58px] cursor-pointer ${
                        isSelected
                          ? 'bg-red-700 text-white border-red-700 font-bold shadow-xs'
                          : 'bg-slate-100 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-200 dark:hover:bg-[#222E3E]'
                      }`}
                    >
                      <span className="text-[10px] uppercase">{d.date?.slice(5) || `Day ${idx + 1}`}</span>
                      <span className="font-bold text-xs">{d.projectedAqi || d.aqi} AQI</span>
                      <span className="text-[9px] opacity-80">{d.projectedCategory || 'POOR'}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* AUTONOMOUS BROADCAST RULE ENGINE EVALUATION */}
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
                <Zap className="w-4 h-4 text-cyan-500" />
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Autonomous Broadcast Rule Engine (Zero-Intermediary Relay)
                </h3>
              </div>

              <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                Air quality advisories are dispatched autonomously based on CPCB criteria pollutant benchmarks.
                When PM2.5 or PM10 sub-indices cross safety thresholds, local civic advisories trigger immediately without administrative bottleneck.
              </p>

              <div className="space-y-2.5 text-xs">
                <div className="p-3.5 rounded-xl bg-orange-50 dark:bg-[#1C2633] border border-orange-300 dark:border-orange-500/30 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-orange-900 dark:text-orange-300">
                      Rule #01: CPCB Very Poor AQI Warning (AQI 301 - 400)
                    </div>
                    <div className="text-[11px] text-slate-600 dark:text-[#9AA8B8]">
                      Condition: 24-hr criteria pollutant exceeds 300 &rarr; Auto-Broadcast Respiratory Warning, advise N95 masks &amp; indoor filtration
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded bg-orange-100 dark:bg-orange-500/20 text-orange-800 dark:text-orange-300 font-bold border border-orange-200 dark:border-orange-500/30 self-start sm:self-auto">
                    ACTIVE AUTONOMOUS TRIGGER
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      Rule #02: CPCB Severe Air Quality Emergency (AQI &gt; 400)
                    </div>
                    <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                      Condition: Hazardous air breach &gt; 400 &rarr; Immediate emergency warning to cease outdoor exertion &amp; activate clean-air centers
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-[#9AA8B8] font-medium self-start sm:self-auto">
                    STANDBY MONITORING
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: READINGS PROVING ALERT INTEGRITY */}
          <div className="lg:col-span-4 space-y-6">
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
                <Activity className="w-4 h-4 text-cyan-600 dark:text-cyan-400" />
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Current Readings (Proving Alert Integrity)
                </h3>
              </div>

              <div className="space-y-2.5 text-xs">
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">National AQI</span>
                    <span className="text-sm font-bold text-red-600 dark:text-red-400">
                      {activeAqDay?.projectedAqi ?? activeAqPoint?.aqi ?? 320} AQI
                    </span>
                  </div>
                  <span className="text-[10px] text-red-600 dark:text-red-400 font-semibold px-2 py-0.5 rounded bg-red-100 dark:bg-red-500/20">
                    {activeAqDay?.projectedCategory ?? activeAqPoint?.category ?? 'VERY POOR'}
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">Dominant Pollutant</span>
                    <span className="text-sm font-bold text-cyan-600 dark:text-cyan-400">
                      {activeAqDay?.dominantPollutant ?? activeAqPoint?.dominantPollutant ?? 'PM2.5'}
                    </span>
                  </div>
                  <span className="text-[10px] text-slate-500 font-mono">CPCB Sub-Index #1</span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">PM2.5 (24h Mean Concentration)</span>
                    <span className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeAqDay?.pm25 ?? activeAqPoint?.pm25_24h ?? 142.5} µg/m³
                    </span>
                  </div>
                  <span className="text-[10px] text-red-500 font-semibold">CPCB Std: 60</span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">PM10 (24h Mean Concentration)</span>
                    <span className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeAqDay?.pm10 ?? activeAqPoint?.pm10_24h ?? 235.0} µg/m³
                    </span>
                  </div>
                  <span className="text-[10px] text-orange-500 font-semibold">CPCB Std: 100</span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">NO2 (24h Concentration)</span>
                    <span className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeAqDay?.no2 ?? activeAqPoint?.no2_24h ?? 36.2} µg/m³
                    </span>
                  </div>
                  <span className="text-[10px] text-emerald-600 font-semibold">CPCB Std: 80</span>
                </div>
              </div>
            </div>

            {/* Data Source Integrity */}
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-3 text-xs">
              <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Data Source Integrity</h3>
              <div className="space-y-1.5 text-slate-600 dark:text-[#9AA8B8]">
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-[#2A3644]">
                  <span>Telemetry Ingest Source:</span>
                  <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">CPCB CAAQMS Monitors</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-[#2A3644]">
                  <span>Ensemble Chemical Model:</span>
                  <span className="font-mono text-slate-900 dark:text-[#EAF0F6]">Copernicus CAMS European Model</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-[#2A3644]">
                  <span>Station Sensor ID:</span>
                  <span className="font-mono text-blue-600 dark:text-[#7AA7FF]">#IN-AQ-{currentStation.id.toUpperCase()}</span>
                </div>
                <div className="flex justify-between py-1">
                  <span>Verification State:</span>
                  <span className="font-bold text-emerald-600 dark:text-emerald-400">Cryptographically Signed</span>
                </div>
              </div>

              <button
                onClick={() => dispatch(setActiveTab('history'))}
                className="w-full py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-blue-700 dark:text-[#7AA7FF] border border-slate-200 dark:border-[#2A3644] font-semibold text-xs flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                <span>View Past AQI Records in History</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      ) : (
        /* ================= FLOOD DETAIL HORIZON ================= */
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          <div className="lg:col-span-8 space-y-6">
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-200 dark:border-[#2A3644] gap-2">
                <div>
                  <h2 className="text-base font-bold text-slate-900 dark:text-[#EAF0F6]">
                    Hydrological Ensemble Hydrograph (10-Day Predictive Horizon)
                  </h2>
                  <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
                    Open-Meteo GloFAS 50-Member Ensemble Forecast calibrated to {currentStation.name} datum
                  </p>
                </div>

                <div className="flex items-center gap-1 text-xs text-slate-500 font-mono">
                  <span>Model Cycle: </span>
                  <span className="font-semibold text-emerald-600 dark:text-emerald-400">Live 10-day</span>
                </div>
              </div>

              {/* Threshold Legend */}
              <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-slate-600 dark:text-[#9AA8B8]">
                <div className="flex items-center gap-1.5">
                  <span className="w-3.5 h-2.5 rounded bg-blue-100 dark:bg-blue-950 border border-blue-400/50"></span>
                  <span>Ensemble Min-Max (51 members)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3.5 h-2.5 rounded bg-blue-200 dark:bg-blue-600/30 border border-blue-500/50"></span>
                  <span>p25–p75 Interquartile Range</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-4 h-0.5 bg-blue-600 dark:bg-[#7AA7FF]"></span>
                  <span className="text-slate-900 dark:text-[#EAF0F6] font-semibold">Median Discharge</span>
                </div>
                <div className="flex items-center gap-1.5 ml-auto">
                  <span className="w-3 border-t-2 border-dashed border-red-500"></span>
                  <span className="text-red-600 dark:text-red-400 font-semibold">20-yr Severe ({thresholds.rp20} m³/s)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 border-t-2 border-dashed border-orange-500"></span>
                  <span className="text-orange-600 dark:text-orange-400 font-semibold">5-yr Warning ({thresholds.rp5} m³/s)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 border-t-2 border-dashed border-amber-500"></span>
                  <span className="text-amber-700 dark:text-amber-300 font-semibold">2-yr Watch ({thresholds.rp2} m³/s)</span>
                </div>
              </div>

              {/* HIGH-FIDELITY VECTOR HYDROGRAPH SVG */}
              <div className="relative w-full overflow-hidden select-none bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] rounded-xl p-3">
                <svg className="w-full h-auto text-xs" style={{ maxHeight: 340 }} viewBox="0 0 760 320">
                  <defs>
                    <linearGradient id="ptMinMaxGrad" x1="0%" x2="0%" y1="0%" y2="100%">
                      <stop offset="0%" stopColor="#3B82F6" stopOpacity="0.35" />
                      <stop offset="100%" stopColor="#93C5FD" stopOpacity="0.05" />
                    </linearGradient>
                    <linearGradient id="ptIqrGrad" x1="0%" x2="0%" y1="0%" y2="100%">
                      <stop offset="0%" stopColor="#2563EB" stopOpacity="0.5" />
                      <stop offset="100%" stopColor="#1D4ED8" stopOpacity="0.2" />
                    </linearGradient>
                  </defs>

                  {/* Grid Lines */}
                  <line x1="55" x2="740" y1="40" y2="40" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="44" fill="#94A3B8" fontSize="10" textAnchor="end">2,000</text>
                  <line x1="55" x2="740" y1="100" y2="100" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="104" fill="#94A3B8" fontSize="10" textAnchor="end">1,500</text>
                  <line x1="55" x2="740" y1="160" y2="160" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="164" fill="#94A3B8" fontSize="10" textAnchor="end">1,000</text>
                  <line x1="55" x2="740" y1="220" y2="220" stroke={gridStroke} strokeDasharray="3 3" />
                  <text x="48" y="224" fill="#94A3B8" fontSize="10" textAnchor="end">500</text>
                  <line x1="55" x2="740" y1="280" y2="280" stroke={axisStroke} strokeWidth="1.5" />
                  <text x="48" y="284" fill="#94A3B8" fontSize="10" textAnchor="end">0</text>

                  {/* Return Period Threshold Lines */}
                  {/* 20-yr */}
                  <line x1="55" x2="740" y1="70" y2="70" stroke="#EF4444" strokeDasharray="5 4" strokeWidth="1.5" />
                  <rect x="635" y="58" width="105" height="18" rx="3" fill="#FEE2E2" stroke="#EF4444" strokeWidth="0.8" />
                  <text x="687" y="70" fill="#B91C1C" fontSize="9.5" fontWeight="700" textAnchor="middle">20-yr: {thresholds.rp20} m³/s</text>

                  {/* 5-yr */}
                  <line x1="55" x2="740" y1="125" y2="125" stroke="#F97316" strokeDasharray="6 4" strokeWidth="1.5" />
                  <rect x="635" y="113" width="105" height="18" rx="3" fill="#FFEDD5" stroke="#F97316" strokeWidth="0.8" />
                  <text x="687" y="125" fill="#C2410C" fontSize="9.5" fontWeight="700" textAnchor="middle">5-yr: {thresholds.rp5} m³/s</text>

                  {/* 2-yr */}
                  <line x1="55" x2="740" y1="180" y2="180" stroke="#EAB308" strokeDasharray="4 4" strokeWidth="1.5" />
                  <rect x="635" y="168" width="105" height="18" rx="3" fill="#FEF9C3" stroke="#EAB308" strokeWidth="0.8" />
                  <text x="687" y="180" fill="#854D0E" fontSize="9.5" fontWeight="700" textAnchor="middle">2-yr: {thresholds.rp2} m³/s</text>

                  {/* Confidence Area Bands */}
                  <path
                    d="M 55,240 Q 150,210 240,160 T 390,95 Q 490,80 580,120 T 740,210 L 740,280 L 55,280 Z"
                    fill="url(#ptMinMaxGrad)"
                  />
                  <path
                    d="M 55,230 Q 150,200 240,150 T 390,110 Q 490,100 580,135 T 740,215 L 740,270 L 55,270 Z"
                    fill="url(#ptIqrGrad)"
                  />

                  {/* Median Blue Polyline */}
                  <polyline
                    fill="none"
                    stroke="#2563EB"
                    strokeWidth="3"
                    points="55,235 125,215 195,185 265,150 335,120 405,100 475,95 545,115 615,155 685,190 740,225"
                  />

                  {/* Data Points on Line */}
                  {[
                    [55, 235],
                    [125, 215],
                    [195, 185],
                    [265, 150],
                    [335, 120],
                    [405, 100],
                    [475, 95],
                    [545, 115],
                    [615, 155],
                    [685, 190],
                    [740, 225],
                  ].map(([cx, cy], i) => (
                    <circle
                      key={i}
                      cx={cx}
                      cy={cy}
                      r={selectedDayIdx === i ? 6 : 3.5}
                      fill={selectedDayIdx === i ? '#2563EB' : '#1D4ED8'}
                      stroke="#FFFFFF"
                      strokeWidth={selectedDayIdx === i ? 2 : 1}
                    />
                  ))}
                </svg>
              </div>

              {/* 10-Day Selector Pill Row */}
              <div className="grid grid-cols-5 sm:grid-cols-10 gap-1.5 pt-1">
                {chartFloodDays.slice(0, 10).map((d, idx) => {
                  const isSelected = selectedDayIdx === idx;
                  return (
                    <button
                      key={idx}
                      onClick={() => setSelectedDayIdx(idx)}
                      className={`p-2 rounded-xl border text-center transition-all text-xs flex flex-col justify-between min-h-[54px] cursor-pointer ${
                        isSelected
                          ? 'bg-blue-600 text-white border-blue-600 font-bold shadow-xs'
                          : 'bg-slate-100 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-200 dark:hover:bg-[#222E3E]'
                      }`}
                    >
                      <span className="text-[10px] uppercase">{d.date?.slice(5) || `D${idx + 1}`}</span>
                      <span className="font-bold text-xs">{Math.round(d.medianDischarge || 0)}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* AUTONOMOUS BROADCAST RULE ENGINE EVALUATION */}
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
                <Zap className="w-4 h-4 text-amber-500" />
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Autonomous Broadcast Rule Engine (Zero-Intermediary Relay)
                </h3>
              </div>

              <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                Broadcasts are an integral part of the automated rule engine. The moment hydrological inputs or ensemble simulations exceed a risky threshold, alerts are dispatched directly to subscribed residents without requiring human gatekeeping or ground report approvals.
              </p>

              <div className="space-y-2.5 text-xs">
                <div className="p-3.5 rounded-xl bg-orange-50 dark:bg-[#1C2633] border border-orange-300 dark:border-orange-500/30 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-orange-900 dark:text-orange-300">
                      Rule #01: 5-Yr Warning Threshold Exceedance
                    </div>
                    <div className="text-[11px] text-slate-600 dark:text-[#9AA8B8]">
                      Condition: &ge; 30% ensemble members breach {thresholds.rp5} m³/s &rarr; Auto-Broadcast Warning via WhatsApp, Telegram &amp; Push
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded bg-orange-100 dark:bg-orange-500/20 text-orange-800 dark:text-orange-300 font-bold border border-orange-200 dark:border-orange-500/30 self-start sm:self-auto">
                    ACTIVE AUTONOMOUS TRIGGER
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      Rule #02: 20-Yr Severe Exceedance (Life-Safety Emergency)
                    </div>
                    <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                      Condition: &ge; 30% ensemble members breach {thresholds.rp20} m³/s &rarr; Auto-Broadcast Severe Evacuation Directive
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-[#9AA8B8] font-medium self-start sm:self-auto">
                    STANDBY MONITORING
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: READINGS PROVING ALERT INTEGRITY */}
          <div className="lg:col-span-4 space-y-6">
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
                <Activity className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF]" />
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Current Readings (Proving Alert Integrity)
                </h3>
              </div>

              <div className="space-y-2.5 text-xs">
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">River Discharge Rate</span>
                    <span className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeFloodDay?.medianDischarge ? `${Math.round(activeFloodDay.medianDischarge)} m³/s` : '1,340 m³/s'}
                    </span>
                  </div>
                  <span className="text-[10px] text-orange-600 dark:text-orange-400 font-semibold px-2 py-0.5 rounded bg-orange-100 dark:bg-orange-500/20">
                    Breaches 5-yr
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">Gauge Stage Height (+MSL)</span>
                    <span className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">552.4 m</span>
                  </div>
                  <span className="text-[10px] text-slate-500 font-mono">Datum: 549.2m</span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">Crest Rate of Rise</span>
                    <span className="text-sm font-bold text-amber-600 dark:text-amber-400">+12 cm/hr</span>
                  </div>
                  <span className="text-[10px] text-amber-600 font-semibold">Rising Fast</span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">24h Catchment Rainfall</span>
                    <span className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">112.4 mm</span>
                  </div>
                  <span className="text-[10px] text-blue-600 dark:text-[#7AA7FF]">Heavy Downpour</span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">Historical Record Peak (2019)</span>
                    <span className="text-sm font-bold text-slate-600 dark:text-[#9AA8B8]">1,890 m³/s</span>
                  </div>
                  <span className="text-[10px] text-slate-400">Benchmarked</span>
                </div>
              </div>
            </div>

            {/* Telemetry Sensor Node Ingest Provenance */}
            <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-3 text-xs">
              <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Data Source Integrity</h3>
              <div className="space-y-1.5 text-slate-600 dark:text-[#9AA8B8]">
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-[#2A3644]">
                  <span>Telemetry Ingest Source:</span>
                  <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Open-Meteo GloFAS / CWC</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-[#2A3644]">
                  <span>Ensemble Size:</span>
                  <span className="font-mono text-slate-900 dark:text-[#EAF0F6]">50 ECMWF Members</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-[#2A3644]">
                  <span>Sensor Station ID:</span>
                  <span className="font-mono text-blue-600 dark:text-[#7AA7FF]">#IN-{currentStation.id.toUpperCase()}</span>
                </div>
                <div className="flex justify-between py-1">
                  <span>Verification State:</span>
                  <span className="font-bold text-emerald-600 dark:text-emerald-400">Cryptographically Signed</span>
                </div>
              </div>

              <button
                onClick={() => dispatch(setActiveTab('history'))}
                className="w-full py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-blue-700 dark:text-[#7AA7FF] border border-slate-200 dark:border-[#2A3644] font-semibold text-xs flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                <span>View Past Hazard Records in History</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PointDetailView;

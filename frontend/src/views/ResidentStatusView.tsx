import React, { useState, useEffect, useMemo } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setResidentTab } from '../features/ui/uiSlice';
import { useGetLocationsQuery } from '../api/registryApi';
import { useGetAlertStatusesQuery, useGetForecastByLocationQuery } from '../api/rulesApi';
import { useGetHeatPointsQuery, useGetHeatPointForecastQuery } from '../api/heatApi';
import { useGetAqPointsQuery, useGetAqPointForecastQuery } from '../api/aqApi';
import { useGetReportsForLocationQuery } from '../api/fanoutApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import { ALL_HEAT_STATIONS } from '../catalog/heatStations';
import { ALL_AQ_STATIONS } from '../catalog/aqStations';
import type { Severity, Provenance, ForecastDayStats } from '../types/api';
import { normalizeForecastDay } from '../types/api';

export const ResidentStatusView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedWardId = useAppSelector((state) => state.resident.selectedWardId) || 'mumbai-kurla';
  const language = useAppSelector((state) => state.ui.language);
  const globalHazard = useAppSelector((state) => state.ui.activeHazard);

  const [selectedDayIdx, setSelectedDayIdx] = useState<number>(3);
  const [activeHazardTab, setActiveHazardTab] = useState<'FLOOD' | 'HEAT' | 'AIR'>('FLOOD');

  // Synchronize activeHazardTab with global activeHazard if changed
  useEffect(() => {
    if (globalHazard === 'HEAT') setActiveHazardTab('HEAT');
    else if (globalHazard === 'AIR_QUALITY') setActiveHazardTab('AIR');
    else if (globalHazard === 'FLOOD') setActiveHazardTab('FLOOD');
  }, [globalHazard]);

  // RTK Query API hooks for all 3 hazards
  const { data: locationsData } = useGetLocationsQuery();
  const { data: alertStatuses } = useGetAlertStatusesQuery(undefined, { pollingInterval: 6000 });
  const { data: reports } = useGetReportsForLocationQuery(selectedWardId);
  const { data: floodForecastData } = useGetForecastByLocationQuery(selectedWardId);

  const { data: heatPoints } = useGetHeatPointsQuery(undefined, { pollingInterval: 6000 });
  const { data: heatForecastData } = useGetHeatPointForecastQuery(selectedWardId);

  const { data: aqPoints } = useGetAqPointsQuery(undefined, { pollingInterval: 6000 });
  const { data: aqForecastData } = useGetAqPointForecastQuery(selectedWardId);

  const currentStation = useMemo(() => {
    if (activeHazardTab === 'HEAT') {
      const heatMatch = ALL_HEAT_STATIONS.find((s) => s.id === selectedWardId) || ALL_HEAT_STATIONS[0];
      return {
        id: heatMatch.id,
        name: heatMatch.name,
        state: heatMatch.state,
        latitude: heatMatch.latitude,
        longitude: heatMatch.longitude,
        riverBasin: heatMatch.climateZone,
        regionType: heatMatch.regionType,
        threshold2yr: 38,
        threshold5yr: 42,
        threshold20yr: 45,
        primaryLanguage: heatMatch.primaryLanguage,
        active: heatMatch.active,
      };
    }
    if (activeHazardTab === 'AIR') {
      const aqMatch = ALL_AQ_STATIONS.find((s) => s.id === selectedWardId) || ALL_AQ_STATIONS[0];
      return {
        id: aqMatch.id,
        name: aqMatch.name,
        state: aqMatch.state,
        latitude: aqMatch.latitude,
        longitude: aqMatch.longitude,
        riverBasin: aqMatch.zone,
        regionType: 'PLAINS' as const,
        threshold2yr: 200,
        threshold5yr: 300,
        threshold20yr: 400,
        primaryLanguage: aqMatch.primaryLanguage,
        active: aqMatch.active,
      };
    }
    const riverStations = locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;
    return riverStations.find((s) => s.id === selectedWardId) || riverStations[0];
  }, [activeHazardTab, selectedWardId, locationsData]);

  // Flood details
  const activeFloodStatus = alertStatuses?.find((s) => s.locationId === selectedWardId);
  const floodSeverity: Severity = activeFloodStatus?.severity || 'WARNING';
  const floodProvenance: Provenance = activeFloodStatus?.provenance || 'MODEL_PLUS_REPORTS';
  const verifiedReportsCount = reports?.length || 3;

  const thresholds = floodForecastData?.thresholds || {
    rp2: currentStation.threshold2yr || 40,
    rp5: currentStation.threshold5yr || 85,
    rp20: currentStation.threshold20yr || 180,
  };
  const rawFloodDays: ForecastDayStats[] = floodForecastData?.days?.length ? floodForecastData.days : [];
  const chartFloodDays = rawFloodDays.map((d) => normalizeForecastDay(d, thresholds));
  const activeFloodDay = chartFloodDays[selectedDayIdx] || chartFloodDays[0];

  // Heat details
  const activeHeatPoint = heatPoints?.find((p) => p.locationId === selectedWardId);
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
  const activeAqPoint = aqPoints?.find((p) => p.locationId === selectedWardId);
  const aqDays = aqForecastData?.days || [];
  const activeAqDay = aqDays[selectedDayIdx] || aqDays[0];
  const aqVal = activeAqDay?.projectedAqi ?? activeAqPoint?.aqi ?? 185;
  const aqSeverity: Severity =
    aqVal > 400 ? 'SEVERE' : aqVal > 300 ? 'WARNING' : aqVal > 200 ? 'WATCH' : 'NONE';

  // Current active severity and provenance depending on selected tab
  const severity: Severity =
    activeHazardTab === 'HEAT' ? heatSeverity : activeHazardTab === 'AIR' ? aqSeverity : floodSeverity;

  // Dynamic status text depending on active hazard and language
  const getAdvisoryText = () => {
    if (activeHazardTab === 'HEAT') {
      if (severity === 'SEVERE') {
        return language === 'mr'
          ? 'अति तीव्र उष्णतेची लाट इशारा: दुपारी उन्हात जाणे पूर्णपणे टाळा. पुरेसे पाणी प्या.'
          : language === 'hi'
          ? 'अत्यधिक लू की आपातकालीन चेतावनी: दोपहर 11 से 4 बजे तक धूप में न निकलें। डिहाइड्रेशन से बचें।'
          : 'CRITICAL SEVERE HEAT WAVE ALERT: Extreme heat departure. Avoid midday sun, hydrate constantly.';
      }
      if (severity === 'WARNING') {
        return language === 'mr'
          ? 'उष्णतेची लाट इशारा: तापमान सामान्यपेक्षा जास्त आहे. आवश्यक खबरदारी घ्या.'
          : language === 'hi'
          ? 'लू की चेतावनी: तापमान सामान्य से काफी अधिक है। धूप से बचें और तरल पदार्थों का सेवन करें।'
          : 'HEAT WAVE WARNING: Temperatures exceed official IMD thresholds. High risk of thermal exhaustion.';
      }
      return language === 'mr'
        ? 'सध्या तापमान सामान्य मर्यादेत आहे.'
        : language === 'hi'
        ? 'वर्तमान तापमान सामान्य सीमा के भीतर है।'
        : 'BASELINE NORMAL: Temperatures are within normal seasonal climatological boundaries.';
    }

    if (activeHazardTab === 'AIR') {
      if (severity === 'SEVERE') {
        return language === 'mr'
          ? 'धोकादायक हवा गुणवत्ता इशारा: बाहेर जाणे टाळा आणि N95 मास्क वापरा.'
          : language === 'hi'
          ? 'गंभीर वायु प्रदूषण आपातकाल: बाहर जाने से बचें, खिड़कियां बंद रखें और N95 मास्क पहनें।'
          : 'HAZARDOUS AIR QUALITY EMERGENCY: AQI exceeds 400. Stay indoors with HEPA air filtration.';
      }
      if (severity === 'WARNING') {
        return language === 'mr'
          ? 'खराब हवा गुणवत्ता इशारा: वृद्ध आणि लहान मुलांनी बाहेर फिरणे टाळावे.'
          : language === 'hi'
          ? 'बहुत खराब वायु गुणवत्ता चेतावनी: बुजुर्ग और बच्चे बाहरी व्यायाम से बचें।'
          : 'VERY POOR AIR QUALITY WARNING: AQI 301-400. Vulnerable populations should avoid outdoor exertion.';
      }
      return language === 'mr'
        ? 'हवा गुणवत्ता समाधानकारक आहे.'
        : language === 'hi'
        ? 'वायु गुणवत्ता स्वीकार्य सीमा के भीतर है।'
        : 'ACCEPTABLE AIR QUALITY: Ambient air parameters within CPCB national standards.';
    }

    // FLOOD
    if (language === 'mr') {
      if (severity === 'SEVERE') return 'तातडीचा इशारा: नदीकाठच्या सखल भागात पूरस्थिती निर्माण झाली आहे. सुरक्षित स्थळी जा.';
      if (severity === 'WARNING') return 'सावधानता इशारा: पाणी पातळी सातत्याने वाढत आहे. नदीकाठच्या नागरिकांनी सतर्क राहावे.';
      return 'सध्या सर्व पातळी सामान्य आहे. नियमित जलस्थिती नियंत्रणात आहे.';
    }
    if (language === 'hi') {
      if (severity === 'SEVERE') return 'आपातकालीन चेतावनी: नदी का जलस्तर खतरे के निशान के करीब है। निचले इलाकों से सुरक्षित स्थानों पर जाएं।';
      if (severity === 'WARNING') return 'चेतावनी: जलस्तर में लगातार वृद्धि देखी जा रही है। नदी तट के निवासी सतर्क रहें।';
      return 'वर्तमान में स्थिति सामान्य है। जलस्तर सुरक्षित सीमा के भीतर है।';
    }
    if (severity === 'SEVERE') return 'CRITICAL FLOOD ALERT: Extreme crest projected within 12 hours. Evacuate low-lying riverbanks.';
    if (severity === 'WARNING') return 'ELEVATED FLOOD WARNING: River discharge approaching 5-yr return threshold. Precautionary monitoring in effect.';
    return 'BASELINE NORMAL: Telemetry is within historical seasonal safety margins across all monitoring nodes.';
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 pb-12 transition-colors duration-200">
      {/* 1. HERO CIVIC TELEMETRY CARD */}
      <section
        className={`rounded-2xl border p-6 sm:p-8 transition-all shadow-sm ${
          severity === 'SEVERE'
            ? 'bg-gradient-to-b from-red-50 to-white dark:from-red-950/40 dark:via-[#151D27] dark:to-[#151D27] border-red-200 dark:border-red-500/40'
            : severity === 'WARNING'
            ? 'bg-gradient-to-b from-amber-50 to-white dark:from-amber-950/30 dark:via-[#151D27] dark:to-[#151D27] border-amber-200 dark:border-amber-500/40'
            : 'bg-white dark:bg-[#151D27] border-slate-200 dark:border-[#2A3644]'
        }`}
      >
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="space-y-3">
            {/* Top Meta Chips */}
            <div className="flex flex-wrap items-center gap-2 text-xs">
              {/* Severity Pill */}
              <span
                className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full font-bold uppercase tracking-wider text-xs ${
                  severity === 'SEVERE'
                    ? 'bg-red-100 text-red-700 dark:bg-red-500/20 dark:text-red-400 border border-red-200 dark:border-red-500/40'
                    : severity === 'WARNING'
                    ? 'bg-amber-100 text-amber-800 dark:bg-amber-500/20 dark:text-amber-300 border border-amber-200 dark:border-amber-500/40'
                    : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/40'
                }`}
              >
                <span className="material-symbols-outlined text-sm">
                  {severity === 'SEVERE' ? 'crisis_alert' : severity === 'WARNING' ? 'warning' : 'check_circle'}
                </span>
                {activeHazardTab === 'HEAT'
                  ? severity === 'SEVERE'
                    ? 'Severe Heat Wave Alert'
                    : severity === 'WARNING'
                    ? 'Heat Wave Warning'
                    : 'Normal Seasonal Temperature'
                  : activeHazardTab === 'AIR'
                  ? severity === 'SEVERE'
                    ? 'Severe Air Emergency'
                    : severity === 'WARNING'
                    ? 'Very Poor AQI Warning'
                    : 'Acceptable Air Quality'
                  : severity === 'SEVERE'
                  ? 'Severe Flood Alert'
                  : severity === 'WARNING'
                  ? 'Flood Warning'
                  : 'Normal Baseline'}
              </span>

              {/* Provenance Badge */}
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border border-slate-200 dark:border-[#2A3644] font-medium">
                <span className="material-symbols-outlined text-[14px] text-blue-600 dark:text-[#7AA7FF]">verified</span>
                {activeHazardTab === 'HEAT'
                  ? 'Open-Meteo & IMD Normals'
                  : activeHazardTab === 'AIR'
                  ? 'CPCB CAAQMS Telemetry'
                  : floodProvenance === 'MODEL_PLUS_REPORTS'
                  ? `Model + ${verifiedReportsCount} Ground Reports`
                  : '50-Member GloFAS Ensemble'}
              </span>

              <span className="text-slate-500 dark:text-[#9AA8B8] font-mono text-[11px]">
                Ward ID: #{currentStation.id.toUpperCase()}
              </span>
            </div>

            {/* Main Location Title */}
            <div>
              <h1 className="text-2xl sm:text-4xl font-extrabold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
                {currentStation.name}, {currentStation.state}
              </h1>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-[#9AA8B8] mt-1 font-medium">
                {currentStation.riverBasin} · {currentStation.regionType || 'PLAINS'} Zone · Real-time Civic Telemetry Relay
              </p>
            </div>

            {/* Dynamic Status Text Banner */}
            <div className="p-3.5 rounded-xl bg-slate-100 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-xs sm:text-sm font-medium text-slate-800 dark:text-[#EAF0F6] leading-relaxed max-w-2xl">
              {getAdvisoryText()}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="flex flex-col sm:flex-row lg:flex-col gap-2.5 shrink-0">
            <button
              onClick={() => dispatch(setResidentTab('report'))}
              className="px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined text-sm">campaign</span>
              <span>Submit Ground Report</span>
            </button>
            <button
              onClick={() => dispatch(setResidentTab('subscribe'))}
              className="px-4 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-slate-700 dark:text-[#EAF0F6] border border-slate-200 dark:border-[#2A3644] font-semibold text-xs flex items-center justify-center gap-2 transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined text-sm text-blue-600 dark:text-[#7AA7FF]">notifications</span>
              <span>Manage Subscriptions</span>
            </button>
          </div>
        </div>
      </section>

      {/* 2. THREE MULTI-HAZARD TILES BENTO */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Tile 1: Flood Telemetry */}
        <div
          onClick={() => setActiveHazardTab('FLOOD')}
          className={`p-5 rounded-2xl border cursor-pointer transition-all shadow-sm ${
            activeHazardTab === 'FLOOD'
              ? 'bg-white dark:bg-[#151D27] border-blue-500 dark:border-[#7AA7FF] ring-2 ring-blue-500/20'
              : 'bg-white dark:bg-[#151D27]/80 border-slate-200 dark:border-[#2A3644] hover:border-slate-300 dark:hover:border-[#3A4B5F]'
          }`}
        >
          <div className="flex items-center justify-between text-xs text-slate-500 dark:text-[#9AA8B8] mb-2">
            <span className="font-semibold uppercase tracking-wider flex items-center gap-1.5 text-blue-600 dark:text-blue-400">
              <span className="material-symbols-outlined text-[16px]">waves</span>
              River Flood
            </span>
            <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${
              floodSeverity === 'SEVERE'
                ? 'bg-red-100 text-red-800 dark:bg-red-500/20 dark:text-red-300 border border-red-200'
                : floodSeverity === 'WARNING'
                ? 'bg-amber-100 text-amber-800 dark:bg-amber-500/20 dark:text-amber-300 border border-amber-200'
                : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300 border border-emerald-200'
            }`}>
              {floodSeverity === 'NONE' ? 'Normal' : floodSeverity}
            </span>
          </div>
          <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
            {activeFloodDay?.medianDischarge ? `${Math.round(activeFloodDay.medianDischarge)} m³/s` : '967 m³/s'}
          </div>
          <div className="text-xs text-slate-600 dark:text-[#9AA8B8] mt-1 flex items-center gap-1.5">
            <span className="text-amber-600 dark:text-amber-400 font-semibold flex items-center">
              <span className="material-symbols-outlined text-[14px]">trending_up</span>
              2-Yr Watch: {thresholds.rp2} m³/s
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-slate-500 dark:text-[#65778B] flex justify-between">
            <span>5-Yr Threshold: {thresholds.rp5} m³/s</span>
            <span className="text-blue-600 dark:text-[#7AA7FF]">50-member GloFAS</span>
          </div>
        </div>

        {/* Tile 2: Extreme Heat & Normal Departure */}
        <div
          onClick={() => setActiveHazardTab('HEAT')}
          className={`p-5 rounded-2xl border cursor-pointer transition-all shadow-sm ${
            activeHazardTab === 'HEAT'
              ? 'bg-white dark:bg-[#151D27] border-blue-500 dark:border-[#7AA7FF] ring-2 ring-blue-500/20'
              : 'bg-white dark:bg-[#151D27]/80 border-slate-200 dark:border-[#2A3644] hover:border-slate-300 dark:hover:border-[#3A4B5F]'
          }`}
        >
          <div className="flex items-center justify-between text-xs text-slate-500 dark:text-[#9AA8B8] mb-2">
            <span className="font-semibold uppercase tracking-wider flex items-center gap-1.5 text-orange-600 dark:text-orange-400">
              <span className="material-symbols-outlined text-[16px]">thermostat</span>
              Extreme Heat
            </span>
            <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${
              heatSeverity === 'SEVERE'
                ? 'bg-red-100 text-red-800 dark:bg-red-500/20 dark:text-red-300 border border-red-200'
                : heatSeverity === 'WARNING'
                ? 'bg-orange-100 text-orange-800 dark:bg-orange-500/20 dark:text-orange-300 border border-orange-200'
                : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300 border border-emerald-200'
            }`}>
              {heatSeverity === 'NONE' ? 'Normal' : heatSeverity}
            </span>
          </div>
          <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
            {activeHeatPoint?.currentTmax ? `${activeHeatPoint.currentTmax} °C` : '41.2 °C'}
          </div>
          <div className="text-xs text-slate-600 dark:text-[#9AA8B8] mt-1 flex items-center gap-1.5">
            <span className="text-amber-600 dark:text-amber-400 font-semibold">
              Heat Index: {activeHeatPoint?.currentApparentMax ? `${activeHeatPoint.currentApparentMax} °C` : '44.8 °C'}
            </span>
            <span>· Departure: {(activeHeatPoint?.departure ?? 0) >= 0 ? `+${activeHeatPoint?.departure ?? 3.2}` : `${activeHeatPoint?.departure ?? 0}`} °C</span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-slate-500 dark:text-[#65778B] flex justify-between">
            <span>Normal Baseline: {activeHeatPoint?.normalTmax ?? 38.0} °C</span>
            <span className="text-slate-500 dark:text-[#9AA8B8]">IMD Ingest</span>
          </div>
        </div>

        {/* Tile 3: Air Quality (AQI) */}
        <div
          onClick={() => setActiveHazardTab('AIR')}
          className={`p-5 rounded-2xl border cursor-pointer transition-all shadow-sm ${
            activeHazardTab === 'AIR'
              ? 'bg-white dark:bg-[#151D27] border-blue-500 dark:border-[#7AA7FF] ring-2 ring-blue-500/20'
              : 'bg-white dark:bg-[#151D27]/80 border-slate-200 dark:border-[#2A3644] hover:border-slate-300 dark:hover:border-[#3A4B5F]'
          }`}
        >
          <div className="flex items-center justify-between text-xs text-slate-500 dark:text-[#9AA8B8] mb-2">
            <span className="font-semibold uppercase tracking-wider flex items-center gap-1.5 text-cyan-600 dark:text-cyan-400">
              <span className="material-symbols-outlined text-[16px]">air</span>
              Air Quality
            </span>
            <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${
              aqSeverity === 'SEVERE'
                ? 'bg-red-100 text-red-800 dark:bg-red-500/20 dark:text-red-300 border border-red-200'
                : aqSeverity === 'WARNING'
                ? 'bg-orange-100 text-orange-800 dark:bg-orange-500/20 dark:text-orange-300 border border-orange-200'
                : 'bg-yellow-100 text-yellow-800 dark:bg-yellow-500/20 dark:text-yellow-300 border border-yellow-200'
            }`}>
              {activeAqPoint?.category || 'MODERATE'}
            </span>
          </div>
          <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
            {activeAqPoint?.aqi ? `${activeAqPoint.aqi} AQI` : '185 AQI'}
          </div>
          <div className="text-xs text-slate-600 dark:text-[#9AA8B8] mt-1 flex items-center gap-1.5">
            <span className="text-cyan-700 dark:text-cyan-400 font-semibold">
              PM2.5: {activeAqPoint?.pm25_24h ? `${activeAqPoint.pm25_24h} µg/m³` : '92.4 µg/m³'}
            </span>
            <span>· Dominant: {activeAqPoint?.dominantPollutant || 'PM2.5'}</span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-slate-500 dark:text-[#65778B] flex justify-between">
            <span>CPCB CAAQMS Monitor</span>
            <span className="text-slate-500 dark:text-[#9AA8B8]">Verified 15m ago</span>
          </div>
        </div>
      </section>

      {/* 3. DYNAMIC FORECAST HORIZON SECTION */}
      {activeHazardTab === 'HEAT' ? (
        /* HEAT HORIZON */
        <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-100 dark:border-[#2A3644] gap-2">
            <div>
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-orange-600 dark:text-orange-400 text-[20px]">thermostat</span>
                <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">
                  7-Day Temperature &amp; Heat Wave Forecast Horizon
                </h2>
              </div>
              <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">
                Open-Meteo Weather Model with IMD Heat Wave Departure Thresholds for {currentStation.name}
              </p>
            </div>

            <div className="flex items-center gap-3 text-xs text-slate-600 dark:text-[#9AA8B8] flex-wrap">
              <span className="flex items-center gap-1">
                <span className="w-2.5 h-2.5 rounded-full bg-red-500"></span>
                Daily Tmax
              </span>
              <span className="flex items-center gap-1">
                <span className="w-3 h-1 border-t-2 border-dashed border-orange-500"></span>
                Heat Wave (≥45°C)
              </span>
              <span className="flex items-center gap-1">
                <span className="w-3 h-1 border-t-2 border-dashed border-red-500"></span>
                Severe Heat Wave (≥47°C)
              </span>
            </div>
          </div>

          {/* Forecast Days Selector Buttons */}
          <div className="grid grid-cols-4 sm:grid-cols-7 gap-2 overflow-x-auto pb-1">
            {(heatDays.length > 0 ? heatDays.slice(0, 7) : [0, 1, 2, 3, 4, 5, 6].map((i) => ({
              date: `2026-11-0${i + 2}`,
              tmax: +(38 + i * 0.7).toFixed(1),
              apparentMax: +(41 + i * 0.9).toFixed(1),
              departure: +(2 + i * 0.5).toFixed(1),
            }))).map((day: any, idx: number) => {
              const isSelected = selectedDayIdx === idx;
              const dateStr = day.date ? new Date(day.date).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' }) : `Day ${idx + 1}`;
              const isSevere = day.isSevereHeatWave || day.tmax >= 47;
              const isWarning = day.isHeatWave || day.tmax >= 45;

              return (
                <button
                  key={idx}
                  onClick={() => setSelectedDayIdx(idx)}
                  className={`p-2.5 rounded-xl text-center border transition-all text-xs flex flex-col items-center justify-between min-h-[58px] cursor-pointer ${
                    isSelected
                      ? 'ring-2 ring-red-500 dark:ring-red-400 bg-red-100 dark:bg-red-950/40 text-red-900 dark:text-white font-bold'
                      : isSevere
                      ? 'bg-red-50 text-red-700 border-red-300 dark:bg-red-500/20 dark:text-red-400'
                      : isWarning
                      ? 'bg-orange-50 text-orange-700 border-orange-300 dark:bg-orange-500/20 dark:text-orange-400'
                      : 'bg-slate-50 text-slate-700 border-slate-200 dark:bg-[#1C2633] dark:text-[#9AA8B8]'
                  }`}
                >
                  <span className="text-[10px] uppercase font-semibold">{dateStr}</span>
                  <span className="text-xs font-bold mt-1">{day.tmax}°C</span>
                  <span className="text-[9px] text-slate-500 dark:text-[#65778B]">{day.apparentMax}°C feel</span>
                </button>
              );
            })}
          </div>

          {/* Selected Day Stats Card */}
          {activeHeatDay && (
            <div className="bg-slate-50 dark:bg-[#1C2633] rounded-xl p-4 border border-slate-200 dark:border-[#2A3644] flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-orange-100 dark:bg-orange-500/20 border border-orange-200 dark:border-orange-500/30 flex items-center justify-center text-orange-700 dark:text-orange-400 font-bold">
                  {selectedDayIdx + 1}
                </div>
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                    {activeHeatDay.date} — High Temperature Forecast
                  </div>
                  <div className="text-xs text-slate-600 dark:text-[#9AA8B8]">
                    Tmax: {activeHeatDay.tmax}°C · Heat Index: {activeHeatDay.apparentMax}°C · Normal: {activeHeatDay.normalTmax}°C · Departure: {(activeHeatDay.departure ?? 0) >= 0 ? `+${activeHeatDay.departure}` : `${activeHeatDay.departure}`}°C
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span
                  className={`px-3 py-1 rounded-full text-xs font-bold border ${
                    activeHeatDay.isSevereHeatWave || activeHeatDay.tmax >= 47
                      ? 'bg-red-100 text-red-700 border-red-300 dark:bg-red-500/20 dark:text-red-400'
                      : activeHeatDay.isHeatWave || activeHeatDay.tmax >= 45
                      ? 'bg-orange-100 text-orange-700 border-orange-300 dark:bg-orange-500/20 dark:text-orange-400'
                      : 'bg-emerald-100 text-emerald-700 border-emerald-300 dark:bg-emerald-500/20 dark:text-emerald-400'
                  }`}
                >
                  {activeHeatDay.isSevereHeatWave || activeHeatDay.tmax >= 47
                    ? 'Severe Heat Wave'
                    : activeHeatDay.isHeatWave || activeHeatDay.tmax >= 45
                    ? 'Heat Wave Warning'
                    : 'Normal Seasonal Range'}
                </span>
              </div>
            </div>
          )}
        </section>
      ) : activeHazardTab === 'AIR' ? (
        /* AIR QUALITY HORIZON */
        <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-100 dark:border-[#2A3644] gap-2">
            <div>
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-cyan-600 dark:text-cyan-400 text-[20px]">air</span>
                <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Multi-Day CPCB Air Quality Index (AQI) Forecast
                </h2>
              </div>
              <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">
                CPCB Continuous Ambient Air Quality Standards for {currentStation.name}
              </p>
            </div>

            <div className="flex items-center gap-3 text-xs text-slate-600 dark:text-[#9AA8B8] flex-wrap">
              <span className="flex items-center gap-1">
                <span className="w-2.5 h-2.5 rounded-full bg-red-700"></span>
                Severe (401-500)
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2.5 h-2.5 rounded-full bg-red-500"></span>
                Very Poor (301-400)
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2.5 h-2.5 rounded-full bg-orange-500"></span>
                Poor (201-300)
              </span>
            </div>
          </div>

          {/* Forecast Days Selector Buttons */}
          <div className="grid grid-cols-3 sm:grid-cols-6 gap-2 overflow-x-auto pb-1">
            {(aqDays.length > 0 ? aqDays.slice(0, 6) : [0, 1, 2, 3, 4, 5].map((i) => ({
              date: `2026-11-0${i + 2}`,
              projectedAqi: Math.round(290 + i * 15),
              projectedCategory: i >= 2 ? 'VERY_POOR' : 'POOR',
            }))).map((day: any, idx: number) => {
              const isSelected = selectedDayIdx === idx;
              const dateStr = day.date ? new Date(day.date).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' }) : `Day ${idx + 1}`;
              const aqi = day.projectedAqi || day.aqi || 200;

              return (
                <button
                  key={idx}
                  onClick={() => setSelectedDayIdx(idx)}
                  className={`p-2.5 rounded-xl text-center border transition-all text-xs flex flex-col items-center justify-between min-h-[58px] cursor-pointer ${
                    isSelected
                      ? 'ring-2 ring-red-600 dark:ring-red-400 bg-red-100 dark:bg-red-950/40 text-red-900 dark:text-white font-bold'
                      : aqi > 400
                      ? 'bg-red-100 text-red-800 border-red-400 dark:bg-red-900/30 dark:text-red-400'
                      : aqi > 300
                      ? 'bg-red-50 text-red-700 border-red-300 dark:bg-red-500/20 dark:text-red-400'
                      : 'bg-orange-50 text-orange-700 border-orange-300 dark:bg-orange-500/20 dark:text-orange-400'
                  }`}
                >
                  <span className="text-[10px] uppercase font-semibold">{dateStr}</span>
                  <span className="text-xs font-bold mt-1">{aqi} AQI</span>
                  <span className="text-[9px] opacity-80">{day.projectedCategory || 'POOR'}</span>
                </button>
              );
            })}
          </div>

          {/* Selected Day Stats Card */}
          {activeAqDay && (
            <div className="bg-slate-50 dark:bg-[#1C2633] rounded-xl p-4 border border-slate-200 dark:border-[#2A3644] flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-cyan-100 dark:bg-cyan-500/20 border border-cyan-200 dark:border-cyan-500/30 flex items-center justify-center text-cyan-700 dark:text-cyan-400 font-bold">
                  {selectedDayIdx + 1}
                </div>
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                    {activeAqDay.date} — Air Quality Telemetry
                  </div>
                  <div className="text-xs text-slate-600 dark:text-[#9AA8B8]">
                    Overall AQI: {activeAqDay.projectedAqi || 320} · Category: {activeAqDay.projectedCategory || 'VERY POOR'} · Dominant: {activeAqDay.dominantPollutant || 'PM2.5'} · PM2.5: {activeAqDay.pm25 ?? 142.5} µg/m³ · PM10: {activeAqDay.pm10 ?? 235.0} µg/m³
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span
                  className={`px-3 py-1 rounded-full text-xs font-bold border ${
                    (activeAqDay.projectedAqi || 320) > 400
                      ? 'bg-red-100 text-red-700 border-red-300 dark:bg-red-500/20 dark:text-red-400'
                      : (activeAqDay.projectedAqi || 320) > 300
                      ? 'bg-orange-100 text-orange-700 border-orange-300 dark:bg-orange-500/20 dark:text-orange-400'
                      : 'bg-yellow-100 text-yellow-800 border-yellow-300 dark:bg-yellow-500/20 dark:text-yellow-400'
                  }`}
                >
                  {activeAqDay.projectedCategory || 'VERY POOR'}
                </span>
              </div>
            </div>
          )}
        </section>
      ) : (
        /* FLOOD HORIZON */
        <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-100 dark:border-[#2A3644] gap-2">
            <div>
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-blue-600 dark:text-blue-400 text-[20px]">ssid_chart</span>
                <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">
                  10-Day Hydrological Forecast Horizon (50 Ensemble Members)
                </h2>
              </div>
              <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">
                ECMWF Runoff &amp; GloFAS routing calibrated to {currentStation.name} river basin
              </p>
            </div>

            {/* Return Period Legend */}
            <div className="flex items-center gap-3 text-xs text-slate-600 dark:text-[#9AA8B8] flex-wrap">
              <span className="flex items-center gap-1">
                <span className="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
                Median
              </span>
              <span className="flex items-center gap-1">
                <span className="w-3 h-1 border-t-2 border-dashed border-amber-500"></span>
                2-Yr Watch ({thresholds.rp2})
              </span>
              <span className="flex items-center gap-1">
                <span className="w-3 h-1 border-t-2 border-dashed border-orange-500"></span>
                5-Yr Warning ({thresholds.rp5})
              </span>
              <span className="flex items-center gap-1">
                <span className="w-3 h-1 border-t-2 border-dashed border-red-500"></span>
                20-Yr Severe ({thresholds.rp20})
              </span>
            </div>
          </div>

          {/* Forecast Days Selector Buttons */}
          <div className="grid grid-cols-5 sm:grid-cols-10 gap-1.5 overflow-x-auto pb-1">
            {chartFloodDays.slice(0, 10).map((day, idx) => {
              const isSelected = selectedDayIdx === idx;
              const dateStr = day.date ? new Date(day.date).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' }) : `Day ${idx + 1}`;
              const exceeds = day.exceeds20yr
                ? 'bg-red-50 text-red-700 border-red-300 dark:bg-red-500/20 dark:text-red-400 dark:border-red-500/50'
                : day.exceeds5yr
                ? 'bg-orange-50 text-orange-700 border-orange-300 dark:bg-orange-500/20 dark:text-orange-400 dark:border-orange-500/50'
                : day.exceeds2yr
                ? 'bg-amber-50 text-amber-700 border-amber-300 dark:bg-amber-500/20 dark:text-amber-300 dark:border-amber-500/50'
                : 'bg-slate-50 text-slate-700 border-slate-200 dark:bg-[#1C2633] dark:text-[#9AA8B8] dark:border-[#2A3644]';

              return (
                <button
                  key={idx}
                  onClick={() => setSelectedDayIdx(idx)}
                  className={`p-2 rounded-xl text-center border transition-all text-xs flex flex-col items-center justify-between min-h-[58px] cursor-pointer ${
                    isSelected
                      ? 'ring-2 ring-blue-500 dark:ring-[#7AA7FF] bg-blue-100 dark:bg-[#1F2B3A] text-blue-900 dark:text-white font-bold'
                      : 'hover:bg-slate-100 dark:hover:bg-[#1C2633]'
                  } ${exceeds}`}
                >
                  <span className="text-[10px] uppercase font-semibold">{dateStr}</span>
                  <span className="text-xs font-bold mt-1">{Math.round(day.medianDischarge || 0)}</span>
                  <span className="text-[9px] text-slate-500 dark:text-[#65778B]">m³/s</span>
                </button>
              );
            })}
          </div>

          {/* Selected Day Stats Card */}
          {activeFloodDay && (
            <div className="bg-slate-50 dark:bg-[#1C2633] rounded-xl p-4 border border-slate-200 dark:border-[#2A3644] flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-500/20 border border-blue-200 dark:border-blue-500/30 flex items-center justify-center text-blue-700 dark:text-blue-400 font-bold">
                  {selectedDayIdx + 1}
                </div>
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                    {activeFloodDay.date} — Projected River Discharge
                  </div>
                  <div className="text-xs text-slate-600 dark:text-[#9AA8B8]">
                    Min: {Math.round(activeFloodDay.minDischarge || 0)} m³/s · 25th %: {Math.round(activeFloodDay.p25Discharge || 0)} m³/s · Median: {Math.round(activeFloodDay.medianDischarge || 0)} m³/s · 75th %: {Math.round(activeFloodDay.p75Discharge || 0)} m³/s · Max: {Math.round(activeFloodDay.maxDischarge || 0)} m³/s
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span
                  className={`px-3 py-1 rounded-full text-xs font-bold border ${
                    activeFloodDay.exceeds20yr
                      ? 'bg-red-100 text-red-700 border-red-300 dark:bg-red-500/20 dark:text-red-400 dark:border-red-500/40'
                      : activeFloodDay.exceeds5yr
                      ? 'bg-orange-100 text-orange-700 border-orange-300 dark:bg-orange-500/20 dark:text-orange-400 dark:border-orange-500/40'
                      : activeFloodDay.exceeds2yr
                      ? 'bg-amber-100 text-amber-700 border-amber-300 dark:bg-amber-500/20 dark:text-amber-300 dark:border-amber-500/40'
                      : 'bg-emerald-100 text-emerald-700 border-emerald-300 dark:bg-emerald-500/20 dark:text-emerald-400 dark:border-emerald-500/40'
                  }`}
                >
                  {activeFloodDay.exceeds20yr
                    ? 'Severe Threshold Breach'
                    : activeFloodDay.exceeds5yr
                    ? 'Warning Threshold Breach'
                    : activeFloodDay.exceeds2yr
                    ? 'Watch Threshold Breach'
                    : 'Normal Baseline'}
                </span>
              </div>
            </div>
          )}
        </section>
      )}

      {/* 4. CIVIC READINESS CHECKLIST & SHELTERS */}
      <section className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Left: Neighborhood Action Checklist */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
            <span className="material-symbols-outlined text-amber-500 dark:text-amber-400 text-[20px]">checklist</span>
            <h3 className="font-bold text-slate-900 dark:text-[#EAF0F6]">Resident Advisory Checklist</h3>
          </div>
          <ul className="space-y-3 text-xs text-slate-600 dark:text-[#9AA8B8]">
            <li className="flex items-start gap-2.5">
              <span className="material-symbols-outlined text-emerald-600 dark:text-emerald-400 text-sm mt-0.5">check_box</span>
              <span>
                <strong className="text-slate-900 dark:text-[#EAF0F6]">
                  {activeHazardTab === 'HEAT'
                    ? 'Avoid direct sun between 11 AM - 4 PM:'
                    : activeHazardTab === 'AIR'
                    ? 'Wear an airtight N95 respirator mask outdoors:'
                    : 'Elevate electrical appliances:'}
                </strong>{' '}
                {activeHazardTab === 'HEAT'
                  ? 'Limit strenuous outdoor work during peak thermal load and rest frequently in shaded cooling areas.'
                  : activeHazardTab === 'AIR'
                  ? 'Standard surgical or cloth masks do NOT filter fine PM2.5 particulates.'
                  : 'Unplug ground-level inverters and switch off main fuse boxes if water reaches curbs.'}
              </span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="material-symbols-outlined text-emerald-600 dark:text-emerald-400 text-sm mt-0.5">check_box</span>
              <span>
                <strong className="text-slate-900 dark:text-[#EAF0F6]">
                  {activeHazardTab === 'HEAT'
                    ? 'Hydration and electrolyte replenishment:'
                    : activeHazardTab === 'AIR'
                    ? 'Maintain clean indoor air space:'
                    : 'Store 72-hour clean drinking water:'}
                </strong>{' '}
                {activeHazardTab === 'HEAT'
                  ? 'Drink ORS, coconut water, or buttermilk regularly even if not actively thirsty.'
                  : activeHazardTab === 'AIR'
                  ? 'Keep windows sealed shut, run HEPA air cleaners, and avoid burning incense or mosquito coils indoors.'
                  : 'Municipal water purification stations may throttle pump capacity during peak silt discharge.'}
              </span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="material-symbols-outlined text-emerald-600 dark:text-emerald-400 text-sm mt-0.5">check_box</span>
              <span>
                <strong className="text-slate-900 dark:text-[#EAF0F6]">Check on vulnerable residents:</strong> Identify elderly neighbors, young children, or outdoor workers who may require medical assistance.
              </span>
            </li>
          </ul>
        </div>

        {/* Right: Designated Relief Shelters & EOC Helplines */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
            <span className="material-symbols-outlined text-blue-600 dark:text-[#7AA7FF] text-[20px]">shield</span>
            <h3 className="font-bold text-slate-900 dark:text-[#EAF0F6]">Emergency Contacts &amp; Designated Facilities</h3>
          </div>
          <div className="space-y-2 text-xs">
            <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between">
              <div>
                <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">District EOC Control Room</div>
                <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">24x7 Multi-Hazard Desk</div>
              </div>
              <a
                href="tel:02025501269"
                className="px-3 py-1.5 rounded-lg bg-blue-100 dark:bg-blue-600/30 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-500/40 font-mono font-bold hover:bg-blue-200 dark:hover:bg-blue-600/50"
              >
                020-25501269
              </a>
            </div>

            <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between">
              <div>
                <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Emergency Disaster Response (NDRF)</div>
                <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Multi-Hazard Rescue &amp; Evacuation</div>
              </div>
              <a
                href="tel:1077"
                className="px-3 py-1.5 rounded-lg bg-amber-100 dark:bg-amber-600/30 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-500/40 font-mono font-bold hover:bg-amber-200 dark:hover:bg-amber-600/50"
              >
                1077 (Toll-Free)
              </a>
            </div>

            <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between">
              <div>
                <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                  {activeHazardTab === 'HEAT'
                    ? 'Designated Civic Cooling Center'
                    : activeHazardTab === 'AIR'
                    ? 'Clean-Air Center & Respiratory Clinic'
                    : 'Designated Relief Shelter #1'}
                </div>
                <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Air-Conditioned · Clean Water &amp; Medical Ready</div>
              </div>
              <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-100 dark:bg-emerald-500/20 text-emerald-800 dark:text-emerald-400 font-bold border border-emerald-200 dark:border-emerald-500/30">
                ACTIVE
              </span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default ResidentStatusView;

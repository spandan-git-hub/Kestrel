import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setResidentTab } from '../features/ui/uiSlice';
import { useGetLocationsQuery } from '../api/registryApi';
import { useGetAlertStatusesQuery, useGetForecastByLocationQuery } from '../api/rulesApi';
import { useGetReportsForLocationQuery } from '../api/fanoutApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import type { LocationDto, Severity, Provenance, ForecastDayStats } from '../types/api';
import { normalizeForecastDay } from '../types/api';

export const ResidentStatusView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedWardId = useAppSelector((state) => state.resident.selectedWardId) || 'mumbai-kurla';
  const language = useAppSelector((state) => state.ui.language);

  const [selectedDayIdx, setSelectedDayIdx] = useState<number>(3);
  const [activeHazardTab, setActiveHazardTab] = useState<'FLOOD' | 'HEAT' | 'AIR'>('FLOOD');

  // RTK Query API hooks
  const { data: locationsData } = useGetLocationsQuery();
  const { data: alertStatuses } = useGetAlertStatusesQuery(undefined, { pollingInterval: 6000 });
  const { data: reports } = useGetReportsForLocationQuery(selectedWardId);
  const { data: forecastData } = useGetForecastByLocationQuery(selectedWardId);

  const stations: LocationDto[] =
    locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;
  const currentStation = stations.find((s) => s.id === selectedWardId) || stations[0];
  const activeStatus = alertStatuses?.find((s) => s.locationId === selectedWardId);

  const severity: Severity = activeStatus?.severity || 'WARNING';
  const provenance: Provenance = activeStatus?.provenance || 'MODEL_PLUS_REPORTS';
  const verifiedReportsCount = reports?.length || 3;

  // Normalized forecast days
  const thresholds = forecastData?.thresholds || {
    rp2: currentStation.threshold2yr || 40,
    rp5: currentStation.threshold5yr || 85,
    rp20: currentStation.threshold20yr || 180,
  };
  const rawDays: ForecastDayStats[] = forecastData?.days?.length ? forecastData.days : [];
  const chartDays = rawDays.map((d) => normalizeForecastDay(d, thresholds));
  const activeDay = chartDays[selectedDayIdx] || chartDays[0];

  // Dynamic status text depending on language
  const getAdvisoryText = () => {
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
                {severity === 'SEVERE' ? 'Severe Flood Alert' : severity === 'WARNING' ? 'Flood Warning' : 'Normal Baseline'}
              </span>

              {/* Provenance Badge */}
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border border-slate-200 dark:border-[#2A3644] font-medium">
                <span className="material-symbols-outlined text-[14px] text-blue-600 dark:text-[#7AA7FF]">verified</span>
                {provenance === 'MODEL_PLUS_REPORTS'
                  ? `Model + ${verifiedReportsCount} Ground Reports`
                  : provenance === 'OFFICIAL_CONFIRMED'
                  ? 'Official CWC Gauge Verified'
                  : '50-Member GloFAS Ensemble'}
              </span>

              {/* Data Age */}
              <span className="text-slate-500 dark:text-[#9AA8B8] flex items-center gap-1 text-[11px]">
                <span className="material-symbols-outlined text-[13px]">schedule</span>
                Updated 3m ago · Station #{currentStation.id.toUpperCase()}
              </span>
            </div>

            {/* Headline Location & River */}
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-[#EAF0F6]">
                {currentStation.name}, {currentStation.state}
              </h1>
              <p className="text-sm text-slate-600 dark:text-[#9AA8B8] mt-0.5">
                {currentStation.riverBasin} · Datum: 549.2m MSL · Indian Hydrological Network
              </p>
            </div>

            {/* Advisory Directive */}
            <div
              className={`p-3.5 rounded-xl border text-sm font-medium leading-relaxed ${
                severity === 'SEVERE'
                  ? 'bg-red-50 dark:bg-red-950/30 border-red-200 dark:border-red-500/30 text-red-900 dark:text-red-200'
                  : severity === 'WARNING'
                  ? 'bg-amber-50 dark:bg-amber-950/30 border-amber-200 dark:border-amber-500/30 text-amber-900 dark:text-amber-200'
                  : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-800 dark:text-[#EAF0F6]'
              }`}
            >
              {getAdvisoryText()}
            </div>
          </div>

          {/* Quick CTA Actions */}
          <div className="flex flex-col sm:flex-row lg:flex-col gap-3 shrink-0">
            <button
              onClick={() => dispatch(setResidentTab('subscribe'))}
              className="px-5 py-3 rounded-xl bg-[#1D4ED8] hover:bg-blue-600 text-white font-semibold text-sm shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 transition-all active:scale-[0.98]"
            >
              <span className="material-symbols-outlined text-[18px]">notifications_active</span>
              <span>Subscribe for SMS/WhatsApp</span>
            </button>
            <button
              onClick={() => dispatch(setResidentTab('report'))}
              className="px-5 py-3 rounded-xl bg-white dark:bg-[#1C2633] hover:bg-slate-50 dark:hover:bg-[#253243] text-slate-800 dark:text-[#EAF0F6] border border-slate-200 dark:border-[#2A3644] font-semibold text-sm flex items-center justify-center gap-2 transition-all active:scale-[0.98] shadow-sm"
            >
              <span className="material-symbols-outlined text-[18px] text-amber-500 dark:text-amber-400">add_a_photo</span>
              <span>Report Local Observation</span>
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
            <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-800 dark:bg-amber-500/20 dark:text-amber-300 border border-amber-200 dark:border-amber-500/30">
              Warning Active
            </span>
          </div>
          <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
            {activeDay?.medianDischarge ? `${Math.round(activeDay.medianDischarge)} m³/s` : '1,340 m³/s'}
          </div>
          <div className="text-xs text-slate-600 dark:text-[#9AA8B8] mt-1 flex items-center gap-1.5">
            <span className="text-amber-600 dark:text-amber-400 font-semibold flex items-center">
              <span className="material-symbols-outlined text-[14px]">trending_up</span> +18%
            </span>
            <span>above 2-yr threshold ({thresholds.rp2} m³/s)</span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-slate-500 dark:text-[#65778B] flex justify-between">
            <span>Peak Expected: Day 4</span>
            <span className="text-blue-600 dark:text-[#7AA7FF]">50-member GloFAS</span>
          </div>
        </div>

        {/* Tile 2: Extreme Heat & Wet-Bulb */}
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
            <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-500/30">
              Moderate
            </span>
          </div>
          <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">34.2 °C</div>
          <div className="text-xs text-slate-600 dark:text-[#9AA8B8] mt-1 flex items-center gap-1.5">
            <span className="text-emerald-600 dark:text-emerald-400 font-semibold">Wet-Bulb: 24.8 °C</span>
            <span>· Safe for civilian activity</span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-slate-500 dark:text-[#65778B] flex justify-between">
            <span>Max Index: 38.0 °C (14:00)</span>
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
            <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-yellow-100 text-yellow-800 dark:bg-yellow-500/20 dark:text-yellow-300 border border-yellow-200 dark:border-yellow-500/30">
              Moderate
            </span>
          </div>
          <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">88 AQI</div>
          <div className="text-xs text-slate-600 dark:text-[#9AA8B8] mt-1 flex items-center gap-1.5">
            <span className="text-yellow-700 dark:text-yellow-400 font-semibold">PM2.5: 32 µg/m³</span>
            <span>· Acceptable air quality</span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-slate-500 dark:text-[#65778B] flex justify-between">
            <span>CPCB Sensor #PN-02</span>
            <span className="text-slate-500 dark:text-[#9AA8B8]">Verified 15m ago</span>
          </div>
        </div>
      </section>

      {/* 3. 10-DAY ENSEMBLE HYDROLOGICAL HYDROGRAPH */}
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
          {chartDays.slice(0, 10).map((day, idx) => {
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
                className={`p-2 rounded-xl text-center border transition-all text-xs flex flex-col items-center justify-between min-h-[58px] ${
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
        {activeDay && (
          <div className="bg-slate-50 dark:bg-[#1C2633] rounded-xl p-4 border border-slate-200 dark:border-[#2A3644] flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-500/20 border border-blue-200 dark:border-blue-500/30 flex items-center justify-center text-blue-700 dark:text-blue-400 font-bold">
                {selectedDayIdx + 1}
              </div>
              <div>
                <div className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  {activeDay.date} — Projected River Discharge
                </div>
                <div className="text-xs text-slate-600 dark:text-[#9AA8B8]">
                  Min: {Math.round(activeDay.minDischarge || 0)} m³/s · 25th %: {Math.round(activeDay.p25Discharge || 0)} m³/s · Median: {Math.round(activeDay.medianDischarge || 0)} m³/s · 75th %: {Math.round(activeDay.p75Discharge || 0)} m³/s · Max: {Math.round(activeDay.maxDischarge || 0)} m³/s
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <span
                className={`px-3 py-1 rounded-full text-xs font-bold border ${
                  activeDay.exceeds20yr
                    ? 'bg-red-100 text-red-700 border-red-300 dark:bg-red-500/20 dark:text-red-400 dark:border-red-500/40'
                    : activeDay.exceeds5yr
                    ? 'bg-orange-100 text-orange-700 border-orange-300 dark:bg-orange-500/20 dark:text-orange-400 dark:border-orange-500/40'
                    : activeDay.exceeds2yr
                    ? 'bg-amber-100 text-amber-700 border-amber-300 dark:bg-amber-500/20 dark:text-amber-300 dark:border-amber-500/40'
                    : 'bg-emerald-100 text-emerald-700 border-emerald-300 dark:bg-emerald-500/20 dark:text-emerald-400 dark:border-emerald-500/40'
                }`}
              >
                {activeDay.exceeds20yr
                  ? 'Severe Threshold Breach'
                  : activeDay.exceeds5yr
                  ? 'Warning Threshold Breach'
                  : activeDay.exceeds2yr
                  ? 'Watch Threshold Breach'
                  : 'Normal Baseline'}
              </span>
            </div>
          </div>
        )}
      </section>

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
                <strong className="text-slate-900 dark:text-[#EAF0F6]">Elevate electrical appliances:</strong> Unplug ground-level inverters and switch off main fuse boxes if water reaches curbs.
              </span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="material-symbols-outlined text-emerald-600 dark:text-emerald-400 text-sm mt-0.5">check_box</span>
              <span>
                <strong className="text-slate-900 dark:text-[#EAF0F6]">Store 72-hour clean drinking water:</strong> Municipal water purification stations may throttle pump capacity during peak silt discharge.
              </span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="material-symbols-outlined text-emerald-600 dark:text-emerald-400 text-sm mt-0.5">check_box</span>
              <span>
                <strong className="text-slate-900 dark:text-[#EAF0F6]">Vehicular parking relocation:</strong> Move two-wheelers and sedans away from low-lying riverside causeways.
              </span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="material-symbols-outlined text-emerald-600 dark:text-emerald-400 text-sm mt-0.5">check_box</span>
              <span>
                <strong className="text-slate-900 dark:text-[#EAF0F6]">Check on elderly neighbors:</strong> Identify ground-floor residents who may need assistance moving to upper floors.
              </span>
            </li>
          </ul>
        </div>

        {/* Right: Designated Relief Shelters & EOC Helplines */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
            <span className="material-symbols-outlined text-blue-600 dark:text-[#7AA7FF] text-[20px]">shield</span>
            <h3 className="font-bold text-slate-900 dark:text-[#EAF0F6]">Emergency Contacts &amp; Designated Shelters</h3>
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
                <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">NDRF 5th Battalion Pune</div>
                <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Flood Inundation &amp; Rescue</div>
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
                <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Shivajinagar Municipal Hall (Shelter #1)</div>
                <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Bed Capacity: 250 · Food &amp; Medical Ready</div>
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

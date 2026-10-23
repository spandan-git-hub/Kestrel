import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from './app/hooks';
import { setRole, UserRole } from './features/auth/authSlice';
import { setLanguage, SupportedLanguage } from './features/ui/uiSlice';
import { setSelectedLocation } from './features/map/mapSlice';
import {
  SeverityBadge,
  ProvenanceBadge,
  DataAge,
  StaleBanner,
  ForecastBandChart,
} from './components';
import { useGetForecastByLocationQuery, useGetAlertStatusesQuery } from './api/rulesApi';
import { Shield, Radio, Activity, Globe, MapPin, Zap, RefreshCw, Layers } from 'lucide-react';
import type { Severity, Provenance, ForecastDayStats } from './types/api';

const PILOT_LOCATIONS = [
  { id: 'mumbai-kurla', name: 'Mumbai Kurla (Mithi)', basin: 'Mithi River Basin' },
  { id: 'patna-ward-12', name: 'Patna Ward 12 (Ganges)', basin: 'Ganges River Basin' },
  { id: 'guwahati-brahmaputra', name: 'Guwahati (Brahmaputra)', basin: 'Brahmaputra Basin' },
  { id: 'cuttack-mahanadi', name: 'Cuttack (Mahanadi)', basin: 'Mahanadi Delta' },
];

// Fallback demo forecast data if backend microservices are offline during UI inspection
const DEMO_FORECAST_DAYS: ForecastDayStats[] = [
  {
    date: '2026-10-24',
    medianDischarge: 45.2,
    p25Discharge: 38.0,
    p75Discharge: 54.1,
    maxDischarge: 62.0,
    exceeds2yr: false,
    exceeds5yr: false,
    exceeds20yr: false,
    exceedanceShare: 0.0,
  },
  {
    date: '2026-10-25',
    medianDischarge: 58.7,
    p25Discharge: 46.5,
    p75Discharge: 72.3,
    maxDischarge: 88.0,
    exceeds2yr: false,
    exceeds5yr: false,
    exceeds20yr: false,
    exceedanceShare: 0.08,
  },
  {
    date: '2026-10-26',
    medianDischarge: 112.4,
    p25Discharge: 92.1,
    p75Discharge: 135.0,
    maxDischarge: 165.2,
    exceeds2yr: true,
    exceeds5yr: false,
    exceeds20yr: false,
    exceedanceShare: 0.42,
  },
  {
    date: '2026-10-27',
    medianDischarge: 185.0,
    p25Discharge: 148.2,
    p75Discharge: 228.6,
    maxDischarge: 275.0,
    exceeds2yr: true,
    exceeds5yr: false,
    exceeds20yr: false,
    exceedanceShare: 0.68,
  },
  {
    date: '2026-10-28',
    medianDischarge: 278.4,
    p25Discharge: 215.0,
    p75Discharge: 342.1,
    maxDischarge: 395.0,
    exceeds2yr: true,
    exceeds5yr: true,
    exceeds20yr: false,
    exceedanceShare: 0.88,
  },
  {
    date: '2026-10-29',
    medianDischarge: 310.2,
    p25Discharge: 240.5,
    p75Discharge: 380.0,
    maxDischarge: 430.5,
    exceeds2yr: true,
    exceeds5yr: true,
    exceeds20yr: false,
    exceedanceShare: 0.92,
  },
  {
    date: '2026-10-30',
    medianDischarge: 230.1,
    p25Discharge: 185.0,
    p75Discharge: 285.4,
    maxDischarge: 340.0,
    exceeds2yr: true,
    exceeds5yr: false,
    exceeds20yr: false,
    exceedanceShare: 0.55,
  },
  {
    date: '2026-10-31',
    medianDischarge: 160.8,
    p25Discharge: 125.0,
    p75Discharge: 198.5,
    maxDischarge: 235.0,
    exceeds2yr: true,
    exceeds5yr: false,
    exceeds20yr: false,
    exceedanceShare: 0.25,
  },
  {
    date: '2026-11-01',
    medianDischarge: 95.3,
    p25Discharge: 75.0,
    p75Discharge: 120.0,
    maxDischarge: 145.0,
    exceeds2yr: false,
    exceeds5yr: false,
    exceeds20yr: false,
    exceedanceShare: 0.05,
  },
  {
    date: '2026-11-02',
    medianDischarge: 62.0,
    p25Discharge: 48.0,
    p75Discharge: 78.0,
    maxDischarge: 92.0,
    exceeds2yr: false,
    exceeds5yr: false,
    exceeds20yr: false,
    exceedanceShare: 0.0,
  },
];

export const App: React.FC = () => {
  const dispatch = useAppDispatch();
  const role = useAppSelector((state) => state.auth.role);
  const language = useAppSelector((state) => state.ui.language);
  const selectedLocation = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const simulationMode = useAppSelector((state) => state.ui.simulationMode);

  const [showStaleDemo, setShowStaleDemo] = useState(false);
  const [selectedDayIdx, setSelectedDayIdx] = useState<number | null>(4);

  // RTK Query hooks
  const { data: forecastData, isFetching: isForecastFetching, refetch: refetchForecast } =
    useGetForecastByLocationQuery(selectedLocation);
  const { data: alertStatuses } = useGetAlertStatusesQuery();

  const activeLocationStatus = alertStatuses?.find((s) => s.locationId === selectedLocation);
  const currentLocationMeta = PILOT_LOCATIONS.find((l) => l.id === selectedLocation) || PILOT_LOCATIONS[0];

  const chartDays = forecastData?.days?.length ? forecastData.days : DEMO_FORECAST_DAYS;
  const chartThresholds = forecastData?.thresholds || { rp2: 100, rp5: 250, rp20: 450 };

  return (
    <div className="min-h-screen bg-[#0b0f19] text-slate-100 flex flex-col font-sans">
      {/* Top Navigation */}
      <header className="border-b border-slate-800 bg-[#101726]/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-600/20 text-blue-400 rounded-lg border border-blue-500/30">
              <Radio className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-lg tracking-wider text-slate-100">KESTREL</span>
                <span className="text-xs px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20 font-medium">
                  PROTOTYPE
                </span>
              </div>
              <p className="text-xs text-slate-400">Multi-Hazard Alert Relay · India</p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {/* Language Selector */}
            <div className="flex items-center space-x-1 bg-slate-900 border border-slate-800 rounded-lg p-1 text-xs">
              <Globe className="w-3.5 h-3.5 text-slate-400 ml-1" />
              {(['en', 'hi', 'mr'] as SupportedLanguage[]).map((lang) => (
                <button
                  key={lang}
                  onClick={() => dispatch(setLanguage(lang))}
                  className={`px-2 py-1 rounded transition-colors ${
                    language === lang
                      ? 'bg-blue-600 text-white font-medium shadow-sm'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {lang.toUpperCase()}
                </button>
              ))}
            </div>

            {/* Role Switcher */}
            <div className="flex items-center space-x-1 bg-slate-900 border border-slate-800 rounded-lg p-1 text-xs">
              {(['COORDINATOR', 'RESIDENT'] as UserRole[]).map((r) => (
                <button
                  key={r}
                  onClick={() => dispatch(setRole(r))}
                  className={`px-2.5 py-1 rounded transition-colors font-medium ${
                    role === r
                      ? 'bg-emerald-600 text-white shadow-sm'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {r === 'COORDINATOR' ? 'Coordinator' : 'Resident'}
                </button>
              ))}
            </div>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 space-y-6">
        {/* Architecture & Phase Status */}
        <div className="bg-gradient-to-r from-blue-950/40 via-slate-900/60 to-purple-950/40 border border-blue-800/30 rounded-xl p-5 shadow-lg">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center space-x-2">
                <Shield className="w-4 h-4 text-emerald-400" />
                <h1 className="text-base font-semibold text-slate-100">
                  Phase 3: Design System Primitives & SVG Forecast Chart
                </h1>
              </div>
              <p className="text-sm text-slate-400">
                SeverityBadge · ProvenanceBadge · DataAge · StaleBanner · 10-Day Ensemble SVG Band Chart
              </p>
            </div>
            <div className="flex items-center space-x-3 text-xs">
              <span className="flex items-center px-3 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 font-medium">
                <span className="w-2 h-2 rounded-full bg-emerald-500 mr-2 animate-ping" />
                PT3-03 Implemented
              </span>
            </div>
          </div>
        </div>

        {/* Stale Data Warning Banner Demonstration */}
        <StaleBanner
          isStale={showStaleDemo || (activeLocationStatus?.isStale ?? false)}
          dataAgeSeconds={showStaleDemo ? 140000 : activeLocationStatus?.dataAgeSeconds}
          dataAsOf={activeLocationStatus?.dataAsOf || '2026-10-21T08:00:00Z'}
          language={language}
          onRefresh={() => refetchForecast()}
          isRefreshing={isForecastFetching}
        />

        {/* Design System Primitives Showcase */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Severity Badge Card */}
          <div className="bg-[#101726]/90 border border-slate-800 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                SeverityBadge (Color+Icon+Text)
              </span>
              <span className="text-[10px] text-slate-500 font-mono">PT3-03 §4.1</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {(['NONE', 'WATCH', 'WARNING', 'SEVERE'] as Severity[]).map((sev) => (
                <SeverityBadge key={sev} severity={sev} size="sm" language={language} />
              ))}
            </div>
          </div>

          {/* Provenance Badge Card */}
          <div className="bg-[#101726]/90 border border-slate-800 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                ProvenanceBadge (Origin)
              </span>
              <span className="text-[10px] text-slate-500 font-mono">PT3-03 §4.2</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {(['MODEL_ONLY', 'MODEL_PLUS_REPORTS', 'OFFICIAL_CONFIRMED'] as Provenance[]).map((prov) => (
                <ProvenanceBadge key={prov} provenance={prov} size="sm" language={language} />
              ))}
            </div>
          </div>

          {/* DataAge Freshness Card */}
          <div className="bg-[#101726]/90 border border-slate-800 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                DataAge (Freshness)
              </span>
              <span className="text-[10px] text-slate-500 font-mono">PT3-03 §4.3</span>
            </div>
            <div className="space-y-2">
              <div>
                <DataAge seconds={300} language={language} />
              </div>
              <div>
                <DataAge seconds={43200} language={language} />
              </div>
              <div>
                <DataAge seconds={135000} isStale={true} language={language} />
              </div>
            </div>
          </div>

          {/* Controls & Demonstration Toggles */}
          <div className="bg-[#101726]/90 border border-slate-800 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Interactive Controls
              </span>
              <button
                onClick={() => setShowStaleDemo(!showStaleDemo)}
                className={`text-[11px] px-2 py-0.5 rounded border transition-colors ${
                  showStaleDemo
                    ? 'bg-red-500/20 text-red-300 border-red-500/40'
                    : 'bg-slate-800 text-slate-400 border-slate-700'
                }`}
              >
                {showStaleDemo ? 'Stale: ON' : 'Stale: OFF'}
              </button>
            </div>
            <div className="text-xs text-slate-400 space-y-1">
              <div className="flex items-center justify-between">
                <span>Active Location:</span>
                <span className="font-medium text-slate-200">{currentLocationMeta.name.split(' ')[0]}</span>
              </div>
              <div className="flex items-center justify-between">
                <span>Simulation:</span>
                <span className="font-mono text-slate-300">{simulationMode || 'CALM'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Location Selector Bar */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center space-x-2 text-xs font-semibold text-slate-300">
            <MapPin className="w-4 h-4 text-emerald-400" />
            <span>SELECT PILOT RIVER BASIN:</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {PILOT_LOCATIONS.map((loc) => (
              <button
                key={loc.id}
                onClick={() => dispatch(setSelectedLocation(loc.id))}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
                  selectedLocation === loc.id
                    ? 'bg-blue-600/30 border-blue-500 text-blue-200 shadow-sm'
                    : 'bg-slate-800/60 border-slate-700 text-slate-400 hover:text-slate-200'
                }`}
              >
                {loc.name}
              </button>
            ))}
          </div>
        </div>

        {/* Forecast Band Chart Section */}
        <div className="space-y-2">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center space-x-2">
              <Layers className="w-4 h-4 text-blue-400" />
              <h2 className="text-sm font-semibold text-slate-200">
                Hydrological Ensemble Spread & Return Period Breach Chart
              </h2>
            </div>
            <button
              onClick={() => refetchForecast()}
              disabled={isForecastFetching}
              className="inline-flex items-center space-x-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isForecastFetching ? 'animate-spin' : ''}`} />
              <span>Refresh Forecast</span>
            </button>
          </div>

          <ForecastBandChart
            days={chartDays}
            thresholds={chartThresholds}
            locationName={currentLocationMeta.name}
            language={language}
            selectedDayIndex={selectedDayIdx}
            onSelectDay={(_day, idx) => setSelectedDayIdx(idx)}
          />
        </div>

        {/* State Monitor Grid */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
            <div className="flex items-center space-x-2 text-slate-400 text-xs">
              <Zap className="w-4 h-4 text-yellow-400" />
              <span>Current Role</span>
            </div>
            <div className="text-lg font-bold text-slate-100">{role}</div>
            <div className="text-xs text-slate-500">authSlice.role</div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
            <div className="flex items-center space-x-2 text-slate-400 text-xs">
              <Globe className="w-4 h-4 text-blue-400" />
              <span>Active Language</span>
            </div>
            <div className="text-lg font-bold text-slate-100 uppercase">{language}</div>
            <div className="text-xs text-slate-500">uiSlice.language</div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
            <div className="flex items-center space-x-2 text-slate-400 text-xs">
              <MapPin className="w-4 h-4 text-emerald-400" />
              <span>Selected Location</span>
            </div>
            <div className="text-lg font-bold text-slate-100 truncate">{selectedLocation || 'None'}</div>
            <div className="text-xs text-slate-500">mapSlice.selectedLocationId</div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
            <div className="flex items-center space-x-2 text-slate-400 text-xs">
              <Activity className="w-4 h-4 text-purple-400" />
              <span>Simulation Mode</span>
            </div>
            <div className="text-lg font-bold text-slate-100 truncate">{simulationMode || 'None'}</div>
            <div className="text-xs text-slate-500">uiSlice.simulationMode</div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default App;

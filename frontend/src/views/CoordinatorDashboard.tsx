import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation } from '../features/map/mapSlice';
import {
  MapView,
  ForecastBandChart,
  StaleBanner,
  ErrorBoundary,
} from '../components';
import { SimulationBar } from './SimulationBar';
import { BroadcastModal } from './BroadcastModal';
import { LocationCard } from './LocationCard';
import { useGetLocationsQuery } from '../api/registryApi';
import { useGetAlertStatusesQuery, useGetForecastByLocationQuery } from '../api/rulesApi';
import { useGetRecentDispatchesQuery } from '../api/fanoutApi';
import type { LocationDto, ForecastDayStats } from '../types/api';
import { normalizeForecastDay } from '../types/api';
import {
  ShieldAlert,
  Layers,
  Map as MapIcon,
  Activity,
  RefreshCw,
  Radio,
} from 'lucide-react';

const FALLBACK_LOCATIONS: LocationDto[] = [
  {
    id: 'mumbai-kurla',
    name: 'Kurla Ward L, Mumbai (Mithi)',
    state: 'Maharashtra',
    latitude: 19.0657,
    longitude: 72.8794,
    riverBasin: 'Mithi River Basin',
    threshold2yr: 40.0,
    threshold5yr: 85.0,
    threshold20yr: 180.0,
    primaryLanguage: 'mr',
    active: true,
  },
  {
    id: 'patna-ward12',
    name: 'Ward 12, Patna (Ganges)',
    state: 'Bihar',
    latitude: 25.6127,
    longitude: 85.1589,
    riverBasin: 'Ganges River Basin',
    threshold2yr: 15000.0,
    threshold5yr: 22000.0,
    threshold20yr: 32000.0,
    primaryLanguage: 'hi',
    active: true,
  },
  {
    id: 'guwahati-river',
    name: 'Riverside, Guwahati (Brahmaputra)',
    state: 'Assam',
    latitude: 26.1856,
    longitude: 91.7478,
    riverBasin: 'Brahmaputra Basin',
    threshold2yr: 25000.0,
    threshold5yr: 38000.0,
    threshold20yr: 55000.0,
    primaryLanguage: 'en',
    active: true,
  },
  {
    id: 'cuttack-delta',
    name: 'Cuttack Municipal Corp (Mahanadi)',
    state: 'Odisha',
    latitude: 20.4625,
    longitude: 85.8828,
    riverBasin: 'Mahanadi Delta',
    threshold2yr: 8000.0,
    threshold5yr: 14000.0,
    threshold20yr: 24000.0,
    primaryLanguage: 'en',
    active: true,
  },
];

// Fallback demo forecast days for inspection
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

export const CoordinatorDashboard: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedLocation = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const language = useAppSelector((state) => state.ui.language);

  const [selectedDayIdx, setSelectedDayIdx] = useState<number | null>(4);

  // RTK Query queries with polling
  const { data: locationsData } = useGetLocationsQuery();
  const { data: alertStatuses, isFetching: isStatusesFetching } = useGetAlertStatusesQuery(undefined, {
    pollingInterval: 5000,
  });
  const {
    data: forecastData,
    isFetching: isForecastFetching,
    refetch: refetchForecast,
  } = useGetForecastByLocationQuery(selectedLocation);
  const { data: recentDispatches } = useGetRecentDispatchesQuery(10, {
    pollingInterval: 3000,
  });

  const locations = locationsData && locationsData.length > 0 ? locationsData : FALLBACK_LOCATIONS;
  const activeStatus = alertStatuses?.find((s) => s.locationId === selectedLocation);
  const selectedLocationMeta = locations.find((l) => l.id === selectedLocation) || locations[0];

  const rawDays = forecastData?.days?.length ? forecastData.days : DEMO_FORECAST_DAYS;
  const chartThresholds = forecastData?.thresholds || {
    rp2: selectedLocationMeta.threshold2yr || 40,
    rp5: selectedLocationMeta.threshold5yr || 85,
    rp20: selectedLocationMeta.threshold20yr || 180,
  };
  const chartDays = rawDays.map((d) => normalizeForecastDay(d, chartThresholds));

  // KPI Calculations
  const activeElevatedCount =
    alertStatuses?.filter((s) => s.severity === 'WATCH' || s.severity === 'WARNING' || s.severity === 'SEVERE').length ||
    0;

  return (
    <div className="space-y-6">
      {/* Sticky Top Simulation Bar */}
      <SimulationBar />

      {/* Coordinator KPI Summary Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {/* Total Basins */}
        <div className="bg-[#101726]/90 border border-slate-800 rounded-2xl p-4 space-y-1 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="font-semibold uppercase tracking-wider">Pilot Basins</span>
            <Layers className="w-4 h-4 text-blue-400" />
          </div>
          <div className="text-2xl font-bold text-slate-100">{locations.length}</div>
          <div className="text-[11px] text-slate-500">Mithi, Ganges, Brahmaputra, Mahanadi</div>
        </div>

        {/* Elevated Risk Count */}
        <div className="bg-[#101726]/90 border border-slate-800 rounded-2xl p-4 space-y-1 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="font-semibold uppercase tracking-wider">Elevated Hazards</span>
            <ShieldAlert className={`w-4 h-4 ${activeElevatedCount > 0 ? 'text-orange-400 animate-pulse' : 'text-emerald-400'}`} />
          </div>
          <div className={`text-2xl font-bold ${activeElevatedCount > 0 ? 'text-orange-400' : 'text-emerald-400'}`}>
            {activeElevatedCount} <span className="text-xs font-normal text-slate-400">/ {locations.length}</span>
          </div>
          <div className="text-[11px] text-slate-500">Watch, Warning or Severe active</div>
        </div>

        {/* Live Dispatches Count */}
        <div className="bg-[#101726]/90 border border-slate-800 rounded-2xl p-4 space-y-1 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="font-semibold uppercase tracking-wider">Dispatches Logged</span>
            <Radio className="w-4 h-4 text-purple-400" />
          </div>
          <div className="text-2xl font-bold text-slate-100">{recentDispatches?.length ?? 0}</div>
          <div className="text-[11px] text-slate-500">Telegram, WhatsApp & Mock feed</div>
        </div>

        {/* Engine Link Status */}
        <div className="bg-[#101726]/90 border border-slate-800 rounded-2xl p-4 space-y-1 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="font-semibold uppercase tracking-wider">Rules Engine</span>
            <Activity className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="flex items-center space-x-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
            <span className="text-sm font-bold text-emerald-400">Synchronized</span>
          </div>
          <div className="text-[11px] text-slate-500 flex items-center space-x-1">
            <span>5s RTK Query poll</span>
            {isStatusesFetching && <RefreshCw className="w-2.5 h-2.5 animate-spin text-slate-400 ml-1" />}
          </div>
        </div>
      </div>

      {/* Stale Warning Banner if active location is stale */}
      <StaleBanner
        isStale={activeStatus?.isStale ?? false}
        dataAgeSeconds={activeStatus?.dataAgeSeconds}
        dataAsOf={activeStatus?.dataAsOf || undefined}
        language={language}
        onRefresh={() => refetchForecast()}
        isRefreshing={isForecastFetching}
      />

      {/* Main Grid: Map & Location Detail Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Interactive Leaflet Map (7 cols) */}
        <div className="lg:col-span-7 space-y-3">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center space-x-2">
              <MapIcon className="w-4 h-4 text-emerald-400" />
              <h3 className="text-sm font-bold text-slate-100">
                Interactive Pilot Hazard Map
              </h3>
            </div>
            <span className="text-xs text-slate-400">
              Select marker to inspect forecast & broadcast
            </span>
          </div>

          <MapView
            height={460}
            onSelectLocation={(id) => dispatch(setSelectedLocation(id))}
          />
        </div>

        {/* Right: Location Detail Cards Grid (5 cols) */}
        <div className="lg:col-span-5 space-y-3">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center space-x-2">
              <Activity className="w-4 h-4 text-blue-400" />
              <h3 className="text-sm font-bold text-slate-100">
                Pilot Locations & Thresholds
              </h3>
            </div>
            <span className="text-xs text-slate-400">One-tap relay triggers</span>
          </div>

          <div className="space-y-3 max-h-[460px] overflow-y-auto pr-1">
            {locations.map((loc) => {
              const status = alertStatuses?.find((s) => s.locationId === loc.id);
              return (
                <LocationCard
                  key={loc.id}
                  location={loc}
                  status={status}
                  isSelected={selectedLocation === loc.id}
                  language={language}
                />
              );
            })}
          </div>
        </div>
      </div>

      {/* Forecast Band Chart Section for Selected Basin */}
      <div className="space-y-3">
        <div className="flex items-center justify-between px-1">
          <div className="flex items-center space-x-2">
            <Layers className="w-4 h-4 text-blue-400" />
            <h3 className="text-sm font-bold text-slate-100">
              10-Day Hydrological Ensemble Spread · {selectedLocationMeta.name}
            </h3>
          </div>
          <button
            onClick={() => refetchForecast()}
            disabled={isForecastFetching}
            className="inline-flex items-center space-x-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isForecastFetching ? 'animate-spin' : ''}`} />
            <span>Refresh Forecast Data</span>
          </button>
        </div>

        <ErrorBoundary fallbackTitle="Forecast Chart Temporarily Unavailable">
          <ForecastBandChart
            days={chartDays}
            thresholds={chartThresholds}
            locationName={selectedLocationMeta.name}
            language={language}
            selectedDayIndex={selectedDayIdx !== null && selectedDayIdx < chartDays.length ? selectedDayIdx : 0}
            onSelectDay={(_day, idx) => setSelectedDayIdx(idx)}
          />
        </ErrorBoundary>
      </div>

      {/* Broadcast Modal Dialog */}
      <BroadcastModal />
    </div>
  );
};

export default CoordinatorDashboard;

import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation } from '../features/map/mapSlice';
import {
  MapView,
  ForecastBandChart,
  StaleBanner,
  ErrorBoundary,
} from '../components';
import { LiveSyncBar } from './LiveSyncBar';
import { BroadcastModal } from './BroadcastModal';
import { LocationCard } from './LocationCard';
import { useGetLocationsQuery } from '../api/registryApi';
import { useGetAlertStatusesQuery, useGetForecastByLocationQuery } from '../api/rulesApi';
import { useGetRecentDispatchesQuery } from '../api/fanoutApi';
import type { LocationDto, ForecastDayStats } from '../types/api';
import { normalizeForecastDay } from '../types/api';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import {
  ShieldAlert,
  Layers,
  Map as MapIcon,
  Activity,
  RefreshCw,
  Radio,
} from 'lucide-react';

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
  const { data: recentDispatches } = useGetRecentDispatchesQuery(20, {
    pollingInterval: 3000,
  });

  const locations: LocationDto[] =
    locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;
  const activeStatus = alertStatuses?.find((s) => s.locationId === selectedLocation);
  const selectedLocationMeta = locations.find((l) => l.id === selectedLocation) || locations[0];

  const rawDays: ForecastDayStats[] = forecastData?.days?.length ? forecastData.days : [];
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
      {/* Sticky Top Live Sync Bar */}
      <LiveSyncBar />

      {/* Coordinator KPI Summary Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {/* Total Basins */}
        <div className="bg-[#101726]/90 border border-slate-800 rounded-2xl p-4 space-y-1 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="font-semibold uppercase tracking-wider">Monitored Rivers</span>
            <Layers className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-2xl font-bold text-slate-100">{locations.length}</div>
          <div className="text-[11px] text-slate-500">All 10 Indian Hydrological Basins</div>
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
          <div className="text-[11px] text-slate-500">Telegram, Web Push & WhatsApp feeds</div>
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
                Interactive All-India Hazard Map
              </h3>
            </div>
            <span className="text-xs text-slate-400">
              Select station to inspect forecast & broadcast
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
              <Activity className="w-4 h-4 text-cyan-400" />
              <h3 className="text-sm font-bold text-slate-100">
                Monitored River Stations & Thresholds
              </h3>
            </div>
            <span className="text-xs text-slate-400">Live consensus & telemetry</span>
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
            <Layers className="w-4 h-4 text-cyan-400" />
            <h3 className="text-sm font-bold text-slate-100">
              10-Day Hydrological Ensemble Spread (p₁₀–p₉₀) · {selectedLocationMeta.name}
            </h3>
          </div>
          <button
            onClick={() => refetchForecast()}
            disabled={isForecastFetching}
            className="inline-flex items-center space-x-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors cursor-pointer disabled:opacity-50"
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
            dataAsOf={activeStatus?.dataAsOf}
            dataAgeSeconds={activeStatus?.dataAgeSeconds}
            isStale={activeStatus?.isStale}
            memberCount={forecastData?.memberCount ?? 51}
            onRefresh={() => refetchForecast()}
            isRefreshing={isForecastFetching}
          />
        </ErrorBoundary>
      </div>

      {/* Broadcast Modal Dialog */}
      <BroadcastModal />
    </div>
  );
};

export default CoordinatorDashboard;

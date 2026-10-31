import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation, setSelectedBasin } from '../features/map/mapSlice';
import { setCoordinatorTab } from '../features/ui/uiSlice';
import { MapView, BASIN_OPTIONS } from '../components/MapView';
import { useGetLocationsQuery } from '../api/registryApi';
import { useGetAlertStatusesQuery, useGetForecastByLocationQuery } from '../api/rulesApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import type { LocationDto } from '../types/api';

export const CoordinatorOverviewMapView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedLocation = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const selectedBasin = useAppSelector((state) => state.map.selectedBasin) || 'ALL';

  const [isInspectorOpen, setIsInspectorOpen] = useState<boolean>(true);

  // RTK Query API hooks
  const { data: locationsData } = useGetLocationsQuery();
  const { data: alertStatuses } = useGetAlertStatusesQuery(undefined, { pollingInterval: 5000 });
  const { data: forecastData } = useGetForecastByLocationQuery(selectedLocation);

  const stations: LocationDto[] =
    locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;
  const currentStation = stations.find((s) => s.id === selectedLocation) || stations[0];
  const activeStatus = alertStatuses?.find((s) => s.locationId === selectedLocation);

  const severity = activeStatus?.severity || 'WARNING';
  const provenance = activeStatus?.provenance || 'MODEL_PLUS_REPORTS';

  const elevatedCount =
    alertStatuses?.filter((s) => s.severity === 'WATCH' || s.severity === 'WARNING' || s.severity === 'SEVERE')
      .length || 2;

  return (
    <div className="relative w-full h-[calc(100vh-130px)] flex flex-col overflow-hidden rounded-2xl border border-slate-200 dark:border-[#2A3644] bg-white dark:bg-[#0B1118]">
      {/* 1. UPSTREAM INGESTION WARNING BANNER (STITCH AMBER BANNER) */}
      <div className="bg-amber-50 dark:bg-[#2A200B] border-b border-amber-300 dark:border-[#FACC15]/40 px-4 py-2 flex items-center justify-between shrink-0 z-30">
        <div className="flex items-center gap-2.5 text-xs text-amber-900 dark:text-[#FACC15]">
          <div className="w-5 h-5 rounded-full bg-amber-200/80 dark:bg-[#3B2C0D] flex items-center justify-center shrink-0 border border-amber-300 dark:border-[#FACC15]/30">
            <span className="material-symbols-outlined text-sm text-amber-800 dark:text-[#FACC15]">warning</span>
          </div>
          <span className="font-medium text-[12px]">
            <strong className="font-bold text-amber-950 dark:text-[#FEF08A]">Upstream Ingestion Notice:</strong> Open-Meteo flood feed delayed — data may be out of date.
          </span>
          <span className="hidden sm:inline text-[11px] text-amber-900 dark:text-[#FACC15] bg-amber-100 dark:bg-[#3B2C0D] px-2 py-0.5 rounded border border-amber-300 dark:border-[#FACC15]/40">
            Last cycle: 3h 14m ago
          </span>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => dispatch(setCoordinatorTab('feeds'))}
            className="text-xs text-amber-800 dark:text-[#FEF08A] hover:underline font-medium"
          >
            View feed telemetry
          </button>
        </div>
      </div>

      {/* 2. BASIN FILTER TOOLBAR */}
      <div className="bg-white dark:bg-[#151D27] border-b border-slate-200 dark:border-[#2A3644] px-4 py-2.5 flex items-center justify-between shrink-0 z-20 gap-3 overflow-x-auto no-scrollbar">
        <div className="flex items-center gap-2 text-xs font-semibold">
          <span className="text-slate-500 dark:text-[#9AA8B8] uppercase tracking-wider text-[11px] shrink-0">Basin Filter:</span>
          {BASIN_OPTIONS.slice(0, 6).map((basin) => {
            const isActive = selectedBasin === basin.id;
            return (
              <button
                key={basin.id}
                onClick={() => dispatch(setSelectedBasin(basin.id))}
                className={`px-3 py-1 rounded-xl text-xs whitespace-nowrap transition-all border ${
                  isActive
                    ? 'bg-[#1D4ED8] text-white border-blue-500 font-bold shadow-xs'
                    : 'bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
                }`}
              >
                {basin.label}
              </button>
            );
          })}
        </div>

        <div className="flex items-center gap-2 text-xs text-slate-600 dark:text-[#9AA8B8] shrink-0">
          <span className="px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-500/20 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-500/30 font-bold">
            {elevatedCount} Elevated
          </span>
          <span className="text-slate-400 dark:text-[#65778B]">| {stations.length} Total</span>
        </div>
      </div>

      {/* 3. MAIN INTERACTIVE MAP CANVAS */}
      <div className="relative flex-1 w-full h-full overflow-hidden">
        {/* Leaflet MapView */}
        <MapView
          height="100%"
          className="w-full h-full z-10"
          onSelectLocation={(locId) => {
            dispatch(setSelectedLocation(locId));
            setIsInspectorOpen(true);
          }}
        />

        {/* 4. RIGHT-HAND TELEMETRY INSPECTOR DRAWER */}
        {isInspectorOpen && currentStation && (
          <div className="absolute right-4 top-4 bottom-4 w-80 sm:w-96 bg-white/95 dark:bg-[#151D27]/95 backdrop-blur-md border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 shadow-2xl z-20 flex flex-col justify-between overflow-y-auto custom-scrollbar animate-in slide-in-from-right-4">
            <div className="space-y-4">
              {/* Header */}
              <div className="flex items-start justify-between pb-3 border-b border-slate-200 dark:border-[#2A3644]">
                <div>
                  <div className="flex items-center gap-2">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                        severity === 'SEVERE'
                          ? 'bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-400 border border-red-200 dark:border-red-500/40'
                          : severity === 'WARNING'
                          ? 'bg-amber-100 dark:bg-amber-500/20 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-500/40'
                          : 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/40'
                      }`}
                    >
                      {severity}
                    </span>
                    <span className="text-[11px] text-slate-600 dark:text-[#9AA8B8] bg-slate-100 dark:bg-[#1C2633] px-2 py-0.5 rounded border border-slate-200 dark:border-[#2A3644]">
                      {provenance === 'MODEL_PLUS_REPORTS'
                        ? 'Model + Reports'
                        : provenance === 'OFFICIAL_CONFIRMED'
                        ? 'CWC Verified'
                        : 'Model Only'}
                    </span>
                  </div>
                  <h3 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6] mt-1">{currentStation.name}</h3>
                  <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
                    {currentStation.riverBasin} · {currentStation.state}
                  </p>
                </div>
                <button
                  onClick={() => setIsInspectorOpen(false)}
                  className="text-slate-400 hover:text-slate-700 dark:text-[#9AA8B8] dark:hover:text-white"
                >
                  <span className="material-symbols-outlined text-sm">close</span>
                </button>
              </div>

              {/* Real-time Telemetry Metrics */}
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644]">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8] block">Discharge Rate</span>
                  <span className="text-xl font-bold text-slate-900 dark:text-[#EAF0F6]">
                    {forecastData?.days?.[3]?.medianDischarge
                      ? `${Math.round(forecastData.days[3].medianDischarge)} m³/s`
                      : '1,340 m³/s'}
                  </span>
                  <span className="text-[10px] text-amber-600 dark:text-amber-400 font-semibold block mt-0.5">
                    ▲ Approaching 5-yr
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644]">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8] block">Return Threshold</span>
                  <span className="text-xl font-bold text-slate-900 dark:text-[#EAF0F6]">
                    {currentStation.threshold5yr || 85} m³/s
                  </span>
                  <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block mt-0.5">5-Yr Warning Limit</span>
                </div>
              </div>

              {/* Sensor Data Age Line */}
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-600 dark:text-[#9AA8B8] space-y-1">
                <div className="flex justify-between">
                  <span>Sensor Node ID:</span>
                  <span className="font-mono text-slate-900 dark:text-[#EAF0F6]">#{currentStation.id.toUpperCase()}</span>
                </div>
                <div className="flex justify-between">
                  <span>Coordinates:</span>
                  <span className="font-mono text-slate-900 dark:text-[#EAF0F6]">
                    {currentStation.latitude.toFixed(2)}°N, {currentStation.longitude.toFixed(2)}°E
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Last Ingest:</span>
                  <span className="text-emerald-600 dark:text-emerald-400 font-semibold">3m ago (GloFAS v4.0)</span>
                </div>
              </div>
            </div>

            {/* CTAs */}
            <div className="space-y-2 pt-4 border-t border-slate-200 dark:border-[#2A3644]">
              <button
                onClick={() => dispatch(setCoordinatorTab('point-detail'))}
                className="w-full py-2.5 rounded-xl bg-[#1D4ED8] hover:bg-blue-600 text-white font-bold text-xs shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 transition-all active:scale-[0.98]"
              >
                <span className="material-symbols-outlined text-sm">ssid_chart</span>
                <span>Inspect Point Detail (Flood)</span>
              </button>

              <button
                onClick={() => dispatch(setCoordinatorTab('broadcast'))}
                className="w-full py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-slate-800 dark:text-[#EAF0F6] border border-slate-200 dark:border-[#2A3644] font-semibold text-xs flex items-center justify-center gap-2 transition-all"
              >
                <span className="material-symbols-outlined text-sm text-amber-500 dark:text-amber-400">cell_tower</span>
                <span>Draft Citizen Broadcast</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

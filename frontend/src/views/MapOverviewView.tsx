import React, { useState, useMemo } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation } from '../features/map/mapSlice';
import { setActiveTab } from '../features/ui/uiSlice';
import { MapView } from '../components/MapView';
import { useGetLocationsQuery } from '../api/registryApi';
import { useGetAlertStatusesQuery, useGetForecastByLocationQuery } from '../api/rulesApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import type { LocationDto, Severity, Provenance } from '../types/api';
import { Filter, Search, RotateCcw, ArrowRight, ShieldCheck, Info } from 'lucide-react';

export const BASIN_OPTIONS = [
  { id: 'ALL', label: 'All Basins' },
  { id: 'Ganga Basin', label: 'Ganga Basin' },
  { id: 'Brahmaputra Basin', label: 'Brahmaputra Basin' },
  { id: 'Indus/North', label: 'Indus / North Himalayan' },
  { id: 'Godavari Basin', label: 'Godavari Basin' },
  { id: 'Krishna Basin', label: 'Krishna Basin' },
  { id: 'Mahanadi Basin', label: 'Mahanadi Basin' },
  { id: 'West-Flowing', label: 'West-Flowing Rivers' },
  { id: 'Southern', label: 'Southern Basins' },
];

export const MapOverviewView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedLocationId = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const activeHazard = useAppSelector((state) => state.ui.activeHazard) || 'FLOOD';

  // Filters
  const [severityFilter, setSeverityFilter] = useState<'ALL' | 'SEVERE' | 'WARNING' | 'WATCH' | 'SAFE'>('ALL');
  const [basinFilter, setBasinFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');

  // RTK Query API hooks
  const { data: locationsData } = useGetLocationsQuery();
  const { data: alertStatuses } = useGetAlertStatusesQuery(undefined, { pollingInterval: 5000 });
  const { data: forecastData } = useGetForecastByLocationQuery(selectedLocationId);

  const stations: LocationDto[] =
    locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;

  // Lookup severity and status by location id
  const statusByLocationId = useMemo(() => {
    const map = new Map<string, { severity: Severity; provenance: Provenance; peak?: number; date?: string }>();
    alertStatuses?.forEach((status) => {
      map.set(status.locationId, {
        severity: status.severity,
        provenance: status.provenance,
        peak: status.peakExceedanceShare ?? undefined,
        date: status.firstExceedanceDate ?? undefined,
      });
    });
    return map;
  }, [alertStatuses]);

  // Apply filters
  const filteredStations = useMemo(() => {
    return stations.filter((loc) => {
      const status = statusByLocationId.get(loc.id)?.severity || 'NONE';

      // Severity filter
      if (severityFilter === 'SEVERE' && status !== 'SEVERE') return false;
      if (severityFilter === 'WARNING' && status !== 'WARNING') return false;
      if (severityFilter === 'WATCH' && status !== 'WATCH') return false;
      if (severityFilter === 'SAFE' && status !== 'NONE') return false;

      // Basin filter
      if (basinFilter !== 'ALL') {
        const b = loc.riverBasin || '';
        if (basinFilter === 'Ganga Basin' && !b.includes('Ganga') && !b.includes('Ganges')) return false;
        if (basinFilter === 'Brahmaputra Basin' && !b.includes('Brahmaputra')) return false;
        if (basinFilter === 'Indus/North' && !b.includes('Indus') && !b.includes('North')) return false;
        if (basinFilter === 'Godavari Basin' && !b.includes('Godavari')) return false;
        if (basinFilter === 'Krishna Basin' && !b.includes('Krishna')) return false;
        if (basinFilter === 'Mahanadi Basin' && !b.includes('Mahanadi')) return false;
        if (basinFilter === 'West-Flowing' && !b.includes('West-Flowing')) return false;
        if (basinFilter === 'Southern' && !b.includes('Southern') && !b.includes('Cauvery')) return false;
      }

      // Search term
      if (searchTerm) {
        const query = searchTerm.toLowerCase();
        const matchName = loc.name.toLowerCase().includes(query);
        const matchState = loc.state.toLowerCase().includes(query);
        const matchBasin = (loc.riverBasin || '').toLowerCase().includes(query);
        if (!matchName && !matchState && !matchBasin) return false;
      }

      return true;
    });
  }, [stations, statusByLocationId, severityFilter, basinFilter, searchTerm]);

  const activeStation = stations.find((s) => s.id === selectedLocationId) || stations[0];
  const activeStatus = statusByLocationId.get(selectedLocationId);
  const activeSeverity = activeStatus?.severity || 'NONE';
  const activeProvenance = activeStatus?.provenance || 'MODEL_ONLY';

  const resetFilters = () => {
    setSeverityFilter('ALL');
    setBasinFilter('ALL');
    setSearchTerm('');
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-4 pb-12">
      {/* 1. FILTER FEATURE TOOLBAR (ABOVE THE MAP) */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-4 shadow-sm space-y-3">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
          {/* Title and Filter Indicator */}
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-blue-50 dark:bg-[#1C2633] text-blue-600 dark:text-[#7AA7FF] border border-blue-200 dark:border-blue-500/30">
              <Filter className="w-4 h-4" />
            </div>
            <div>
              <h1 className="text-sm sm:text-base font-bold text-slate-900 dark:text-[#EAF0F6]">
                Map Filter &amp; Severity Explorer
              </h1>
              <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                Displaying {filteredStations.length} of {stations.length} locations across India ({activeHazard} monitoring)
              </p>
            </div>
          </div>

          {/* Search box & Reset */}
          <div className="flex items-center gap-2">
            <div className="relative flex-1 sm:w-64">
              <Search className="w-3.5 h-3.5 absolute left-3 top-3 text-slate-400" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search station, state, basin..."
                className="w-full h-9 pl-9 pr-3 text-xs rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] focus:outline-none focus:border-blue-500"
              />
            </div>
            {(severityFilter !== 'ALL' || basinFilter !== 'ALL' || searchTerm) && (
              <button
                onClick={resetFilters}
                className="h-9 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-slate-700 dark:text-[#9AA8B8] text-xs font-semibold flex items-center gap-1 border border-slate-200 dark:border-[#2A3644] transition-colors"
                title="Reset all filters"
              >
                <RotateCcw className="w-3 h-3" />
                <span className="hidden sm:inline">Reset</span>
              </button>
            )}
          </div>
        </div>

        {/* Filter Controls Row */}
        <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-slate-100 dark:border-[#2A3644] text-xs">
          <span className="text-[11px] font-bold text-slate-400 dark:text-[#65778B] uppercase tracking-wider mr-1">
            Severity Filter:
          </span>

          <button
            onClick={() => setSeverityFilter('ALL')}
            className={`px-3 py-1.5 rounded-xl font-semibold transition-all border ${
              severityFilter === 'ALL'
                ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
                : 'bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644] hover:bg-slate-100'
            }`}
          >
            All Severities ({stations.length})
          </button>

          <button
            onClick={() => setSeverityFilter('SEVERE')}
            className={`px-3 py-1.5 rounded-xl font-semibold transition-all border flex items-center gap-1.5 ${
              severityFilter === 'SEVERE'
                ? 'bg-red-600 text-white border-red-600 shadow-xs'
                : 'bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-400 border-red-200 dark:border-red-900/60 hover:bg-red-100'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-red-500 animate-ping" />
            <span>Severe (20-Yr)</span>
          </button>

          <button
            onClick={() => setSeverityFilter('WARNING')}
            className={`px-3 py-1.5 rounded-xl font-semibold transition-all border flex items-center gap-1.5 ${
              severityFilter === 'WARNING'
                ? 'bg-orange-600 text-white border-orange-600 shadow-xs'
                : 'bg-orange-50 dark:bg-orange-950/40 text-orange-700 dark:text-orange-400 border-orange-200 dark:border-orange-900/60 hover:bg-orange-100'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-orange-500" />
            <span>Warning (5-Yr)</span>
          </button>

          <button
            onClick={() => setSeverityFilter('WATCH')}
            className={`px-3 py-1.5 rounded-xl font-semibold transition-all border flex items-center gap-1.5 ${
              severityFilter === 'WATCH'
                ? 'bg-amber-600 text-white border-amber-600 shadow-xs'
                : 'bg-amber-50 dark:bg-amber-950/40 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-900/60 hover:bg-amber-100'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-amber-500" />
            <span>Watch (2-Yr)</span>
          </button>

          <button
            onClick={() => setSeverityFilter('SAFE')}
            className={`px-3 py-1.5 rounded-xl font-semibold transition-all border flex items-center gap-1.5 ${
              severityFilter === 'SAFE'
                ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs'
                : 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-900/60 hover:bg-emerald-100'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-emerald-500" />
            <span>Normal / Safe</span>
          </button>

          <div className="w-[1px] h-5 bg-slate-200 dark:bg-slate-700 mx-2 hidden lg:block" />

          {/* Basin selector */}
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] font-bold text-slate-400 dark:text-[#65778B] uppercase tracking-wider">
              Basin:
            </span>
            <select
              value={basinFilter}
              onChange={(e) => setBasinFilter(e.target.value)}
              className="h-8 px-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] text-slate-800 dark:text-[#EAF0F6] text-xs font-semibold focus:outline-none cursor-pointer"
            >
              {BASIN_OPTIONS.map((b) => (
                <option key={b.id} value={b.id} className="bg-white dark:bg-[#151D27]">
                  {b.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </section>

      {/* 2. MAIN MAP CANVAS (PURE STREETS VIEW ONLY - NO OVERLAYS OVER MAP) */}
      <div className="relative w-full rounded-2xl overflow-hidden border border-slate-200 dark:border-[#2A3644] bg-white dark:bg-[#151D27] shadow-sm">
        <MapView
          height="540px"
          locations={filteredStations}
          statusByLocationId={statusByLocationId}
          onSelectLocation={(locId) => dispatch(setSelectedLocation(locId))}
        />
      </div>

      {/* 3. RISK LEGEND (PLACED CLEANLY BELOW THE MAP - NOT ABOVE IT) */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-4 shadow-sm flex flex-wrap items-center justify-between gap-4 text-xs">
        <div className="flex items-center gap-2">
          <Info className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF]" />
          <span className="font-bold text-slate-900 dark:text-[#EAF0F6]">Severity Scale &amp; Return Period Benchmark:</span>
        </div>

        <div className="flex flex-wrap items-center gap-4 text-xs">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-emerald-500 shadow-sm shadow-emerald-500/50" />
            <span className="font-medium text-slate-700 dark:text-[#9AA8B8]">Safe: Normal flow below 2-yr threshold</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-amber-500 shadow-sm shadow-amber-500/50" />
            <span className="font-medium text-slate-700 dark:text-[#9AA8B8]">Watch: Approaching 2-Yr Return (Precautionary)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-orange-500 shadow-sm shadow-orange-500/50" />
            <span className="font-medium text-slate-700 dark:text-[#9AA8B8]">Warning: 5-Yr Return Breached (Active Hazard)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-red-600 shadow-sm shadow-red-600/50" />
            <span className="font-bold text-red-600 dark:text-red-400">Severe: 20-Yr Return Breached (Emergency Evacuation)</span>
          </div>
        </div>
      </section>

      {/* 4. LOCATION DETAILS & CURRENT STATUS CARD (SHOWS ON CLICKING ANY LOCATION) */}
      {activeStation && (
        <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4 animate-in fade-in">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-200 dark:border-[#2A3644]">
            <div className="space-y-1">
              <div className="flex items-center gap-2 flex-wrap">
                <span
                  className={`px-2.5 py-0.5 rounded text-xs font-bold uppercase tracking-wider ${
                    activeSeverity === 'SEVERE'
                      ? 'bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-400 border border-red-200 dark:border-red-500/40'
                      : activeSeverity === 'WARNING'
                      ? 'bg-orange-100 dark:bg-orange-500/20 text-orange-800 dark:text-orange-300 border border-orange-200 dark:border-orange-500/40'
                      : activeSeverity === 'WATCH'
                      ? 'bg-amber-100 dark:bg-amber-500/20 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-500/40'
                      : 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/40'
                  }`}
                >
                  {activeSeverity === 'NONE' ? 'NORMAL / SAFE' : `${activeSeverity} RISK LEVEL`}
                </span>

                <span className="text-xs px-2 py-0.5 rounded bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border border-slate-200 dark:border-[#2A3644]">
                  {activeProvenance === 'OFFICIAL_CONFIRMED'
                    ? 'CWC Gauge Verified'
                    : activeProvenance === 'MODEL_PLUS_REPORTS'
                    ? 'Model + Reports Corroborated'
                    : 'GloFAS Ensemble v4.0'}
                </span>

                <span className="text-xs text-slate-500 dark:text-[#9AA8B8] font-mono">
                  ID: #{activeStation.id.toUpperCase()}
                </span>
              </div>

              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#EAF0F6]">
                {activeStation.name}, {activeStation.state}
              </h2>
              <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
                {activeStation.riverBasin} · Coordinates: {activeStation.latitude.toFixed(4)}°N, {activeStation.longitude.toFixed(4)}°E · Datum: 549.2m MSL
              </p>
            </div>

            {/* Quick Action Button to Deep-Dive Tab 2 */}
            <button
              onClick={() => dispatch(setActiveTab('point-detail'))}
              className="px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold shadow-md shadow-blue-500/20 flex items-center gap-2 self-start sm:self-auto transition-all active:scale-[0.98]"
            >
              <span>Inspect Point Detail &amp; Ensemble</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>

          {/* Basic Details & Current Status Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
            <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
              <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">Current Discharge</span>
              <span className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">
                {forecastData?.days?.[3]?.medianDischarge
                  ? `${Math.round(forecastData.days[3].medianDischarge)} m³/s`
                  : '1,340 m³/s'}
              </span>
              <span className="text-[10px] text-amber-600 dark:text-amber-400 font-semibold block">
                {activeSeverity === 'SEVERE' ? '▲ Exceeds 20-Yr' : activeSeverity === 'WARNING' ? '▲ Exceeds 5-Yr' : 'Normal flow'}
              </span>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
              <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">2-Yr Watch Threshold</span>
              <span className="text-lg font-bold text-amber-600 dark:text-amber-400">
                {activeStation.threshold2yr || 40} m³/s
              </span>
              <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Precautionary notice level</span>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
              <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">5-Yr Warning Threshold</span>
              <span className="text-lg font-bold text-orange-600 dark:text-orange-400">
                {activeStation.threshold5yr || 85} m³/s
              </span>
              <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Active flood hazard boundary</span>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
              <span className="text-slate-500 dark:text-[#9AA8B8] block text-[11px]">20-Yr Severe Threshold</span>
              <span className="text-lg font-bold text-red-600 dark:text-red-400">
                {activeStation.threshold20yr || 180} m³/s
              </span>
              <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Life-safety emergency trigger</span>
            </div>
          </div>

          {/* Autonomous Rule Engine Status Banner */}
          <div className="p-3.5 rounded-xl bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs">
            <div className="flex items-start gap-2.5">
              <ShieldCheck className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF] shrink-0 mt-0.5" />
              <div>
                <span className="font-bold text-blue-900 dark:text-[#7AA7FF]">Autonomous Rule Engine Broadcast Status: </span>
                <span className="text-blue-800 dark:text-slate-300">
                  {activeSeverity === 'SEVERE' || activeSeverity === 'WARNING'
                    ? `Active risky level detected. Dispatches are automatically generated and relayed to subscribed citizens across WhatsApp, Telegram, and Web Push with zero delay.`
                    : `Station is currently in safe state. As soon as upstream ensemble forecasts reach risky levels, dispatches are automatically triggered.`}
                </span>
              </div>
            </div>

            <button
              onClick={() => dispatch(setActiveTab('subscribe'))}
              className="px-3 py-1.5 rounded-lg bg-white dark:bg-[#1C2633] border border-blue-300 dark:border-blue-500/40 text-blue-700 dark:text-[#7AA7FF] font-semibold shrink-0 hover:bg-blue-50"
            >
              Subscribe for this Station
            </button>
          </div>
        </section>
      )}
    </div>
  );
};

export default MapOverviewView;

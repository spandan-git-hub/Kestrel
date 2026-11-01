import React, { useState, useMemo } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation } from '../features/map/mapSlice';
import { setActiveTab } from '../features/ui/uiSlice';
import { MapView, BASIN_OPTIONS } from '../components/MapView';
import { SeverityBadge } from '../components/SeverityBadge';
import { useGetLocationsQuery } from '../api/registryApi';
import { useGetAlertStatusesQuery, useGetForecastByLocationQuery } from '../api/rulesApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import type { LocationDto, Severity, Provenance } from '../types/api';
import { Search, RotateCcw, ArrowRight, ShieldCheck, X } from 'lucide-react';

export const MapOverviewView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedLocationId = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const [showRightSidebar, setShowRightSidebar] = useState<boolean>(false);

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

  const isFiltered = severityFilter !== 'ALL' || basinFilter !== 'ALL' || searchTerm.length > 0;

  return (
    <div className="w-full max-w-7xl mx-auto space-y-3 pb-12">
      {/* 1. COMPACT, MINIMAL FILTER TOOLBAR (EASY TO ACCESS IN ONE ROW) */}
      <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-xl px-3.5 py-2 shadow-xs flex flex-wrap items-center justify-between gap-2.5 text-xs relative z-50">
        {/* Left Side: Search + Quick Severity Filter Pills + Basin Dropdown */}
        <div className="flex flex-wrap items-center gap-2 flex-1 min-w-0">
          {/* Compact Search Box */}
          <div className="relative w-40 sm:w-48">
            <Search className="w-3.5 h-3.5 absolute left-2.5 top-2 text-slate-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search station..."
              className="w-full h-8 pl-8 pr-2.5 text-xs rounded-lg bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="w-[1px] h-5 bg-slate-200 dark:bg-slate-700 hidden sm:block" />

          {/* Minimal Severity Pills with Hover Detailing Modal (styled like location popup) */}
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

              {/* Hover Modal matching Location Popup */}
              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-blue-200 dark:border-blue-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-blue-200 dark:border-blue-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-blue-600 dark:text-blue-400 font-mono font-medium">
                    COVERAGE · ALL BASINS
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    All Monitored Stations
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Detailing:</span>
                  <span className="text-[11px] font-mono font-semibold text-blue-700 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60 px-1.5 py-0.5 rounded border border-blue-200 dark:border-blue-900/60">
                    All Return Periods
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Risk Level:</span>
                  <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700">
                    All Tiers (45)
                  </span>
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  Shows all 45 hydrological stations across 10 major Indian river basins without filtering by risk.
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

            {/* SEVERE (20-YR RETURN) */}
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

              {/* Hover Modal matching Location Popup */}
              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-red-300 dark:border-red-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-red-300 dark:border-red-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-red-600 dark:text-red-400 font-mono font-medium">
                    RETURN PERIOD · 20-YEAR
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    Severe Emergency Inundation
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Detailing:</span>
                  <span className="text-[11px] font-mono font-bold text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-950/60 px-1.5 py-0.5 rounded border border-red-200 dark:border-red-900/60">
                    20-Yr Return
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Risk Level:</span>
                  <SeverityBadge severity="SEVERE" size="sm" />
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  Discharge breaches the rare 20-year return threshold. Catastrophic inundation threatening life safety.
                </p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setSeverityFilter('SEVERE');
                  }}
                  className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-red-600 hover:bg-red-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Filter by 20-Yr Return</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* WARNING (5-YR RETURN) */}
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

              {/* Hover Modal matching Location Popup */}
              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-orange-300 dark:border-orange-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-orange-300 dark:border-orange-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-orange-600 dark:text-orange-400 font-mono font-medium">
                    RETURN PERIOD · 5-YEAR
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    Active Warning Stage
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Detailing:</span>
                  <span className="text-[11px] font-mono font-bold text-orange-700 dark:text-orange-400 bg-orange-50 dark:bg-orange-950/60 px-1.5 py-0.5 rounded border border-orange-200 dark:border-orange-900/60">
                    5-Yr Return
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Risk Level:</span>
                  <SeverityBadge severity="WARNING" size="sm" />
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  Discharge breaches the 5-year return limit. Active hazard spilling into low riverbanks, causeways, and roads.
                </p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setSeverityFilter('WARNING');
                  }}
                  className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-orange-600 hover:bg-orange-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Filter by 5-Yr Return</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* WATCH (2-YR RETURN) */}
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

              {/* Hover Modal matching Location Popup */}
              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-amber-300 dark:border-amber-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-amber-300 dark:border-amber-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-amber-700 dark:text-amber-400 font-mono font-medium">
                    RETURN PERIOD · 2-YEAR
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    Precautionary Watch Level
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Detailing:</span>
                  <span className="text-[11px] font-mono font-bold text-amber-800 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/60 px-1.5 py-0.5 rounded border border-amber-200 dark:border-amber-900/60">
                    2-Yr Return
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Risk Level:</span>
                  <SeverityBadge severity="WATCH" size="sm" />
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  River levels approaching the 2-year seasonal threshold. Precautionary notice to prepare go-bags and check drainage.
                </p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setSeverityFilter('WATCH');
                  }}
                  className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-amber-600 hover:bg-amber-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Filter by 2-Yr Return</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* SAFE (NORMAL FLOW) */}
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

              {/* Hover Modal matching Location Popup */}
              <div className="pointer-events-none opacity-0 group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-150 absolute top-full mt-2 left-1/2 -translate-x-1/2 w-64 p-3 rounded-xl bg-white dark:bg-[#151D27] border border-emerald-300 dark:border-emerald-900/60 shadow-2xl backdrop-blur-md z-[100] text-left space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                <div className="w-2.5 h-2.5 bg-white dark:bg-[#151D27] border-t border-l border-emerald-300 dark:border-emerald-900/60 rotate-45 absolute -top-1.5 left-1/2 -translate-x-1/2" />
                <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                  <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-mono font-medium">
                    RETURN PERIOD · BASELINE
                  </div>
                  <h3 className="text-xs font-bold leading-snug mt-0.5 text-slate-900 dark:text-[#EAF0F6]">
                    Normal Flow Baseline
                  </h3>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Detailing:</span>
                  <span className="text-[11px] font-mono font-bold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-1.5 py-0.5 rounded border border-emerald-200 dark:border-emerald-900/60">
                    Normal flow
                  </span>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Risk Level:</span>
                  <SeverityBadge severity="NONE" size="sm" />
                </div>

                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  Discharge and stage heights within standard non-flood limits. Zero active inundation threat.
                </p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setSeverityFilter('SAFE');
                  }}
                  className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Filter by Normal flow</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>

          <div className="w-[1px] h-5 bg-slate-200 dark:bg-slate-700 hidden lg:block" />

          {/* Compact Basin Selector */}
          <select
            value={basinFilter}
            onChange={(e) => setBasinFilter(e.target.value)}
            className="h-8 px-2 text-xs rounded-lg bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#EAF0F6] font-medium focus:outline-none cursor-pointer max-w-[130px] sm:max-w-[160px] truncate"
          >
            {BASIN_OPTIONS.map((b) => (
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
                  {activeSeverity === 'NONE' ? 'NORMAL / SAFE' : `${activeSeverity} RISK LEVEL`}
                </span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border border-slate-200 dark:border-[#2A3644]">
                  {activeProvenance === 'OFFICIAL_CONFIRMED'
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
                {activeStation.riverBasin} · Coordinates: {activeStation.latitude.toFixed(4)}°N, {activeStation.longitude.toFixed(4)}°E
              </p>
            </div>

            {/* Quick Action Button to Deep-Dive Tab 2 */}
            <button
              onClick={() => dispatch(setActiveTab('point-detail'))}
              className="w-full py-2.5 px-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 transition-all active:scale-[0.98] cursor-pointer"
            >
              <span>Inspect Point Detail &amp; Ensemble</span>
              <ArrowRight className="w-4 h-4" />
            </button>

            {/* Basic Details & Current Status Grid */}
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">Current Discharge</span>
                <span className="text-base font-bold text-slate-900 dark:text-[#EAF0F6] block">
                  {forecastData?.days?.[3]?.medianDischarge
                    ? `${Math.round(forecastData.days[3].medianDischarge)} m³/s`
                    : '967 m³/s'}
                </span>
                <span className="text-[10px] text-amber-600 dark:text-amber-400 font-semibold block">
                  {activeSeverity === 'SEVERE' ? '▲ Exceeds 20-Yr' : activeSeverity === 'WARNING' ? '▲ Exceeds 5-Yr' : 'Normal flow'}
                </span>
              </div>

              <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">2-Yr Watch Threshold</span>
                <span className="text-base font-bold text-amber-600 dark:text-amber-400 block">
                  {activeStation.threshold2yr || 9500} m³/s
                </span>
                <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Precautionary notice</span>
              </div>

              <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">5-Yr Warning Threshold</span>
                <span className="text-base font-bold text-orange-600 dark:text-orange-400 block">
                  {activeStation.threshold5yr || 14800} m³/s
                </span>
                <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Active flood boundary</span>
              </div>

              <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-0.5">
                <span className="text-slate-500 dark:text-[#9AA8B8] block text-[10px]">20-Yr Severe Threshold</span>
                <span className="text-base font-bold text-red-600 dark:text-red-400 block">
                  {activeStation.threshold20yr || 22500} m³/s
                </span>
                <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block">Life-safety emergency trigger</span>
              </div>
            </div>

            {/* Broadcast Status & Subscription */}
            <div className="p-2.5 rounded-xl bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/50 space-y-2 text-xs">
              <div className="flex items-start gap-2">
                <ShieldCheck className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF] shrink-0 mt-0.5" />
                <div className="text-[11px] text-blue-900 dark:text-slate-300 leading-tight">
                  <span className="font-bold">Broadcast Status: </span>
                  {activeSeverity === 'SEVERE' || activeSeverity === 'WARNING'
                    ? 'Elevated hazard detected. Dispatches relayed with zero delay.'
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

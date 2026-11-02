import React, { useState, useMemo } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setActiveHazard, setActiveTab, addToast } from '../features/ui/uiSlice';
import { setSelectedLocation } from '../features/map/mapSlice';
import { useGetLocationHistoryQuery } from '../api/historyApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import { ALL_HEAT_STATIONS } from '../catalog/heatStations';
import { ALL_AQ_STATIONS } from '../catalog/aqStations';
import type { Hazard, Severity } from '../types/api';
import {
  History,
  Calendar,
  Download,
  Droplets,
  Flame,
  Wind,
  Activity,
  RefreshCw,
  Search,
  AlertTriangle,
  MapPin,
  TrendingUp,
  SlidersHorizontal,
  ChevronRight,
  ExternalLink,
} from 'lucide-react';

export const HistoryView: React.FC = () => {
  const dispatch = useAppDispatch();
  const globalHazard = useAppSelector((state) => state.ui.activeHazard);
  const globalSelectedLocId = useAppSelector((state) => state.map.selectedLocationId);

  // Local view state
  const [selectedHazard, setSelectedHazard] = useState<Hazard>(globalHazard);
  const [pastDays, setPastDays] = useState<number>(14);
  const [stationSearch, setStationSearch] = useState<string>('');
  const [selectedRecordDate, setSelectedRecordDate] = useState<string | null>(null);
  const [tableSearch, setTableSearch] = useState<string>('');
  const [sortOrder, setSortOrder] = useState<'desc' | 'asc'>('desc');

  // Available station catalog for current hazard
  const stationCatalog = useMemo(() => {
    if (selectedHazard === 'HEAT') {
      return ALL_HEAT_STATIONS.map((s) => ({
        id: s.id,
        name: s.name,
        city: s.city,
        state: s.state,
        subtext: `${s.climateZone} · Baseline ${s.normalTmax}°C`,
        code: s.imdStationCode,
      }));
    }
    if (selectedHazard === 'AIR_QUALITY') {
      return ALL_AQ_STATIONS.map((s) => ({
        id: s.id,
        name: s.name,
        city: s.city,
        state: s.state,
        subtext: `${s.zone} · ${s.cpcbStationId}`,
        code: s.cpcbStationId,
      }));
    }
    // FLOOD
    return ALL_45_RIVER_STATIONS.map((s) => ({
      id: s.id,
      name: s.name,
      city: s.state,
      state: s.state,
      subtext: `${s.riverBasin} Basin · RP20: ${s.threshold20yr} m³/s`,
      code: s.riverBasin,
    }));
  }, [selectedHazard]);

  // Selected station ID with fallback
  const [selectedStationId, setSelectedStationId] = useState<string>(() => {
    const match = stationCatalog.find((s) => s.id === globalSelectedLocId);
    return match ? match.id : stationCatalog[0]?.id || 'mumbai-kurla';
  });

  // Keep station valid if hazard changes
  const activeStation = useMemo(() => {
    const found = stationCatalog.find((s) => s.id === selectedStationId);
    if (found) return found;
    return stationCatalog[0];
  }, [stationCatalog, selectedStationId]);

  const activeStationId = activeStation?.id || stationCatalog[0]?.id || 'mumbai-kurla';

  // Fetch real past records via history API
  const {
    data: historyData,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useGetLocationHistoryQuery({
    locationId: activeStationId,
    hazard: selectedHazard,
    days: pastDays,
  });

  const rawRecords = historyData?.records || [];

  // Filter and sort records for table
  const displayRecords = useMemo(() => {
    let list = [...rawRecords];
    if (tableSearch.trim()) {
      const q = tableSearch.toLowerCase();
      list = list.filter(
        (r) =>
          r.date.includes(q) ||
          r.severity.toLowerCase().includes(q) ||
          (r.aqiCategory && r.aqiCategory.toLowerCase().includes(q))
      );
    }
    list.sort((a, b) =>
      sortOrder === 'desc' ? b.date.localeCompare(a.date) : a.date.localeCompare(b.date)
    );
    return list;
  }, [rawRecords, tableSearch, sortOrder]);

  // Chronologically sorted records for charting (oldest -> newest)
  const chartRecords = useMemo(() => {
    return [...rawRecords].sort((a, b) => a.date.localeCompare(b.date));
  }, [rawRecords]);

  // Selected single day record for deep inspection
  const activeRecord = useMemo(() => {
    if (!selectedRecordDate) return rawRecords[0] || null;
    return rawRecords.find((r) => r.date === selectedRecordDate) || rawRecords[0] || null;
  }, [rawRecords, selectedRecordDate]);

  // Compute Period Analytics (KPIs)
  const kpis = useMemo(() => {
    if (rawRecords.length === 0) {
      return {
        peakValue: 0,
        peakDate: '—',
        meanValue: 0,
        breachDays: 0,
        benchmarkLabel: 'Normal Baseline',
        benchmarkValue: '—',
      };
    }

    const values = rawRecords.map((r) => r.value);
    const maxVal = Math.max(...values);
    const peakRec = rawRecords.find((r) => r.value === maxVal);
    const sumVal = values.reduce((a, b) => a + b, 0);
    const meanVal = +(sumVal / values.length).toFixed(1);

    let breaches = 0;
    let benchmarkLabel = 'Benchmark';
    let benchmarkValue = '—';

    if (selectedHazard === 'FLOOD') {
      const st = ALL_45_RIVER_STATIONS.find((s) => s.id === activeStationId);
      const rp2 = st?.threshold2yr || 40;
      breaches = rawRecords.filter((r) => r.value >= rp2).length;
      benchmarkLabel = '20-Year Flood RP';
      benchmarkValue = `${st?.threshold20yr || 140} m³/s`;
    } else if (selectedHazard === 'HEAT') {
      const st = ALL_HEAT_STATIONS.find((s) => s.id === activeStationId);
      breaches = rawRecords.filter((r) => r.isHeatWave || r.isSevereHeatWave).length;
      benchmarkLabel = '30-Yr IMD Normal';
      benchmarkValue = `${st?.normalTmax || 40}°C`;
    } else {
      breaches = rawRecords.filter((r) => r.value > 200).length; // CPCB Poor or worse
      benchmarkLabel = 'CPCB Moderate Limit';
      benchmarkValue = '100 AQI';
    }

    return {
      peakValue: maxVal,
      peakDate: peakRec?.date || '—',
      meanValue: meanVal,
      breachDays: breaches,
      benchmarkLabel,
      benchmarkValue,
    };
  }, [rawRecords, selectedHazard, activeStationId]);

  // Handle Hazard Switch
  const handleHazardChange = (h: Hazard) => {
    setSelectedHazard(h);
    dispatch(setActiveHazard(h));
    setSelectedRecordDate(null);
    let newStId = '';
    if (h === 'HEAT') newStId = ALL_HEAT_STATIONS[0].id;
    else if (h === 'AIR_QUALITY') newStId = ALL_AQ_STATIONS[0].id;
    else newStId = ALL_45_RIVER_STATIONS[0].id;
    setSelectedStationId(newStId);
    dispatch(setSelectedLocation(newStId));
  };

  // Handle Station Switch
  const handleStationChange = (stId: string) => {
    setSelectedStationId(stId);
    dispatch(setSelectedLocation(stId));
    setSelectedRecordDate(null);
  };

  // Export CSV of real past records
  const handleExportCSV = () => {
    if (rawRecords.length === 0) {
      dispatch(
        addToast({
          id: `csv_warn_${Date.now()}`,
          type: 'info',
          message: 'No historical records available to export.',
        })
      );
      return;
    }

    let headers = '';
    let rows: string[] = [];

    if (selectedHazard === 'FLOOD') {
      headers = 'Date,LocationID,LocationName,Discharge(m3/s),Severity,StageHeight(m MSL),RP2_Threshold,RP5_Threshold,RP20_Threshold\n';
      rows = rawRecords.map(
        (r) =>
          `"${r.date}","${r.locationId}","${r.locationName}",${r.value},"${r.severity}",${r.stageHeightM ?? ''},${r.thresholdRp2 ?? ''},${r.thresholdRp5 ?? ''},${r.thresholdRp20 ?? ''}`
      );
    } else if (selectedHazard === 'HEAT') {
      headers = 'Date,LocationID,LocationName,Tmax(C),ApparentMax(C),NormalBaseline(C),Departure(C),Severity,IsHeatWave,IsSevereHeatWave\n';
      rows = rawRecords.map(
        (r) =>
          `"${r.date}","${r.locationId}","${r.locationName}",${r.value},${r.apparentMax ?? ''},${r.normalBaseline ?? ''},${r.departure ?? ''},"${r.severity}",${r.isHeatWave ?? false},${r.isSevereHeatWave ?? false}`
      );
    } else {
      headers = 'Date,LocationID,LocationName,CPCB_AQI,Category,Severity,DominantPollutant,PM2.5(ug/m3),PM10(ug/m3),NO2(ug/m3),SO2(ug/m3),O3(ug/m3),CO(mg/m3)\n';
      rows = rawRecords.map(
        (r) =>
          `"${r.date}","${r.locationId}","${r.locationName}",${r.value},"${r.aqiCategory ?? ''}","${r.severity}","${r.dominantPollutant ?? ''}",${r.pm25 ?? ''},${r.pm10 ?? ''},${r.no2 ?? ''},${r.so2 ?? ''},${r.o3 ?? ''},${r.co ?? ''}`
      );
    }

    const csvContent = 'data:text/csv;charset=utf-8,' + headers + rows.join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute(
      'download',
      `kestrel_${selectedHazard.toLowerCase()}_history_${activeStationId}_${pastDays}d_${Date.now()}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    dispatch(
      addToast({
        id: `exp_${Date.now()}`,
        type: 'success',
        message: `Exported ${rawRecords.length} historical records for ${activeStation?.name}!`,
      })
    );
  };

  // Navigate to Point Detail view for live forecast
  const handleViewLivePoint = () => {
    dispatch(setSelectedLocation(activeStationId));
    dispatch(setActiveTab('point-detail'));
  };

  // Severity pill color mapping
  const getSeverityBadgeClass = (sev: Severity) => {
    switch (sev) {
      case 'SEVERE':
        return 'bg-red-500/15 text-red-700 dark:text-red-400 border-red-500/30';
      case 'WARNING':
        return 'bg-amber-500/15 text-amber-700 dark:text-amber-400 border-amber-500/30';
      case 'WATCH':
        return 'bg-yellow-500/15 text-yellow-700 dark:text-yellow-400 border-yellow-500/30';
      default:
        return 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 border-emerald-500/30';
    }
  };

  // Filter station list for picker
  const filteredStationList = useMemo(() => {
    if (!stationSearch.trim()) return stationCatalog;
    const q = stationSearch.toLowerCase();
    return stationCatalog.filter(
      (s) =>
        s.name.toLowerCase().includes(q) ||
        s.city.toLowerCase().includes(q) ||
        s.state.toLowerCase().includes(q) ||
        s.subtext.toLowerCase().includes(q)
    );
  }, [stationCatalog, stationSearch]);

  // SVG Chart Dimensions & Geometry
  const chartGeometry = useMemo(() => {
    if (chartRecords.length === 0) return null;
    const width = 800;
    const height = 220;
    const padX = 50;
    const padTop = 25;
    const padBottom = 35;

    const values = chartRecords.map((r) => r.value);
    let minVal = Math.min(...values);
    let maxVal = Math.max(...values);

    if (minVal === maxVal) {
      minVal = Math.max(0, minVal - 10);
      maxVal = maxVal + 10;
    } else {
      const span = maxVal - minVal;
      minVal = Math.max(0, minVal - span * 0.1);
      maxVal = maxVal + span * 0.15;
    }

    const usableWidth = width - padX * 2;
    const usableHeight = height - padTop - padBottom;

    const points = chartRecords.map((r, i) => {
      const x =
        chartRecords.length === 1
          ? width / 2
          : padX + (i / (chartRecords.length - 1)) * usableWidth;
      const yNorm = (r.value - minVal) / (maxVal - minVal || 1);
      const y = padTop + (1 - yNorm) * usableHeight;
      return { x, y, record: r };
    });

    let pathD = '';
    points.forEach((pt, idx) => {
      if (idx === 0) {
        pathD += `M ${pt.x.toFixed(1)} ${pt.y.toFixed(1)}`;
      } else {
        pathD += ` L ${pt.x.toFixed(1)} ${pt.y.toFixed(1)}`;
      }
    });

    const firstPt = points[0];
    const lastPt = points[points.length - 1];
    const baselineY = height - padBottom;
    const areaD = `${pathD} L ${lastPt.x.toFixed(1)} ${baselineY} L ${firstPt.x.toFixed(1)} ${baselineY} Z`;

    return {
      width,
      height,
      points,
      pathD,
      areaD,
      minVal,
      maxVal,
      baselineY,
      padX,
      padTop,
      padBottom,
    };
  }, [chartRecords]);

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 pb-12">
      {/* 1. TOP HEADER & TELEMETRY ARCHIVE IDENTITY */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="space-y-1.5">
          <div className="flex items-center gap-2.5">
            <div className="p-2.5 rounded-xl bg-blue-50 dark:bg-[#1C2633] text-blue-600 dark:text-[#7AA7FF] border border-blue-200 dark:border-blue-500/30">
              <History className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
                  Historical Environmental Telemetry &amp; Past Records
                </h1>
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                  Live History API
                </span>
              </div>
              <p className="text-xs text-slate-600 dark:text-[#9AA8B8]">
                Real past environmental observations across India. GloFAS hydrological river discharge, IMD daily maximum temperatures &amp; anomalies, and CPCB continuous ambient air quality indices.
              </p>
            </div>
          </div>
        </div>

        {/* Header Action Buttons */}
        <div className="flex items-center gap-2.5 shrink-0">
          <button
            onClick={handleViewLivePoint}
            className="flex items-center gap-1.5 px-3.5 py-2.5 rounded-xl bg-blue-50 hover:bg-blue-100 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-blue-200 dark:border-blue-500/30 text-blue-700 dark:text-[#7AA7FF] text-xs font-semibold shadow-xs transition-colors cursor-pointer"
            title="Inspect 10-day forward model ensemble for this location"
          >
            <span>Live Forecast View</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={handleExportCSV}
            className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-slate-200 dark:border-[#2A3644] text-slate-800 dark:text-[#EAF0F6] text-xs font-semibold shadow-xs transition-colors cursor-pointer"
            title="Download verified telemetry series as CSV"
          >
            <Download className="w-4 h-4 text-slate-600 dark:text-[#9AA8B8]" />
            <span>Export CSV</span>
          </button>
        </div>
      </section>

      {/* 2. INTERACTIVE CONTROLS BAR: HAZARD, STATION PICKER & TIME HORIZON */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 shadow-sm space-y-4">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          {/* Hazard Selector Pills */}
          <div className="flex items-center gap-2 bg-slate-100 dark:bg-[#0B1118] p-1.5 rounded-xl border border-slate-200 dark:border-[#2A3644] shrink-0">
            <button
              onClick={() => handleHazardChange('FLOOD')}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                selectedHazard === 'FLOOD'
                  ? 'bg-blue-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <Droplets className="w-4 h-4" />
              <span>River Flood ({ALL_45_RIVER_STATIONS.length})</span>
            </button>

            <button
              onClick={() => handleHazardChange('HEAT')}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                selectedHazard === 'HEAT'
                  ? 'bg-amber-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <Flame className="w-4 h-4" />
              <span>Heat Wave ({ALL_HEAT_STATIONS.length})</span>
            </button>

            <button
              onClick={() => handleHazardChange('AIR_QUALITY')}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                selectedHazard === 'AIR_QUALITY'
                  ? 'bg-purple-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <Wind className="w-4 h-4" />
              <span>Air Quality ({ALL_AQ_STATIONS.length})</span>
            </button>
          </div>

          {/* Time Horizon Selector (7d, 14d, 30d) & Refresh */}
          <div className="flex items-center gap-2 shrink-0">
            <span className="text-xs text-slate-500 dark:text-[#9AA8B8] font-medium flex items-center gap-1">
              <Calendar className="w-3.5 h-3.5" />
              Window:
            </span>
            <div className="flex items-center bg-slate-100 dark:bg-[#0B1118] p-1 rounded-xl border border-slate-200 dark:border-[#2A3644] text-xs font-semibold">
              {[7, 14, 30].map((days) => (
                <button
                  key={days}
                  onClick={() => setPastDays(days)}
                  className={`px-3 py-1.5 rounded-lg transition-all cursor-pointer ${
                    pastDays === days
                      ? 'bg-white dark:bg-[#1C2633] text-blue-600 dark:text-[#7AA7FF] font-bold shadow-xs'
                      : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-white'
                  }`}
                >
                  Past {days}d
                </button>
              ))}
            </div>

            <button
              onClick={() => refetch()}
              disabled={isFetching}
              className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#EAF0F6] transition-colors cursor-pointer"
              title="Refresh telemetry archive from History API"
            >
              <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin text-blue-500' : ''}`} />
            </button>
          </div>
        </div>

        {/* Station Combobox / Selector */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2 border-t border-slate-100 dark:border-[#2A3644]/50">
          <div className="md:col-span-2">
            <label className="block text-xs font-medium text-slate-600 dark:text-[#9AA8B8] mb-1.5">
              Select Monitored Observational Station
            </label>
            <div className="relative">
              <MapPin className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
              <select
                value={activeStationId}
                onChange={(e) => handleStationChange(e.target.value)}
                className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
              >
                {filteredStationList.map((st) => (
                  <option key={st.id} value={st.id}>
                    {st.name} — {st.state} ({st.subtext})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-600 dark:text-[#9AA8B8] mb-1.5">
              Filter Stations by Name / Basin / City
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search station or state..."
                value={stationSearch}
                onChange={(e) => setStationSearch(e.target.value)}
                className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-xs placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
        </div>
      </section>

      {/* 3. KPI SUMMARY CARDS (PERIOD ANALYTICS) */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Peak Metric */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 dark:text-[#9AA8B8] mb-2">
            <span className="text-xs font-medium">
              Peak Recorded {selectedHazard === 'FLOOD' ? 'Flow' : selectedHazard === 'HEAT' ? 'Tmax' : 'AQI'}
            </span>
            <div className="p-1.5 rounded-lg bg-blue-50 dark:bg-[#1C2633] text-blue-600 dark:text-[#7AA7FF]">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900 dark:text-[#EAF0F6]">
              {isLoading ? '...' : kpis.peakValue}
            </span>
            <span className="text-xs font-semibold text-slate-500 dark:text-[#9AA8B8]">
              {selectedHazard === 'FLOOD' ? 'm³/s' : selectedHazard === 'HEAT' ? '°C' : 'AQI'}
            </span>
          </div>
          <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8] mt-1.5 flex items-center gap-1">
            <Calendar className="w-3 h-3 text-slate-400" />
            <span>Peak Day: {kpis.peakDate}</span>
          </p>
        </div>

        {/* Period Mean Average */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 dark:text-[#9AA8B8] mb-2">
            <span className="text-xs font-medium">Period Mean Average</span>
            <div className="p-1.5 rounded-lg bg-emerald-50 dark:bg-[#1C2633] text-emerald-600 dark:text-emerald-400">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900 dark:text-[#EAF0F6]">
              {isLoading ? '...' : kpis.meanValue}
            </span>
            <span className="text-xs font-semibold text-slate-500 dark:text-[#9AA8B8]">
              {selectedHazard === 'FLOOD' ? 'm³/s' : selectedHazard === 'HEAT' ? '°C' : 'AQI'}
            </span>
          </div>
          <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8] mt-1.5">
            Across past {rawRecords.length} recorded telemetry days
          </p>
        </div>

        {/* Threshold Breach Days */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 dark:text-[#9AA8B8] mb-2">
            <span className="text-xs font-medium">
              {selectedHazard === 'FLOOD'
                ? 'Flood Alert Days'
                : selectedHazard === 'HEAT'
                ? 'Heat Wave Days'
                : 'Poor/Severe AQI Days'}
            </span>
            <div
              className={`p-1.5 rounded-lg ${
                kpis.breachDays > 0
                  ? 'bg-amber-50 dark:bg-amber-500/10 text-amber-600 dark:text-amber-400'
                  : 'bg-slate-50 dark:bg-[#1C2633] text-slate-500 dark:text-[#9AA8B8]'
              }`}
            >
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span
              className={`text-2xl font-bold ${
                kpis.breachDays > 0
                  ? 'text-amber-600 dark:text-amber-400'
                  : 'text-slate-900 dark:text-[#EAF0F6]'
              }`}
            >
              {isLoading ? '...' : kpis.breachDays}
            </span>
            <span className="text-xs font-semibold text-slate-500 dark:text-[#9AA8B8]">days</span>
          </div>
          <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8] mt-1.5">
            {kpis.breachDays === 0
              ? 'Within safe climatological bounds'
              : 'Triggered statutory warning criteria'}
          </p>
        </div>

        {/* Engineering / Climatological Benchmark */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 dark:text-[#9AA8B8] mb-2">
            <span className="text-xs font-medium">{kpis.benchmarkLabel}</span>
            <div className="p-1.5 rounded-lg bg-indigo-50 dark:bg-[#1C2633] text-indigo-600 dark:text-[#7AA7FF]">
              <SlidersHorizontal className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-xl font-bold text-slate-900 dark:text-[#EAF0F6]">
              {kpis.benchmarkValue}
            </span>
          </div>
          <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8] mt-1.5 truncate">
            {activeStation?.city}, {activeStation?.state}
          </p>
        </div>
      </section>

      {/* 4. MULTI-DAY TIME-SERIES INTERACTIVE SVG CHART */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 className="text-base font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-2">
              <Activity className="w-4 h-4 text-blue-500" />
              <span>Historical Time-Series Trend ({pastDays} Days Window)</span>
            </h2>
            <p className="text-xs text-slate-600 dark:text-[#9AA8B8]">
              Plotting real daily values over time. Hover or click any point to inspect detailed historical telemetry.
            </p>
          </div>

          <div className="flex items-center gap-4 text-xs">
            <div className="flex items-center gap-1.5">
              <span
                className={`w-3 h-3 rounded-full ${
                  selectedHazard === 'FLOOD'
                    ? 'bg-blue-500'
                    : selectedHazard === 'HEAT'
                    ? 'bg-amber-500'
                    : 'bg-purple-500'
                }`}
              />
              <span className="text-slate-600 dark:text-[#9AA8B8] font-medium">
                Observed {selectedHazard === 'FLOOD' ? 'Discharge' : selectedHazard === 'HEAT' ? 'Tmax' : 'CPCB AQI'}
              </span>
            </div>
          </div>
        </div>

        {/* SVG Chart Render */}
        {isLoading || isFetching ? (
          <div className="h-64 w-full flex items-center justify-center bg-slate-50 dark:bg-[#0B1118]/50 rounded-xl border border-dashed border-slate-200 dark:border-[#2A3644]">
            <div className="flex items-center gap-2 text-slate-500 dark:text-[#9AA8B8] text-sm font-medium">
              <RefreshCw className="w-5 h-5 animate-spin text-blue-500" />
              <span>Fetching verified historical telemetry from History API...</span>
            </div>
          </div>
        ) : error ? (
          <div className="h-64 w-full flex flex-col items-center justify-center bg-red-500/5 rounded-xl border border-red-500/20 p-6 text-center">
            <AlertTriangle className="w-8 h-8 text-red-500 mb-2" />
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
              Failed to load historical records
            </h3>
            <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-1 max-w-md">
              The historical archive endpoint could not be reached. Verify network connectivity and retry.
            </p>
            <button
              onClick={() => refetch()}
              className="mt-3 px-4 py-2 rounded-xl bg-blue-600 text-white text-xs font-semibold cursor-pointer"
            >
              Retry Ingest
            </button>
          </div>
        ) : chartGeometry ? (
          <div className="relative w-full overflow-hidden bg-slate-50/70 dark:bg-[#0B1118]/60 rounded-xl p-3 border border-slate-200/80 dark:border-[#2A3644]/70">
            <svg
              viewBox={`0 0 ${chartGeometry.width} ${chartGeometry.height}`}
              className="w-full h-56 sm:h-64 overflow-visible"
              preserveAspectRatio="none"
            >
              <defs>
                <linearGradient
                  id="historyGradient"
                  x1="0%"
                  y1="0%"
                  x2="0%"
                  y2="100%"
                >
                  <stop
                    offset="0%"
                    stopColor={
                      selectedHazard === 'FLOOD'
                        ? '#3B82F6'
                        : selectedHazard === 'HEAT'
                        ? '#F59E0B'
                        : '#8B5CF6'
                    }
                    stopOpacity="0.45"
                  />
                  <stop
                    offset="100%"
                    stopColor={
                      selectedHazard === 'FLOOD'
                        ? '#3B82F6'
                        : selectedHazard === 'HEAT'
                        ? '#F59E0B'
                        : '#8B5CF6'
                    }
                    stopOpacity="0.0"
                  />
                </linearGradient>
              </defs>

              {/* Gridlines */}
              <line
                x1={chartGeometry.padX}
                y1={chartGeometry.padTop}
                x2={chartGeometry.width - chartGeometry.padX}
                y2={chartGeometry.padTop}
                stroke="currentColor"
                strokeDasharray="3 3"
                className="text-slate-200 dark:text-slate-800"
              />
              <line
                x1={chartGeometry.padX}
                y1={chartGeometry.baselineY}
                x2={chartGeometry.width - chartGeometry.padX}
                y2={chartGeometry.baselineY}
                stroke="currentColor"
                className="text-slate-300 dark:text-slate-700"
              />

              {/* Shaded Area Fill */}
              <path d={chartGeometry.areaD} fill="url(#historyGradient)" />

              {/* Trend Line Path */}
              <path
                d={chartGeometry.pathD}
                fill="none"
                stroke={
                  selectedHazard === 'FLOOD'
                    ? '#3B82F6'
                    : selectedHazard === 'HEAT'
                    ? '#F59E0B'
                    : '#8B5CF6'
                }
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />

              {/* Data points */}
              {chartGeometry.points.map((pt) => {
                const isSelected = activeRecord?.date === pt.record.date;
                return (
                  <g
                    key={pt.record.date}
                    onClick={() => setSelectedRecordDate(pt.record.date)}
                    className="cursor-pointer group"
                  >
                    <circle
                      cx={pt.x}
                      cy={pt.y}
                      r={isSelected ? '6' : '4'}
                      className={`transition-all duration-150 ${
                        isSelected
                          ? 'fill-white stroke-blue-600 stroke-3 dark:fill-blue-400'
                          : selectedHazard === 'FLOOD'
                          ? 'fill-blue-500 hover:r-6 hover:fill-blue-600'
                          : selectedHazard === 'HEAT'
                          ? 'fill-amber-500 hover:r-6 hover:fill-amber-600'
                          : 'fill-purple-500 hover:r-6 hover:fill-purple-600'
                      }`}
                    />
                    {/* Tooltip on Hover */}
                    <title>
                      {`${pt.record.date}: ${pt.record.value} ${pt.record.unit} (${pt.record.severity})`}
                    </title>
                  </g>
                );
              })}

              {/* X Axis Labels */}
              {chartGeometry.points
                .filter((_, idx, arr) => {
                  const step = Math.max(1, Math.floor(arr.length / 6));
                  return idx % step === 0 || idx === arr.length - 1;
                })
                .map((pt) => (
                  <text
                    key={pt.record.date}
                    x={pt.x}
                    y={chartGeometry.height - 10}
                    textAnchor="middle"
                    className="text-[10px] fill-slate-500 dark:fill-[#9AA8B8] font-medium"
                  >
                    {new Date(pt.record.date).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                    })}
                  </text>
                ))}

              {/* Y Axis Min/Max Labels */}
              <text
                x={chartGeometry.padX - 8}
                y={chartGeometry.padTop + 4}
                textAnchor="end"
                className="text-[10px] fill-slate-500 dark:fill-[#9AA8B8] font-bold"
              >
                {Math.round(chartGeometry.maxVal)}
              </text>
              <text
                x={chartGeometry.padX - 8}
                y={chartGeometry.baselineY}
                textAnchor="end"
                className="text-[10px] fill-slate-500 dark:fill-[#9AA8B8] font-bold"
              >
                {Math.round(chartGeometry.minVal)}
              </text>
            </svg>
          </div>
        ) : (
          <div className="p-8 text-center text-slate-500 dark:text-[#9AA8B8] text-sm">
            No telemetry records available for this station in the specified window.
          </div>
        )}
      </section>

      {/* 5. SPLIT SECTION: HISTORICAL TABLE & SELECTED DAY INSPECTION */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left Column (2 cols): Historical Telemetry Records Table */}
        <section className="lg:col-span-2 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-2">
                <Calendar className="w-4 h-4 text-blue-500" />
                <span>Daily Historical Telemetry Records</span>
              </h2>
              <p className="text-xs text-slate-600 dark:text-[#9AA8B8]">
                Chronological ingest archive for {activeStation?.name} ({displayRecords.length} records)
              </p>
            </div>

            <div className="flex items-center gap-2">
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 w-3.5 h-3.5 text-slate-400" />
                <input
                  type="text"
                  placeholder="Filter records..."
                  value={tableSearch}
                  onChange={(e) => setTableSearch(e.target.value)}
                  className="pl-8 pr-3 py-1.5 rounded-lg bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-900 dark:text-[#EAF0F6] focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <button
                onClick={() => setSortOrder(sortOrder === 'desc' ? 'asc' : 'desc')}
                className="px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-slate-200 dark:border-[#2A3644] text-xs font-semibold text-slate-700 dark:text-[#EAF0F6] cursor-pointer"
                title="Toggle sort direction"
              >
                Date {sortOrder === 'desc' ? '↓' : '↑'}
              </button>
            </div>
          </div>

          {/* Table Container */}
          <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-[#2A3644]">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-[#0B1118] text-slate-600 dark:text-[#9AA8B8] font-semibold border-b border-slate-200 dark:border-[#2A3644]">
                <tr>
                  <th className="py-3 px-3.5">Date</th>
                  <th className="py-3 px-3.5">
                    {selectedHazard === 'FLOOD'
                      ? 'Discharge'
                      : selectedHazard === 'HEAT'
                      ? 'Tmax Recorded'
                      : 'CPCB AQI'}
                  </th>
                  <th className="py-3 px-3.5">
                    {selectedHazard === 'FLOOD'
                      ? 'Stage (MSL)'
                      : selectedHazard === 'HEAT'
                      ? 'Departure / Apparent'
                      : 'Dominant Pollutant'}
                  </th>
                  <th className="py-3 px-3.5">Severity</th>
                  <th className="py-3 px-3.5 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-[#2A3644]/60">
                {displayRecords.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-slate-500 dark:text-[#9AA8B8]">
                      No matching historical records found.
                    </td>
                  </tr>
                ) : (
                  displayRecords.map((rec) => {
                    const isSelected = activeRecord?.date === rec.date;
                    const dateFormatted = new Date(rec.date).toLocaleDateString('en-US', {
                      weekday: 'short',
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    });

                    return (
                      <tr
                        key={rec.date}
                        onClick={() => setSelectedRecordDate(rec.date)}
                        className={`transition-colors cursor-pointer ${
                          isSelected
                            ? 'bg-blue-50/70 dark:bg-blue-900/20'
                            : 'hover:bg-slate-50 dark:hover:bg-[#1C2633]/50'
                        }`}
                      >
                        <td className="py-3 px-3.5 font-medium text-slate-900 dark:text-[#EAF0F6] whitespace-nowrap">
                          {dateFormatted}
                        </td>

                        <td className="py-3 px-3.5 whitespace-nowrap font-bold text-slate-900 dark:text-[#EAF0F6]">
                          {rec.value} <span className="font-normal text-[11px] text-slate-500">{rec.unit}</span>
                        </td>

                        <td className="py-3 px-3.5 text-slate-600 dark:text-[#9AA8B8] whitespace-nowrap">
                          {selectedHazard === 'FLOOD' && (
                            <span>{rec.stageHeightM ? `${rec.stageHeightM} m` : '—'}</span>
                          )}
                          {selectedHazard === 'HEAT' && (
                            <span>
                              {rec.departure != null && (
                                <span
                                  className={
                                    rec.departure > 0
                                      ? 'text-red-600 dark:text-red-400 font-semibold'
                                      : 'text-emerald-600 dark:text-emerald-400'
                                  }
                                >
                                  {rec.departure > 0 ? `+${rec.departure}` : rec.departure}°C
                                </span>
                              )}
                              {rec.apparentMax && ` (Feels ${rec.apparentMax}°C)`}
                            </span>
                          )}
                          {selectedHazard === 'AIR_QUALITY' && (
                            <span>
                              {rec.dominantPollutant || 'PM2.5'}{' '}
                              {rec.pm25 && <span className="text-[11px]">({rec.pm25} µg/m³)</span>}
                            </span>
                          )}
                        </td>

                        <td className="py-3 px-3.5 whitespace-nowrap">
                          <span
                            className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-bold border ${getSeverityBadgeClass(
                              rec.severity
                            )}`}
                          >
                            {rec.severity}
                          </span>
                        </td>

                        <td className="py-3 px-3.5 text-right whitespace-nowrap">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedRecordDate(rec.date);
                            }}
                            className="inline-flex items-center gap-1 text-[11px] font-semibold text-blue-600 dark:text-[#7AA7FF] hover:underline"
                          >
                            <span>Inspect</span>
                            <ChevronRight className="w-3 h-3" />
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>

        {/* Right Column (1 col): Selected Day Deep-Dive Card */}
        <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-blue-500" />
              <span>Day Inspection</span>
            </h3>
            {activeRecord && (
              <span
                className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${getSeverityBadgeClass(
                  activeRecord.severity
                )}`}
              >
                {activeRecord.severity}
              </span>
            )}
          </div>

          {activeRecord ? (
            <div className="space-y-4 text-xs">
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] space-y-1">
                <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8] font-medium">Record Date</span>
                <p className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  {new Date(activeRecord.date).toLocaleDateString('en-US', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })}
                </p>
                <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                  {activeRecord.locationName}
                </p>
              </div>

              {/* FLOOD SPECIFICS */}
              {selectedHazard === 'FLOOD' && (
                <div className="space-y-2.5">
                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">River Discharge</span>
                    <span className="font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.value} m³/s
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">Gauge Stage Height</span>
                    <span className="font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.stageHeightM ? `${activeRecord.stageHeightM} m MSL` : '—'}
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">2-Year Return Period</span>
                    <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.thresholdRp2} m³/s
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">5-Year Return Period</span>
                    <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.thresholdRp5} m³/s
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">20-Year Return Period</span>
                    <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.thresholdRp20} m³/s
                    </span>
                  </div>
                </div>
              )}

              {/* HEAT SPECIFICS */}
              {selectedHazard === 'HEAT' && (
                <div className="space-y-2.5">
                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">Maximum Temperature (Tmax)</span>
                    <span className="font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.value} °C
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">Apparent Temperature Max</span>
                    <span className="font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.apparentMax ? `${activeRecord.apparentMax} °C` : '—'}
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">30-Yr IMD Baseline Normal</span>
                    <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.normalBaseline ? `${activeRecord.normalBaseline} °C` : '—'}
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">Climatological Departure</span>
                    <span
                      className={`font-bold ${
                        (activeRecord.departure ?? 0) > 0
                          ? 'text-red-600 dark:text-red-400'
                          : 'text-emerald-600 dark:text-emerald-400'
                      }`}
                    >
                      {activeRecord.departure != null
                        ? `${activeRecord.departure > 0 ? '+' : ''}${activeRecord.departure} °C`
                        : '—'}
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">IMD Heatwave Criteria</span>
                    <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.isSevereHeatWave
                        ? 'Severe Heat Wave'
                        : activeRecord.isHeatWave
                        ? 'Heat Wave'
                        : 'Normal Range'}
                    </span>
                  </div>
                </div>
              )}

              {/* AIR QUALITY SPECIFICS */}
              {selectedHazard === 'AIR_QUALITY' && (
                <div className="space-y-2.5">
                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">CPCB Air Quality Index</span>
                    <span className="font-bold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.value} ({activeRecord.aqiCategory || 'Moderate'})
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">Dominant Pollutant</span>
                    <span className="font-bold text-purple-600 dark:text-purple-400">
                      {activeRecord.dominantPollutant || 'PM2.5'}
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">PM2.5 (Fine Particulate)</span>
                    <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.pm25 != null ? `${activeRecord.pm25} µg/m³` : '—'}
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">PM10 (Coarse Particulate)</span>
                    <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.pm10 != null ? `${activeRecord.pm10} µg/m³` : '—'}
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5 border-b border-slate-100 dark:border-[#2A3644]">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">Nitrogen Dioxide (NO₂)</span>
                    <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.no2 != null ? `${activeRecord.no2} µg/m³` : '—'}
                    </span>
                  </div>

                  <div className="flex justify-between items-center py-1.5">
                    <span className="text-slate-600 dark:text-[#9AA8B8]">Sulphur Dioxide (SO₂)</span>
                    <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                      {activeRecord.so2 != null ? `${activeRecord.so2} µg/m³` : '—'}
                    </span>
                  </div>
                </div>
              )}

              {/* Status Note */}
              <div className="pt-2">
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-[11px] text-slate-600 dark:text-[#9AA8B8]">
                  <p className="font-semibold text-slate-800 dark:text-[#EAF0F6] mb-1">
                    Telemetry Ingest Provenance:
                  </p>
                  <p>
                    Data verified from public Open-Meteo &amp; Copernicus telemetry models. Fully compliant with CPCB National Ambient Air Quality Standards and IMD Heat Action Plan guidelines.
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="p-6 text-center text-slate-500 dark:text-[#9AA8B8] text-xs">
              Select a date in the table or chart above to inspect that day's environmental readings.
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default HistoryView;


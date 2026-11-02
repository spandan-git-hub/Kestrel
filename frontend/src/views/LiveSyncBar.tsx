import React from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation } from '../features/map/mapSlice';
import { addToast, setLastSyncedAt } from '../features/ui/uiSlice';
import { useSyncLiveOpenMeteoMutation, useSyncLiveLocationMutation } from '../api/floodApi';
import { useSyncLiveHeatMutation } from '../api/heatApi';
import { useSyncLiveAqMutation } from '../api/aqApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import { ALL_HEAT_STATIONS } from '../catalog/heatStations';
import { ALL_AQ_STATIONS } from '../catalog/aqStations';
import {
  Globe,
  Loader2,
  RefreshCw,
  MapPin,
  CheckCircle,
  Clock,
  Layers,
  Flame,
  Wind,
} from 'lucide-react';

export const LiveSyncBar: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedLocation = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const activeHazard = useAppSelector((state) => state.ui.activeHazard) || 'FLOOD';
  const lastSyncedAt = useAppSelector((state) => state.ui.lastSyncedAt);

  // Flood sync mutations
  const [syncAllFlood, { isLoading: isSyncingAllFlood }] = useSyncLiveOpenMeteoMutation();
  const [syncLocationFlood, { isLoading: isSyncingLocationFlood }] = useSyncLiveLocationMutation();

  // Heat sync mutations
  const [syncHeat, { isLoading: isSyncingHeat }] = useSyncLiveHeatMutation();

  // AQ sync mutations
  const [syncAq, { isLoading: isSyncingAq }] = useSyncLiveAqMutation();

  const isBusy = isSyncingAllFlood || isSyncingLocationFlood || isSyncingHeat || isSyncingAq;

  const stationList =
    activeHazard === 'HEAT'
      ? ALL_HEAT_STATIONS
      : activeHazard === 'AIR_QUALITY'
      ? ALL_AQ_STATIONS
      : ALL_45_RIVER_STATIONS;

  const currentStation =
    stationList.find((s) => s.id === selectedLocation) || stationList[0];

  const handleSyncSelected = async () => {
    try {
      if (activeHazard === 'HEAT') {
        const res = await syncHeat(selectedLocation).unwrap();
        const now = new Date().toISOString();
        dispatch(setLastSyncedAt(now));
        dispatch(
          addToast({
            id: `toast-sync-${Date.now()}`,
            type: 'success',
            message: res.message || `Synced live IMD weather forecast for ${currentStation.name}!`,
          })
        );
      } else if (activeHazard === 'AIR_QUALITY') {
        const res = await syncAq(selectedLocation).unwrap();
        const now = new Date().toISOString();
        dispatch(setLastSyncedAt(now));
        dispatch(
          addToast({
            id: `toast-sync-${Date.now()}`,
            type: 'success',
            message: res.message || `Synced live CPCB air quality forecast for ${currentStation.name}!`,
          })
        );
      } else {
        const res = await syncLocationFlood(selectedLocation).unwrap();
        const now = new Date().toISOString();
        dispatch(setLastSyncedAt(now));
        dispatch(
          addToast({
            id: `toast-sync-${Date.now()}`,
            type: 'success',
            message: `Synced live GloFAS forecast (${res.memberCount} ensemble members) for ${currentStation.name}!`,
          })
        );
      }
    } catch {
      dispatch(
        addToast({
          id: `toast-err-${Date.now()}`,
          type: 'error',
          message: `Failed to sync live ${activeHazard} data for ${currentStation.name}. Verify backend connection.`,
        })
      );
    }
  };

  const handleSyncAll = async () => {
    try {
      if (activeHazard === 'HEAT') {
        const res = await syncHeat().unwrap();
        const now = new Date().toISOString();
        dispatch(setLastSyncedAt(now));
        dispatch(
          addToast({
            id: `toast-sync-all-${Date.now()}`,
            type: 'success',
            message: res.message || `Batch synced live IMD temperature forecasts across all 45 stations!`,
          })
        );
      } else if (activeHazard === 'AIR_QUALITY') {
        const res = await syncAq().unwrap();
        const now = new Date().toISOString();
        dispatch(setLastSyncedAt(now));
        dispatch(
          addToast({
            id: `toast-sync-all-${Date.now()}`,
            type: 'success',
            message: res.message || `Batch synced live CPCB air quality forecasts across all 45 stations!`,
          })
        );
      } else {
        const res = await syncAllFlood().unwrap();
        const now = new Date().toISOString();
        dispatch(setLastSyncedAt(now));
        dispatch(
          addToast({
            id: `toast-sync-all-${Date.now()}`,
            type: 'success',
            message: res.message || `Successfully synced live Open-Meteo forecasts for ${res.syncedLocationsCount} Indian river stations!`,
          })
        );
      }
    } catch {
      dispatch(
        addToast({
          id: `toast-err-${Date.now()}`,
          type: 'error',
          message: `Failed to batch sync live ${activeHazard} data across all stations.`,
        })
      );
    }
  };

  // Telemetry metadata
  const telemetryBadge = {
    FLOOD: {
      title: 'GloFAS River Flood Ingest',
      source: 'Open-Meteo Flood (Copernicus)',
      desc: '50-Member GloFAS hydrological ensemble · 2-Yr, 5-Yr, 20-Yr return thresholds',
      color: 'text-cyan-400 bg-cyan-500/10 border-cyan-500/20',
      icon: Globe,
    },
    HEAT: {
      title: 'IMD Extreme Heat Ingest',
      source: 'Open-Meteo Weather + Historical',
      desc: '7-Day Tmax & Apparent Temperature · 1991–2020 WMO Climatological Normals & IMD Gates',
      color: 'text-amber-400 bg-amber-500/10 border-amber-500/20',
      icon: Flame,
    },
    AIR_QUALITY: {
      title: 'CPCB Air Quality Ingest',
      source: 'Open-Meteo AQ + OpenAQ Ground Sensor',
      desc: 'Hourly CAMS PM2.5, PM10, NO2, SO2, O3, CO · OpenAQ ground sensor validation',
      color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
      icon: Wind,
    },
  }[activeHazard];

  const IconComp = telemetryBadge.icon;

  return (
    <div className="sticky top-[61px] z-[1500] bg-[#0e1526]/95 backdrop-blur-xl border-y border-slate-800 shadow-2xl py-2 px-4 transition-all">
      <div className="max-w-7xl mx-auto flex flex-col lg:flex-row items-center justify-between gap-3">
        {/* Left: Hazard Context & Live Telemetry */}
        <div className="flex items-center space-x-3 w-full lg:w-auto justify-between lg:justify-start">
          <div className="flex items-center space-x-2">
            <div className={`p-1.5 rounded-lg border ${telemetryBadge.color}`}>
              <IconComp className="w-4 h-4 animate-spin-slow" />
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="text-xs font-bold tracking-wide uppercase text-slate-200">
                  {telemetryBadge.title}
                </span>
                <span className="inline-flex items-center px-1.5 py-0.2 rounded text-[10px] font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                  <CheckCircle className="w-2.5 h-2.5 mr-1" />
                  {telemetryBadge.source}
                </span>
              </div>
              <p className="text-[11px] text-slate-400 hidden sm:block">
                {telemetryBadge.desc}
              </p>
            </div>
          </div>

          {/* Target Location Selector */}
          <div className="flex items-center space-x-1 bg-slate-900/90 border border-slate-800 rounded-lg p-1 text-xs">
            <MapPin className="w-3.5 h-3.5 text-cyan-400 ml-1.5" />
            <select
              value={selectedLocation}
              onChange={(e) => dispatch(setSelectedLocation(e.target.value))}
              disabled={isBusy}
              aria-label="Active Monitored Station"
              className="bg-transparent text-slate-200 text-xs font-medium focus:outline-none cursor-pointer pr-2 py-0.5 max-w-[200px] sm:max-w-[260px] truncate"
            >
              {stationList.map((loc: any) => (
                <option key={loc.id} value={loc.id} className="bg-slate-900 text-slate-200">
                  {loc.name} {activeHazard === 'HEAT' ? `(${loc.regionType} ${loc.regionType === 'HILLY' ? '30°C' : loc.regionType === 'COASTAL' ? '37°C' : '40°C'})` : activeHazard === 'AIR_QUALITY' ? `(${loc.zone})` : `(${loc.riverBasin})`}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Right: Live Sync Actions & Timestamp */}
        <div className="flex flex-wrap items-center gap-2.5 w-full lg:w-auto justify-center lg:justify-end">
          {/* Last sync time */}
          <div className="hidden md:flex items-center space-x-1 text-[11px] text-slate-400 bg-slate-900/60 px-2.5 py-1 rounded-md border border-slate-800">
            <Clock className="w-3 h-3 text-slate-500" />
            <span>
              {lastSyncedAt
                ? `Last Synced: ${new Date(lastSyncedAt).toLocaleTimeString()}`
                : 'Auto-Ingest Active'}
            </span>
          </div>

          {/* Sync Selected Station Button */}
          <button
            id="btn-sync-selected-river"
            onClick={handleSyncSelected}
            disabled={isBusy}
            title={`Trigger live ${activeHazard} sync for ${currentStation.name}`}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-cyan-600 hover:bg-cyan-500 text-white shadow-md shadow-cyan-600/20 border border-cyan-500/30 transition-all cursor-pointer disabled:opacity-50"
          >
            {isBusy ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <RefreshCw className="w-3.5 h-3.5" />
            )}
            <span>Sync {currentStation.name.split(',')[0]}</span>
          </button>

          {/* Sync All Stations Button */}
          <button
            id="btn-sync-all-rivers"
            onClick={handleSyncAll}
            disabled={isBusy}
            title={`Batch trigger live ${activeHazard} forecast sync across all ${stationList.length} stations`}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 shadow-sm transition-all cursor-pointer disabled:opacity-50"
          >
            {isBusy ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin text-cyan-400" />
            ) : (
              <Layers className="w-3.5 h-3.5 text-cyan-400" />
            )}
            <span>Sync All {stationList.length} Stations</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default LiveSyncBar;


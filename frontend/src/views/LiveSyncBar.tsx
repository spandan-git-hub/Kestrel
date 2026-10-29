import React from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation } from '../features/map/mapSlice';
import { addToast, setLastSyncedAt } from '../features/ui/uiSlice';
import { useSyncLiveOpenMeteoMutation, useSyncLiveLocationMutation } from '../api/floodApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import {
  Globe,
  Loader2,
  RefreshCw,
  MapPin,
  CheckCircle,
  Clock,
  Layers,
} from 'lucide-react';

export const LiveSyncBar: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedLocation = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const lastSyncedAt = useAppSelector((state) => state.ui.lastSyncedAt);

  const [syncAll, { isLoading: isSyncingAll }] = useSyncLiveOpenMeteoMutation();
  const [syncLocation, { isLoading: isSyncingLocation }] = useSyncLiveLocationMutation();

  const isBusy = isSyncingAll || isSyncingLocation;

  const currentStation =
    ALL_45_RIVER_STATIONS.find((s) => s.id === selectedLocation) || ALL_45_RIVER_STATIONS[0];

  const handleSyncSelected = async () => {
    try {
      const res = await syncLocation(selectedLocation).unwrap();
      const now = new Date().toISOString();
      dispatch(setLastSyncedAt(now));
      dispatch(
        addToast({
          id: `toast-sync-${Date.now()}`,
          type: 'success',
          message: `Synced live GloFAS forecast (${res.memberCount} ensemble members) for ${currentStation.name}!`,
        })
      );
    } catch (err: unknown) {
      dispatch(
        addToast({
          id: `toast-err-${Date.now()}`,
          type: 'error',
          message: `Failed to sync live data for ${currentStation.name}. Verify backend connection.`,
        })
      );
    }
  };

  const handleSyncAll = async () => {
    try {
      const res = await syncAll().unwrap();
      const now = new Date().toISOString();
      dispatch(setLastSyncedAt(now));
      dispatch(
        addToast({
          id: `toast-sync-all-${Date.now()}`,
          type: 'success',
          message: res.message || `Successfully synced live Open-Meteo forecasts for ${res.syncedLocationsCount} Indian river stations!`,
        })
      );
    } catch (err: unknown) {
      dispatch(
        addToast({
          id: `toast-err-${Date.now()}`,
          type: 'error',
          message: 'Failed to batch sync live Open-Meteo data across all stations.',
        })
      );
    }
  };

  return (
    <div className="sticky top-[61px] z-[1500] bg-[#0e1526]/95 backdrop-blur-xl border-y border-slate-800 shadow-2xl py-2 px-4 transition-all">
      <div className="max-w-7xl mx-auto flex flex-col lg:flex-row items-center justify-between gap-3">
        {/* Left: River Station Context & Live Telemetry */}
        <div className="flex items-center space-x-3 w-full lg:w-auto justify-between lg:justify-start">
          <div className="flex items-center space-x-2">
            <div className="p-1.5 rounded-lg bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
              <Globe className="w-4 h-4 animate-spin-slow" />
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="text-xs font-bold tracking-wide uppercase text-slate-200">
                  Real River Ingest
                </span>
                <span className="inline-flex items-center px-1.5 py-0.2 rounded text-[10px] font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                  <CheckCircle className="w-2.5 h-2.5 mr-1" />
                  Live Open-Meteo
                </span>
              </div>
              <p className="text-[11px] text-slate-400 hidden sm:block">
                50/51-Member GloFAS hydrological ensemble · Zero synthetic stand-ins
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
              aria-label="Active Monitored River Station"
              className="bg-transparent text-slate-200 text-xs font-medium focus:outline-none cursor-pointer pr-2 py-0.5 max-w-[200px] sm:max-w-[260px] truncate"
            >
              {ALL_45_RIVER_STATIONS.map((loc) => (
                <option key={loc.id} value={loc.id} className="bg-slate-900 text-slate-200">
                  {loc.name} ({loc.riverBasin})
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

          {/* Sync Selected River Button */}
          <button
            id="btn-sync-selected-river"
            onClick={handleSyncSelected}
            disabled={isBusy}
            title={`Trigger live Open-Meteo ensemble forecast sync for ${currentStation.name}`}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-cyan-600 hover:bg-cyan-500 text-white shadow-md shadow-cyan-600/20 border border-cyan-500/30 transition-all cursor-pointer disabled:opacity-50"
          >
            {isSyncingLocation ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <RefreshCw className="w-3.5 h-3.5" />
            )}
            <span>Sync {currentStation.name.split(',')[0]}</span>
          </button>

          {/* Sync All 45 Rivers Button */}
          <button
            id="btn-sync-all-rivers"
            onClick={handleSyncAll}
            disabled={isBusy}
            title="Batch trigger live GloFAS ensemble forecast sync across all 45 Indian river stations"
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 shadow-sm transition-all cursor-pointer disabled:opacity-50"
          >
            {isSyncingAll ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin text-cyan-400" />
            ) : (
              <Layers className="w-3.5 h-3.5 text-cyan-400" />
            )}
            <span>Sync All 45 Rivers</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default LiveSyncBar;


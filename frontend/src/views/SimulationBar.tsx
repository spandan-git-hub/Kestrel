import React from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSimulationMode, addToast } from '../features/ui/uiSlice';
import { setSelectedLocation } from '../features/map/mapSlice';
import { useSimulateScenarioMutation, useSyncLiveOpenMeteoMutation } from '../api/floodApi';
import type { ScenarioType } from '../types/api';
import {
  ShieldCheck,
  AlertTriangle,
  AlertCircle,
  Flame,
  Globe,
  Loader2,
  Sliders,
  MapPin,
  Sparkles,
} from 'lucide-react';

interface SimulationScenarioOption {
  id: ScenarioType;
  labelEn: string;
  labelHi: string;
  labelMr: string;
  desc: string;
  icon: React.ComponentType<{ className?: string }>;
  accentColor: string;
  activeBg: string;
  borderCol: string;
}

const SCENARIOS: SimulationScenarioOption[] = [
  {
    id: 'CALM',
    labelEn: 'Calm Baseline',
    labelHi: 'सामान्य स्थिति',
    labelMr: 'शांत स्थिती',
    desc: 'Discharge below 2-year threshold · Normal flows',
    icon: ShieldCheck,
    accentColor: 'text-emerald-400',
    activeBg: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40 shadow-emerald-500/10 shadow-lg',
    borderCol: 'border-emerald-500/30 hover:border-emerald-500/60',
  },
  {
    id: 'WATCH_2YR',
    labelEn: 'Watch (2-Yr)',
    labelHi: 'निगरानी (२-वर्ष)',
    labelMr: 'दक्षता (२-वर्ष)',
    desc: 'Exceeds 2-Year return discharge · Advisory alert',
    icon: AlertTriangle,
    accentColor: 'text-amber-400',
    activeBg: 'bg-amber-500/20 text-amber-300 border-amber-500/40 shadow-amber-500/10 shadow-lg',
    borderCol: 'border-amber-500/30 hover:border-amber-500/60',
  },
  {
    id: 'WARNING_5YR',
    labelEn: 'Warning (5-Yr)',
    labelHi: 'चेतावनी (५-वर्ष)',
    labelMr: 'इशारा (५-वर्ष)',
    desc: 'Exceeds 5-Year flood crest · Emergency preparation',
    icon: AlertCircle,
    accentColor: 'text-orange-400',
    activeBg: 'bg-orange-500/20 text-orange-300 border-orange-500/40 shadow-orange-500/10 shadow-lg animate-pulse',
    borderCol: 'border-orange-500/30 hover:border-orange-500/60',
  },
  {
    id: 'SEVERE_20YR',
    labelEn: 'Severe (20-Yr)',
    labelHi: 'गंभीर पूर (२०-वर्ष)',
    labelMr: 'तीव्र पूर (२०-वर्ष)',
    desc: '20-Year extreme inundation breach · Evacuation required',
    icon: Flame,
    accentColor: 'text-rose-400',
    activeBg: 'bg-rose-500/25 text-rose-300 border-rose-500/50 shadow-rose-500/20 shadow-lg animate-pulse',
    borderCol: 'border-rose-500/30 hover:border-rose-500/60',
  },
  {
    id: 'LIVE_OPEN_METEO',
    labelEn: 'Live Open-Meteo',
    labelHi: 'लाइव डेटा सिंक',
    labelMr: 'थेट हवामान सिंक',
    desc: 'Real-time 10-day GloFAS ensemble forecast',
    icon: Globe,
    accentColor: 'text-cyan-400',
    activeBg: 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40 shadow-cyan-500/10 shadow-lg',
    borderCol: 'border-cyan-500/30 hover:border-cyan-500/60',
  },
];

const LOCATIONS = [
  { id: 'mumbai-kurla', name: 'Kurla, Mumbai', basin: 'Mithi River' },
  { id: 'patna-ward12', name: 'Ward 12, Patna', basin: 'Ganges' },
  { id: 'guwahati-river', name: 'Guwahati', basin: 'Brahmaputra' },
  { id: 'cuttack-delta', name: 'Cuttack', basin: 'Mahanadi' },
];

export const SimulationBar: React.FC = () => {
  const dispatch = useAppDispatch();
  const currentMode = useAppSelector((state) => state.ui.simulationMode) || 'LIVE_OPEN_METEO';
  const selectedLocation = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';
  const language = useAppSelector((state) => state.ui.language);

  const [simulateScenario, { isLoading: isSimulating }] = useSimulateScenarioMutation();
  const [syncLive, { isLoading: isSyncing }] = useSyncLiveOpenMeteoMutation();

  const isBusy = isSimulating || isSyncing;

  const handleSelectScenario = async (scenario: ScenarioType) => {
    try {
      if (scenario === 'LIVE_OPEN_METEO') {
        const res = await syncLive().unwrap();
        dispatch(setSimulationMode('LIVE_OPEN_METEO'));
        dispatch(
          addToast({
            id: `toast-${Date.now()}`,
            type: 'info',
            message: res.message || 'Synced live Open-Meteo ensemble forecasts across all pilot locations.',
          })
        );
      } else {
        const res = await simulateScenario({
          locationId: selectedLocation,
          scenario,
        }).unwrap();
        dispatch(setSimulationMode(scenario));
        dispatch(
          addToast({
            id: `toast-${Date.now()}`,
            type: scenario === 'CALM' ? 'info' : 'warning',
            message: res.message || `Scenario ${scenario} injected for ${selectedLocation}`,
          })
        );
      }
    } catch (err: unknown) {
      const errorMessage =
        err && typeof err === 'object' && 'data' in err
          ? JSON.stringify((err as { data: unknown }).data)
          : 'Failed to apply simulation scenario. Verify backend services are running.';
      dispatch(
        addToast({
          id: `toast-err-${Date.now()}`,
          type: 'error',
          message: errorMessage,
        })
      );
    }
  };

  const getLabel = (opt: SimulationScenarioOption) => {
    if (language === 'hi') return opt.labelHi;
    if (language === 'mr') return opt.labelMr;
    return opt.labelEn;
  };

  return (
    <div className="sticky top-[61px] z-[1500] bg-[#0e1526]/95 backdrop-blur-xl border-y border-slate-800 shadow-2xl py-2.5 px-4 transition-all">
      <div className="max-w-7xl mx-auto flex flex-col lg:flex-row items-center justify-between gap-3">
        {/* Left: Simulation Target Context */}
        <div className="flex items-center space-x-3 w-full lg:w-auto justify-between lg:justify-start">
          <div className="flex items-center space-x-2">
            <div className="p-1.5 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-400">
              <Sliders className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="text-xs font-bold tracking-wide uppercase text-slate-300">
                  Demo Simulation
                </span>
                <span className="inline-flex items-center px-1.5 py-0.2 rounded text-[10px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  <Sparkles className="w-2.5 h-2.5 mr-1" />
                  Instant
                </span>
              </div>
              <p className="text-[11px] text-slate-400 hidden sm:block">
                Inject deterministic hydrological synthetic crests
              </p>
            </div>
          </div>

          {/* Target Location Selector */}
          <div className="flex items-center space-x-1 bg-slate-900/90 border border-slate-800 rounded-lg p-1 text-xs">
            <MapPin className="w-3.5 h-3.5 text-slate-400 ml-1.5" />
            <select
              value={selectedLocation}
              onChange={(e) => dispatch(setSelectedLocation(e.target.value))}
              disabled={isBusy}
              aria-label="Target Location for Simulation"
              className="bg-transparent text-slate-200 text-xs font-medium focus:outline-none cursor-pointer pr-2 py-0.5"
            >
              {LOCATIONS.map((loc) => (
                <option key={loc.id} value={loc.id} className="bg-slate-900 text-slate-200">
                  {loc.name} ({loc.basin})
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Right: 5 Scenario Action Buttons */}
        <div className="flex flex-wrap items-center gap-2 w-full lg:w-auto justify-center lg:justify-end">
          {SCENARIOS.map((opt) => {
            const Icon = opt.icon;
            const isActive = currentMode === opt.id;
            return (
              <button
                key={opt.id}
                id={`btn-sim-${opt.id.toLowerCase()}`}
                onClick={() => handleSelectScenario(opt.id)}
                disabled={isBusy}
                title={opt.desc}
                className={`relative flex items-center space-x-2 px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all duration-200 ${
                  isActive
                    ? opt.activeBg
                    : `bg-slate-900/80 text-slate-300 ${opt.borderCol} hover:bg-slate-800/80 hover:text-white`
                } ${isBusy ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer'}`}
              >
                {isBusy && isActive ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-current" />
                ) : (
                  <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-current' : opt.accentColor}`} />
                )}
                <span>{getLabel(opt)}</span>
                {isActive && (
                  <span className="w-1.5 h-1.5 rounded-full bg-current absolute top-1 right-1" />
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default SimulationBar;

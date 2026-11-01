import React from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import {
  setCoordinatorTab,
  setLanguage,
  setTheme,
  setActiveHazard as setUiHazard,
  SupportedLanguage,
  CoordinatorTab,
  HazardType,
} from '../features/ui/uiSlice';
import { setActiveHazard as setMapHazard } from '../features/map/mapSlice';
import { setRole } from '../features/auth/authSlice';
import { CoordinatorOverviewMapView } from './CoordinatorOverviewMapView';
import { CoordinatorPointDetailView } from './CoordinatorPointDetailView';
import { CoordinatorBroadcastComposerView } from './CoordinatorBroadcastComposerView';
import { CoordinatorReportsInboxView } from './CoordinatorReportsInboxView';
import { CoordinatorBroadcastHistoryView } from './CoordinatorBroadcastHistoryView';
import { CoordinatorFeedsStatusView } from './CoordinatorFeedsStatusView';
import { CoordinatorEvaluationView } from './CoordinatorEvaluationView';
import { CoordinatorSettingsRulesView } from './CoordinatorSettingsRulesView';
import { CoordinatorSignInView } from './CoordinatorSignInView';

export const CoordinatorDashboard: React.FC = () => {
  const dispatch = useAppDispatch();
  const currentTab = useAppSelector((state) => state.ui.coordinatorTab) || 'map';
  const language = useAppSelector((state) => state.ui.language);
  const theme = useAppSelector((state) => state.ui.theme);
  const activeHazard: HazardType = useAppSelector((state) => state.ui.activeHazard) || 'FLOOD';
  const coordinatorToken = useAppSelector((state) => state.auth.coordinatorToken);

  const handleSelectHazard = (h: HazardType) => {
    dispatch(setUiHazard(h));
    dispatch(setMapHazard(h));
  };

  const navItems: { id: CoordinatorTab; label: string; icon: string; badge?: string }[] = [
    { id: 'map', label: 'Map', icon: 'map' },
    { id: 'point-detail', label: 'Point Detail', icon: 'ssid_chart' },
    { id: 'reports', label: 'Reports', icon: 'assignment', badge: '14' },
    { id: 'broadcast', label: 'Broadcast', icon: 'cell_tower', badge: '2' },
    { id: 'history', label: 'History', icon: 'history' },
    { id: 'feeds', label: 'Feeds', icon: 'rss_feed' },
    { id: 'evaluation', label: 'Evaluation', icon: 'insights' },
    { id: 'settings', label: 'Settings', icon: 'tune' },
    { id: 'sign-in', label: coordinatorToken ? 'Session' : 'Sign In', icon: 'verified_user' },
  ];

  return (
    <div className="flex flex-col min-h-screen w-full bg-slate-50 dark:bg-[#0B1118] text-slate-900 dark:text-[#EAF0F6] select-none">
      {/* ================= TOP BAR SHELL (STITCH) ================= */}
      <header className="h-14 bg-white dark:bg-[#151D27] border-b border-slate-200 dark:border-[#2A3644] flex items-center justify-between px-4 z-40 shrink-0">
        <div className="flex items-center gap-6">
          {/* Logo & OPS Badge */}
          <div className="flex items-center gap-2.5">
            <div className={`w-8 h-8 rounded-lg border flex items-center justify-center font-bold transition-colors ${
              activeHazard === 'FLOOD'
                ? 'bg-blue-50 dark:bg-[#1C2633] border-blue-200 dark:border-blue-500/30 text-blue-600 dark:text-[#7AA7FF]'
                : activeHazard === 'HEAT'
                ? 'bg-amber-50 dark:bg-[#1C2633] border-amber-200 dark:border-amber-500/30 text-amber-600 dark:text-amber-400'
                : 'bg-emerald-50 dark:bg-[#1C2633] border-emerald-200 dark:border-emerald-500/30 text-emerald-600 dark:text-emerald-400'
            }`}>
              <span className="material-symbols-outlined text-[20px]">
                {activeHazard === 'FLOOD' ? 'flood' : activeHazard === 'HEAT' ? 'thermostat' : 'air'}
              </span>
            </div>
            <div className="flex items-baseline gap-2">
              <span className="font-bold text-lg tracking-tight text-slate-900 dark:text-[#EAF0F6]">Kestrel</span>
              <span className="text-[10px] font-semibold text-slate-600 dark:text-[#9AA8B8] bg-slate-100 dark:bg-[#1C2633] px-2 py-0.5 rounded-full border border-slate-200 dark:border-[#2A3644]">
                COORDINATOR OPS
              </span>
            </div>
          </div>

          {/* Hazard Toggle */}
          <div className="hidden sm:flex items-center bg-slate-100 dark:bg-[#0B1118] p-1 rounded-xl border border-slate-200 dark:border-[#2A3644] text-xs font-semibold">
            <button
              onClick={() => handleSelectHazard('FLOOD')}
              className={`px-3 py-1 rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer ${
                activeHazard === 'FLOOD'
                  ? 'bg-white dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-bold shadow-xs border border-blue-200 dark:border-blue-500/30'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              <span className="material-symbols-outlined text-sm">waves</span>
              Flood
            </button>
            <button
              onClick={() => handleSelectHazard('HEAT')}
              className={`px-3 py-1 rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer ${
                activeHazard === 'HEAT'
                  ? 'bg-white dark:bg-[#1C2633] text-amber-700 dark:text-amber-400 font-bold shadow-xs border border-amber-200 dark:border-amber-500/30'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              <span className="material-symbols-outlined text-sm">thermostat</span>
              Heat
            </button>
            <button
              onClick={() => handleSelectHazard('AIR_QUALITY')}
              className={`px-3 py-1 rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer ${
                activeHazard === 'AIR_QUALITY'
                  ? 'bg-white dark:bg-[#1C2633] text-emerald-700 dark:text-emerald-400 font-bold shadow-xs border border-emerald-200 dark:border-emerald-500/30'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              <span className="material-symbols-outlined text-sm">air</span>
              Air
            </button>
          </div>
        </div>

        {/* Center/Right Guidance Note & Controls */}
        <div className="flex items-center gap-3 sm:gap-4">
          <div className="hidden lg:flex items-center gap-1.5 text-xs text-slate-600 dark:text-[#9AA8B8] bg-slate-100 dark:bg-[#1C2633] px-3 py-1 rounded-full border border-slate-200 dark:border-[#2A3644]">
            <span className="material-symbols-outlined text-sm text-blue-600 dark:text-[#7AA7FF]">info</span>
            <span>Relay guidance &amp; decision portal — not statutory warning authority</span>
          </div>

          {/* Language Switcher */}
          <div className="flex items-center gap-1 text-xs font-medium bg-slate-100 dark:bg-[#1C2633] px-2 py-1 rounded-xl border border-slate-200 dark:border-[#2A3644]">
            {(['en', 'hi', 'mr'] as SupportedLanguage[]).map((lang, idx) => (
              <React.Fragment key={lang}>
                {idx > 0 && <span className="text-slate-300 dark:text-[#2A3644]">|</span>}
                <button
                  onClick={() => dispatch(setLanguage(lang))}
                  className={`px-1 py-0.5 transition-colors ${
                    language === lang ? 'text-blue-700 dark:text-[#7AA7FF] font-bold' : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
                  }`}
                >
                  {lang === 'en' ? 'EN' : lang === 'hi' ? 'हिन्दी' : 'मराठी'}
                </button>
              </React.Fragment>
            ))}
          </div>

          {/* Theme Toggle */}
          <button
            onClick={() => dispatch(setTheme(theme === 'dark' ? 'light' : 'dark'))}
            className="w-8 h-8 rounded-lg bg-slate-100 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#EAF0F6] flex items-center justify-center transition-colors"
            title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
          >
            <span className="material-symbols-outlined text-amber-500 text-sm">
              {theme === 'dark' ? 'light_mode' : 'dark_mode'}
            </span>
          </button>

          {/* User Profile / Session */}
          <div
            onClick={() => dispatch(setCoordinatorTab('sign-in'))}
            className="flex items-center gap-2 pl-2 border-l border-slate-200 dark:border-[#2A3644] cursor-pointer group"
          >
            <div className="w-8 h-8 rounded-full bg-blue-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-center text-xs font-bold text-blue-700 dark:text-[#EAF0F6] group-hover:border-blue-500 dark:group-hover:border-[#7AA7FF]">
              AK
            </div>
            <div className="hidden xl:block text-left text-xs leading-tight">
              <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Anil Kamble</div>
              <div className="text-[10px] text-slate-500 dark:text-[#9AA8B8]">Pune District EOC</div>
            </div>
          </div>

          {/* Switch to Resident Public View */}
          <button
            onClick={() => dispatch(setRole('RESIDENT'))}
            className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-slate-200 dark:border-[#2A3644] text-xs font-semibold text-slate-700 dark:text-[#EAF0F6] flex items-center gap-1.5 transition-colors"
            title="Switch to Resident Public View"
          >
            <span className="material-symbols-outlined text-sm text-blue-600 dark:text-[#7AA7FF]">person</span>
            <span className="hidden sm:inline">Resident View</span>
          </button>
        </div>
      </header>

      {/* ================= WORKSPACE BODY (LEFT RAIL + MAIN CANVAS) ================= */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left Navigation Rail */}
        <aside className="w-56 bg-white dark:bg-[#151D27] border-r border-slate-200 dark:border-[#2A3644] flex flex-col justify-between shrink-0 z-20">
          <div className="p-3 space-y-1">
            <div className="text-[11px] font-bold text-slate-400 dark:text-[#65778B] tracking-wider uppercase px-3 py-1.5">
              Coordinator Relay
            </div>

            {navItems.map((item) => {
              const isActive = currentTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => dispatch(setCoordinatorTab(item.id))}
                  className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition-all ${
                    isActive
                      ? 'bg-blue-50 dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-bold border border-blue-200 dark:border-blue-500/30 shadow-xs'
                      : 'text-slate-600 dark:text-[#9AA8B8] hover:bg-slate-50 dark:hover:bg-[#1C2633]/60 hover:text-slate-900 dark:hover:text-[#EAF0F6]'
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    <span className="material-symbols-outlined text-[18px]">{item.icon}</span>
                    <span>{item.label}</span>
                  </div>
                  {item.badge && (
                    <span className="text-[10px] bg-slate-100 dark:bg-[#1C2633] text-slate-600 dark:text-[#9AA8B8] border border-slate-200 dark:border-[#2A3644] px-1.5 py-0.5 rounded-full font-bold">
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </div>

          {/* Bottom Jurisdiction Footer */}
          <div className="p-3 border-t border-slate-200 dark:border-[#2A3644] space-y-1.5 text-xs bg-slate-50/80 dark:bg-[#0B1118]/80">
            <div className="flex items-center justify-between text-slate-600 dark:text-[#9AA8B8]">
              <span className="font-medium">Active Jurisdiction</span>
              <span className="text-slate-900 dark:text-[#EAF0F6] font-bold">Pune Urban</span>
            </div>
            <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8] leading-tight">
              CAP-v1.2 Relay Node ID:{' '}
              <span className="font-mono text-blue-700 dark:text-[#7AA7FF]">IN-MH-PUN-04</span>
            </div>
          </div>
        </aside>

        {/* Main Canvas Router */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 custom-scrollbar bg-slate-50 dark:bg-[#0B1118]">
          {currentTab === 'map' && <CoordinatorOverviewMapView />}
          {currentTab === 'point-detail' && <CoordinatorPointDetailView />}
          {currentTab === 'broadcast' && <CoordinatorBroadcastComposerView />}
          {currentTab === 'reports' && <CoordinatorReportsInboxView />}
          {currentTab === 'history' && <CoordinatorBroadcastHistoryView />}
          {currentTab === 'feeds' && <CoordinatorFeedsStatusView />}
          {currentTab === 'evaluation' && <CoordinatorEvaluationView />}
          {currentTab === 'settings' && <CoordinatorSettingsRulesView />}
          {currentTab === 'sign-in' && <CoordinatorSignInView />}
        </main>
      </div>
    </div>
  );
};

export default CoordinatorDashboard;

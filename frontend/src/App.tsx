import React, { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from './app/hooks';
import {
  setActiveTab,
  setActiveHazard as setUiHazard,
  setTheme,
  AppTab,
  HazardType,
} from './features/ui/uiSlice';
import { setActiveHazard as setMapHazard } from './features/map/mapSlice';
import {
  MapOverviewView,
  PointDetailView,
  HistoryView,
  SubscribeView,
  ManualsView,
  AboutView,
} from './views';
import { ToastContainer, ErrorBoundary } from './components';

export const App: React.FC = () => {
  const dispatch = useAppDispatch();
  const activeTab = useAppSelector((state) => state.ui.activeTab) || 'map';
  const activeHazard = useAppSelector((state) => state.ui.activeHazard) || 'FLOOD';
  const theme = useAppSelector((state) => state.ui.theme);

  // Sync theme with HTML class for full Tailwind dark mode & color-scheme
  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
      document.documentElement.style.colorScheme = 'dark';
    } else {
      document.documentElement.classList.remove('dark');
      document.documentElement.style.colorScheme = 'light';
    }
  }, [theme]);

  const handleSelectHazard = (h: HazardType) => {
    dispatch(setUiHazard(h));
    dispatch(setMapHazard(h));
  };

  const navTabs: { id: AppTab; label: string; icon: string }[] = [
    { id: 'map', label: 'Map', icon: 'map' },
    { id: 'point-detail', label: 'Point Detail', icon: 'ssid_chart' },
    { id: 'history', label: 'History', icon: 'history' },
    { id: 'subscribe', label: 'Subscribe', icon: 'notifications_active' },
    { id: 'manuals', label: 'Manuals', icon: 'menu_book' },
    { id: 'about', label: 'About', icon: 'info' },
  ];

  return (
    <div
      className={`min-h-screen flex flex-col font-sans selection:bg-blue-600 selection:text-white transition-colors duration-200 ${
        theme === 'dark' ? 'bg-[#0B1118] text-[#EAF0F6]' : 'bg-[#F6F8FA] text-[#0F172A]'
      }`}
    >
      {/* ================= TOP HEADER (BRAND + HAZARD SWITCH + THEME TOGGLE) ================= */}
      <header className="h-14 bg-white dark:bg-[#151D27] border-b border-slate-200 dark:border-[#2A3644] flex items-center justify-between px-4 sm:px-6 z-40 shrink-0">
        <div className="flex items-center gap-4 sm:gap-6">
          {/* Logo & Platform Name */}
          <button
            onClick={() => dispatch(setActiveTab('map'))}
            className="flex items-center gap-2.5 focus:outline-none"
          >
            <div
              className={`w-8 h-8 rounded-lg border flex items-center justify-center font-bold transition-all shadow-xs ${
                activeHazard === 'FLOOD'
                  ? 'bg-blue-50 dark:bg-[#1C2633] border-blue-200 dark:border-blue-500/30 text-blue-600 dark:text-[#7AA7FF]'
                  : activeHazard === 'HEAT'
                  ? 'bg-amber-50 dark:bg-[#1C2633] border-amber-200 dark:border-amber-500/30 text-amber-600 dark:text-amber-400'
                  : 'bg-emerald-50 dark:bg-[#1C2633] border-emerald-200 dark:border-emerald-500/30 text-emerald-600 dark:text-emerald-400'
              }`}
            >
              <span className="material-symbols-outlined text-[20px]">
                {activeHazard === 'FLOOD' ? 'flood' : activeHazard === 'HEAT' ? 'thermostat' : 'air'}
              </span>
            </div>
            <div className="flex items-baseline gap-2">
              <span className="font-bold text-lg tracking-tight text-slate-900 dark:text-[#EAF0F6]">
                Kestrel
              </span>
              <span className="text-[10px] font-semibold text-slate-500 dark:text-[#9AA8B8] bg-slate-100 dark:bg-[#1C2633] px-2 py-0.5 rounded-full border border-slate-200 dark:border-[#2A3644] hidden sm:inline">
                CIVIC HAZARD RELAY
              </span>
            </div>
          </button>

          {/* HAZARD SWITCH (UP TOP IN THE HEADER) */}
          <div className="flex items-center bg-slate-100 dark:bg-[#0B1118] p-1 rounded-xl border border-slate-200 dark:border-[#2A3644] text-xs font-semibold">
            <button
              onClick={() => handleSelectHazard('FLOOD')}
              className={`px-3 py-1 rounded-lg flex items-center gap-1.5 transition-all cursor-pointer ${
                activeHazard === 'FLOOD'
                  ? 'bg-white dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-bold shadow-xs border border-blue-200 dark:border-blue-500/30'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              <span className="material-symbols-outlined text-sm">waves</span>
              <span>Flood</span>
            </button>

            <button
              onClick={() => handleSelectHazard('HEAT')}
              className={`px-3 py-1 rounded-lg flex items-center gap-1.5 transition-all cursor-pointer ${
                activeHazard === 'HEAT'
                  ? 'bg-white dark:bg-[#1C2633] text-amber-700 dark:text-amber-400 font-bold shadow-xs border border-amber-200 dark:border-amber-500/30'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              <span className="material-symbols-outlined text-sm">thermostat</span>
              <span>Heat</span>
            </button>

            <button
              onClick={() => handleSelectHazard('AIR_QUALITY')}
              className={`px-3 py-1 rounded-lg flex items-center gap-1.5 transition-all cursor-pointer ${
                activeHazard === 'AIR_QUALITY'
                  ? 'bg-white dark:bg-[#1C2633] text-emerald-700 dark:text-emerald-400 font-bold shadow-xs border border-emerald-200 dark:border-emerald-500/30'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              <span className="material-symbols-outlined text-sm">air</span>
              <span>Air</span>
            </button>
          </div>
        </div>

        {/* Right Side Controls: Rule Engine Status & Theme Toggle */}
        <div className="flex items-center gap-3">
          <div className="hidden md:flex items-center gap-1.5 text-xs text-slate-500 dark:text-[#9AA8B8] bg-slate-100 dark:bg-[#1C2633] px-3 py-1 rounded-full border border-slate-200 dark:border-[#2A3644]">
            <span className="material-symbols-outlined text-sm text-blue-600 dark:text-[#7AA7FF]">bolt</span>
            <span>Autonomous Rule Engine Active</span>
          </div>

          {/* Theme Toggle Button */}
          <button
            onClick={() => dispatch(setTheme(theme === 'dark' ? 'light' : 'dark'))}
            className="w-8 h-8 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#EAF0F6] flex items-center justify-center transition-colors shadow-xs"
            title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
          >
            <span className="material-symbols-outlined text-amber-500 text-sm">
              {theme === 'dark' ? 'light_mode' : 'dark_mode'}
            </span>
          </button>
        </div>
      </header>

      {/* ================= WORKSPACE BODY (LEFT SIDEBAR RAIL + MAIN CANVAS) ================= */}
      <div className="flex flex-1 overflow-hidden">
        {/* LEFT NAVIGATION SIDEBAR RAIL */}
        <aside className="w-56 bg-white dark:bg-[#151D27] border-r border-slate-200 dark:border-[#2A3644] hidden md:flex flex-col justify-between shrink-0 z-20">
          <div className="p-3 space-y-1">
            <div className="text-[11px] font-bold text-slate-400 dark:text-[#65778B] tracking-wider uppercase px-3 py-1.5">
              Civic Relay
            </div>

            {navTabs.map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => dispatch(setActiveTab(tab.id))}
                  className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                    isActive
                      ? 'bg-blue-50 dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-bold border border-blue-200 dark:border-blue-500/30 shadow-xs'
                      : 'text-slate-600 dark:text-[#9AA8B8] hover:bg-slate-50 dark:hover:bg-[#1C2633]/60 hover:text-slate-900 dark:hover:text-[#EAF0F6]'
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    <span className="material-symbols-outlined text-[18px]">{tab.icon}</span>
                    <span>{tab.label}</span>
                  </div>
                </button>
              );
            })}
          </div>

          {/* Bottom Sidebar Footer */}
          <div className="p-3 border-t border-slate-200 dark:border-[#2A3644] space-y-1 text-xs bg-slate-50/80 dark:bg-[#0B1118]/80">
            <div className="flex items-center justify-between text-slate-600 dark:text-[#9AA8B8]">
              <span className="font-medium text-[11px]">Coverage</span>
              <span className="text-slate-900 dark:text-[#EAF0F6] font-bold text-[11px]">All-India Basins</span>
            </div>
            <div className="text-[10px] text-slate-500 dark:text-[#9AA8B8] leading-tight">
              Relay Engine: <span className="font-mono text-blue-600 dark:text-[#7AA7FF]">Auto v2.4</span>
            </div>
          </div>
        </aside>

        {/* MAIN CANVAS */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 custom-scrollbar bg-slate-50 dark:bg-[#0B1118]">
          <ErrorBoundary fallbackTitle="View Failed to Render">
            {activeTab === 'map' && <MapOverviewView />}
            {activeTab === 'point-detail' && <PointDetailView />}
            {activeTab === 'history' && <HistoryView />}
            {activeTab === 'subscribe' && <SubscribeView />}
            {activeTab === 'manuals' && <ManualsView />}
            {activeTab === 'about' && <AboutView />}
          </ErrorBoundary>
        </main>
      </div>

      {/* Mobile Navigation Sub-bar (Only on small screens when left sidebar is hidden) */}
      <div className="md:hidden flex items-center justify-around border-t border-slate-200 dark:border-[#2A3644] py-1.5 px-2 bg-white dark:bg-[#151D27] shrink-0 z-30">
        {navTabs.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => dispatch(setActiveTab(tab.id))}
              className={`py-1 px-2 rounded-lg text-xs flex flex-col items-center gap-0.5 ${
                isActive ? 'text-blue-600 dark:text-[#7AA7FF] font-bold' : 'text-slate-500 dark:text-[#9AA8B8]'
              }`}
            >
              <span className="material-symbols-outlined text-[18px]">{tab.icon}</span>
              <span className="text-[10px]">{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Toast Feedback Notifications Container */}
      <ToastContainer />
    </div>
  );
};

export default App;

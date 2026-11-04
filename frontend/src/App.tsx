import React, { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from './app/hooks';
import {
  setActiveTab,
  setActiveHazard as setUiHazard,
  setTheme,
  toggleSidebar,
  AppTab,
  HazardType,
} from './features/ui/uiSlice';
import { setActiveHazard as setMapHazard } from './features/map/mapSlice';
import {
  MapOverviewView,
  PointDetailView,
  HistoryView,
  SubscribeView,
  ResidentMyAlertsView,
  ManualsView,
  AboutView,
} from './views';
import { ToastContainer, ErrorBoundary } from './components';
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react';

export const App: React.FC = () => {
  const dispatch = useAppDispatch();
  const activeTab = useAppSelector((state) => state.ui.activeTab) || 'map';
  const activeHazard = useAppSelector((state) => state.ui.activeHazard) || 'FLOOD';
  const theme = useAppSelector((state) => state.ui.theme);
  const sidebarCollapsed = useAppSelector((state) => state.ui.sidebarCollapsed);

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
    { id: 'my-alerts', label: 'My Alerts', icon: 'mark_chat_unread' },
    { id: 'manuals', label: 'Manuals', icon: 'menu_book' },
    { id: 'about', label: 'About', icon: 'info' },
  ];

  return (
    <div
      className={`h-screen w-screen flex flex-col overflow-hidden font-sans selection:bg-blue-600 selection:text-white transition-colors duration-200 ${
        theme === 'dark' ? 'bg-[#0B1118] text-[#EAF0F6]' : 'bg-[#F6F8FA] text-[#0F172A]'
      }`}
    >
      {/* ================= STICKY TOP HEADER ================= */}
      <header className="sticky top-0 z-40 h-14 w-full bg-white dark:bg-[#151D27] border-b border-slate-200 dark:border-[#2A3644] flex items-center justify-between px-4 sm:px-6 shrink-0 shadow-xs">
        {/* Left: Brand Logo & Title - Kestrel next to hazard icon, with Multi-Hazard Alert System next to it */}
        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={() => dispatch(setActiveTab('map'))}
            className="flex items-center gap-3 focus:outline-none cursor-pointer group text-left"
          >
            <div
              className={`w-10 h-10 rounded-xl border flex items-center justify-center font-bold transition-all shadow-xs shrink-0 ${
                activeHazard === 'FLOOD'
                  ? 'bg-blue-50 dark:bg-[#1C2633] border-blue-200 dark:border-blue-500/30 text-blue-600 dark:text-[#7AA7FF]'
                  : activeHazard === 'HEAT'
                  ? 'bg-amber-50 dark:bg-[#1C2633] border-amber-200 dark:border-amber-500/30 text-amber-600 dark:text-amber-400'
                  : 'bg-emerald-50 dark:bg-[#1C2633] border-emerald-200 dark:border-emerald-500/30 text-emerald-600 dark:text-emerald-400'
              }`}
            >
              <span className="material-symbols-outlined text-[24px]">
                {activeHazard === 'FLOOD' ? 'flood' : activeHazard === 'HEAT' ? 'thermostat' : 'air'}
              </span>
            </div>
            <div className="flex items-baseline gap-2.5">
              <span className="font-extrabold text-[26px] sm:text-[28px] tracking-tight text-slate-900 dark:text-[#EAF0F6] leading-none">
                Kestrel
              </span>
              <span className="text-sm sm:text-base font-semibold text-slate-500 dark:text-[#9AA8B8] hidden sm:inline whitespace-nowrap">
                Multi-Hazard Alert System
              </span>
            </div>
          </button>
        </div>

        {/* Center: Flood, Heat, Air hazard switch buttons placed right in the middle */}
        <div className="flex-1 flex justify-center px-2">
          <div className="flex items-center bg-slate-100 dark:bg-[#0B1118] p-1 rounded-xl border border-slate-200 dark:border-[#2A3644] text-sm font-semibold">
            <button
              onClick={() => handleSelectHazard('FLOOD')}
              className={`px-3.5 py-1.5 rounded-lg flex items-center gap-2 transition-all cursor-pointer text-sm ${
                activeHazard === 'FLOOD'
                  ? 'bg-white dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-bold shadow-xs border border-blue-200 dark:border-blue-500/30'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              <span className="material-symbols-outlined text-[17px]">waves</span>
              <span>Flood</span>
            </button>

            <button
              onClick={() => handleSelectHazard('HEAT')}
              className={`px-3.5 py-1.5 rounded-lg flex items-center gap-2 transition-all cursor-pointer text-sm ${
                activeHazard === 'HEAT'
                  ? 'bg-white dark:bg-[#1C2633] text-amber-700 dark:text-amber-400 font-bold shadow-xs border border-amber-200 dark:border-amber-500/30'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              <span className="material-symbols-outlined text-[17px]">thermostat</span>
              <span>Heat</span>
            </button>

            <button
              onClick={() => handleSelectHazard('AIR_QUALITY')}
              className={`px-3.5 py-1.5 rounded-lg flex items-center gap-2 transition-all cursor-pointer text-sm ${
                activeHazard === 'AIR_QUALITY'
                  ? 'bg-white dark:bg-[#1C2633] text-emerald-700 dark:text-emerald-400 font-bold shadow-xs border border-emerald-200 dark:border-emerald-500/30'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              <span className="material-symbols-outlined text-[17px]">air</span>
              <span>Air</span>
            </button>
          </div>
        </div>

        {/* Right: Theme Toggle Button - Bigger with text & non-orange icon */}
        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={() => dispatch(setTheme(theme === 'dark' ? 'light' : 'dark'))}
            className="h-9 px-3.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#EAF0F6] flex items-center gap-2 transition-all shadow-xs cursor-pointer text-sm font-semibold"
            title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
          >
            <span className="material-symbols-outlined text-[19px] text-slate-700 dark:text-[#EAF0F6]">
              {theme === 'dark' ? 'light_mode' : 'dark_mode'}
            </span>
            <span className="hidden sm:inline">
              {theme === 'dark' ? 'Light Mode' : 'Dark Mode'}
            </span>
          </button>
        </div>
      </header>

      {/* ================= WORKSPACE BODY (FIXED SIDEBAR + SCROLLABLE RIGHT CANVAS) ================= */}
      <div className="flex-1 flex overflow-hidden relative">
        {/* FIXED LEFT SIDEBAR RAIL (COLLAPSIBLE) */}
        <aside
          className={`fixed top-14 left-0 bottom-0 ${
            sidebarCollapsed ? 'w-16' : 'w-56'
          } bg-white dark:bg-[#151D27] border-r border-slate-200 dark:border-[#2A3644] hidden md:flex flex-col justify-between z-30 select-none transition-all duration-300 ease-in-out`}
        >
          <div>
            {/* Collapse/Expand Toggle Button - Rectangle Panel Type */}
            <div className={`p-3 pb-0 flex ${sidebarCollapsed ? 'justify-center' : 'justify-end'}`}>
              <button
                onClick={() => dispatch(toggleSidebar())}
                className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-[#1C2633] text-slate-500 dark:text-[#9AA8B8] flex items-center justify-center transition-colors cursor-pointer"
                title={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              >
                {sidebarCollapsed ? (
                  <PanelLeftOpen className="w-5 h-5" />
                ) : (
                  <PanelLeftClose className="w-5 h-5" />
                )}
              </button>
            </div>

            {/* Navigation Buttons - Pushed down by the height of the filter modal */}
            <div
              className={`p-2.5 ${
                sidebarCollapsed ? 'pt-28' : 'pt-32'
              } space-y-1.5 overflow-y-auto custom-scrollbar transition-all duration-300`}
            >
              {navTabs.map((tab) => {
                const isActive = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    onClick={() => dispatch(setActiveTab(tab.id))}
                    title={tab.label}
                    className={`w-full flex items-center ${
                      sidebarCollapsed ? 'justify-center px-0 py-2.5' : 'justify-between px-3.5 py-2.5'
                    } rounded-xl text-sm font-semibold transition-all cursor-pointer ${
                      isActive
                        ? 'bg-blue-50 dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-bold border border-blue-200 dark:border-blue-500/30 shadow-xs'
                        : 'text-slate-600 dark:text-[#9AA8B8] hover:bg-slate-50 dark:hover:bg-[#1C2633]/60 hover:text-slate-900 dark:hover:text-[#EAF0F6]'
                    }`}
                  >
                    <div className={`flex items-center ${sidebarCollapsed ? 'justify-center' : 'gap-3'}`}>
                      <span className="material-symbols-outlined text-[20px] shrink-0">{tab.icon}</span>
                      {!sidebarCollapsed && <span className="truncate">{tab.label}</span>}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Bottom Sidebar Status Card */}
          {!sidebarCollapsed ? (
            <div className="p-3.5 border-t border-slate-200 dark:border-[#2A3644] text-xs bg-slate-50/80 dark:bg-[#0B1118]/80 shrink-0 transition-opacity duration-200">
              <div className="flex items-center justify-between text-slate-600 dark:text-[#9AA8B8]">
                <span className="font-medium text-xs">Coverage</span>
                <span className="text-slate-900 dark:text-[#EAF0F6] font-bold text-xs">10 Major River Basins</span>
              </div>
            </div>
          ) : (
            <div
              className="p-3 border-t border-slate-200 dark:border-[#2A3644] text-xs bg-slate-50/80 dark:bg-[#0B1118]/80 shrink-0 flex items-center justify-center cursor-pointer"
              title="Coverage: 10 Major River Basins"
              onClick={() => dispatch(toggleSidebar())}
            >
              <span className="material-symbols-outlined text-[20px] text-slate-500 dark:text-[#9AA8B8]">
                public
              </span>
            </div>
          )}
        </aside>

        {/* SCROLLABLE RIGHT CANVAS (ONLY PART THAT SCROLLS) */}
        <div
          className={`flex-1 ${
            sidebarCollapsed ? 'md:ml-16' : 'md:ml-56'
          } h-[calc(100vh-3.5rem)] ${
            activeTab === 'map'
              ? 'overflow-hidden p-2 sm:p-3 pb-2 flex flex-col'
              : 'overflow-y-auto custom-scrollbar p-4 sm:p-6 pb-16 md:pb-6'
          } bg-slate-50 dark:bg-[#0B1118] transition-all duration-300 ease-in-out`}
        >
          <main className={`w-full max-w-7xl mx-auto ${activeTab === 'map' ? 'h-full flex flex-col flex-1 min-h-0' : ''}`}>
            <ErrorBoundary fallbackTitle="View Failed to Render">
              {activeTab === 'map' && <MapOverviewView />}
              {activeTab === 'point-detail' && <PointDetailView />}
              {activeTab === 'history' && <HistoryView />}
              {activeTab === 'subscribe' && <SubscribeView />}
              {activeTab === 'my-alerts' && <ResidentMyAlertsView />}
              {activeTab === 'manuals' && <ManualsView />}
              {activeTab === 'about' && <AboutView />}
            </ErrorBoundary>
          </main>
        </div>
      </div>

      {/* Mobile Navigation Sub-bar (Only on mobile viewport when sidebar is hidden) */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 bg-white dark:bg-[#151D27] border-t border-slate-200 dark:border-[#2A3644] py-1.5 px-2 flex items-center justify-around z-40 shadow-lg">
        {navTabs.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => dispatch(setActiveTab(tab.id))}
              className={`py-1 px-2 rounded-lg text-xs flex flex-col items-center gap-0.5 cursor-pointer ${
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

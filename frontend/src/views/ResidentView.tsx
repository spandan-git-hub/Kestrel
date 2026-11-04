import React, { useState, useMemo } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setResidentTab, setLanguage, setTheme, SupportedLanguage, ResidentTab } from '../features/ui/uiSlice';
import { setSelectedWardId } from '../features/resident/residentSlice';
import { setRole } from '../features/auth/authSlice';
import { ResidentStatusView } from './ResidentStatusView';
import { ResidentSubscribeView } from './ResidentSubscribeView';
import { ResidentReportView } from './ResidentReportView';
import { ResidentAboutView } from './ResidentAboutView';
import { useGetLocationsQuery } from '../api/registryApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import { ALL_HEAT_STATIONS } from '../catalog/heatStations';
import { ALL_AQ_STATIONS } from '../catalog/aqStations';
import type { LocationDto } from '../types/api';

export const ResidentView: React.FC = () => {
  const dispatch = useAppDispatch();
  const currentTab = useAppSelector((state) => state.ui.residentTab) || 'status';
  const language = useAppSelector((state) => state.ui.language);
  const theme = useAppSelector((state) => state.ui.theme);
  const selectedWardId = useAppSelector((state) => state.resident.selectedWardId) || 'mumbai-kurla';

  const [isLocationModalOpen, setIsLocationModalOpen] = useState<boolean>(false);
  const [searchTerm, setSearchTerm] = useState<string>('');

  const { data: locationsData } = useGetLocationsQuery();
  const stations: LocationDto[] = useMemo(() => {
    const list: LocationDto[] = locationsData && locationsData.length > 0 ? [...locationsData] : [...ALL_45_RIVER_STATIONS];
    ALL_HEAT_STATIONS.forEach((h) => {
      if (!list.some((s) => s.id === h.id)) {
        list.push({
          id: h.id,
          name: h.name,
          state: h.state,
          latitude: h.latitude,
          longitude: h.longitude,
          riverBasin: h.climateZone,
          regionType: h.regionType,
          threshold2yr: 38,
          threshold5yr: 42,
          threshold20yr: 45,
          primaryLanguage: h.primaryLanguage,
          active: h.active,
        });
      }
    });
    ALL_AQ_STATIONS.forEach((a) => {
      if (!list.some((s) => s.id === a.id)) {
        list.push({
          id: a.id,
          name: a.name,
          state: a.state,
          latitude: a.latitude,
          longitude: a.longitude,
          riverBasin: a.zone,
          regionType: 'PLAINS',
          threshold2yr: 200,
          threshold5yr: 300,
          threshold20yr: 400,
          primaryLanguage: a.primaryLanguage,
          active: a.active,
        });
      }
    });
    return list;
  }, [locationsData]);
  const currentStation = stations.find((s) => s.id === selectedWardId) || stations[0];

  const filteredStations = stations.filter(
    (st) =>
      st.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      st.state.toLowerCase().includes(searchTerm.toLowerCase()) ||
      st.riverBasin.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const navItems: { id: ResidentTab; label: string; icon: string }[] = [
    { id: 'status', label: 'Status', icon: 'dashboard' },
    { id: 'subscribe', label: 'Subscribe', icon: 'notifications' },
    { id: 'report', label: 'Report', icon: 'add_a_photo' },
    { id: 'about', label: 'About', icon: 'info' },
  ];

  return (
    <div className="w-full min-h-screen flex flex-col font-sans transition-colors duration-200">
      {/* ================= RESIDENT TOP NAVIGATION BAR ================= */}
      <header className="w-full bg-white dark:bg-[#151D27] border-b border-slate-200 dark:border-[#2A3644] sticky top-0 z-40 shadow-sm transition-colors duration-200">
        <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 flex items-center justify-between h-16">
          {/* Brand & Links */}
          <div className="flex items-center gap-6 lg:gap-8">
            <button
              onClick={() => dispatch(setResidentTab('status'))}
              className="flex items-center gap-2.5 focus:outline-none"
            >
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#1D4ED8] to-blue-700 flex items-center justify-center text-white shadow-md shadow-blue-500/20">
                <span className="material-symbols-outlined text-[22px]">flood</span>
              </div>
              <div className="flex flex-col text-left">
                <span className="font-bold text-lg text-slate-900 dark:text-[#EAF0F6] tracking-tight">Kestrel</span>
                <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] font-medium tracking-wide">CIVIC ALERT RELAY</span>
              </div>
            </button>

            {/* Desktop Navigation Links */}
            <nav className="hidden md:flex items-center space-x-1 sm:space-x-2 text-xs font-semibold">
              {navItems.map((item) => {
                const isActive = currentTab === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => dispatch(setResidentTab(item.id))}
                    className={`px-3 py-2 rounded-xl transition-all flex items-center gap-1.5 ${
                      isActive
                        ? 'bg-blue-50 dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-bold border border-blue-200 dark:border-blue-500/30 shadow-xs'
                        : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6] hover:bg-slate-100 dark:hover:bg-[#1C2633]/60'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[17px]">{item.icon}</span>
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </nav>
          </div>

          {/* Trailing Controls: Location, Language, Theme, Role Switcher */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Location Selector Button */}
            <button
              onClick={() => setIsLocationModalOpen(!isLocationModalOpen)}
              className="h-10 px-3 rounded-xl bg-white dark:bg-[#1C2633] hover:bg-slate-50 dark:hover:bg-[#222E3E] border border-slate-200 dark:border-[#2A3644] text-slate-800 dark:text-[#EAF0F6] text-xs font-semibold flex items-center gap-1.5 transition-colors shadow-sm"
              title="Change Ward or River Station"
            >
              <span className="material-symbols-outlined text-red-500 dark:text-red-400 text-[18px]">location_on</span>
              <span className="max-w-[120px] sm:max-w-[180px] truncate">{currentStation.name} ▾</span>
            </button>

            {/* Language Selector */}
            <div className="hidden sm:flex items-center px-2 py-1 rounded-xl bg-white dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] text-xs font-semibold text-slate-600 dark:text-[#9AA8B8]">
              <span className="material-symbols-outlined text-sm mr-1 text-slate-400 dark:text-[#65778B]">language</span>
              {(['en', 'hi', 'mr'] as SupportedLanguage[]).map((lang, idx) => (
                <React.Fragment key={lang}>
                  {idx > 0 && <span className="text-slate-300 dark:text-[#2A3644] mx-1">|</span>}
                  <button
                    onClick={() => dispatch(setLanguage(lang))}
                    className={`px-1.5 py-0.5 rounded transition-colors ${
                      language === lang
                        ? 'text-blue-600 dark:text-[#7AA7FF] font-bold'
                        : 'hover:text-slate-900 dark:hover:text-[#EAF0F6]'
                    }`}
                  >
                    {lang === 'en' ? 'EN' : lang === 'hi' ? 'हिन्दी' : 'मराठी'}
                  </button>
                </React.Fragment>
              ))}
            </div>

            {/* Theme Toggle Button */}
            <button
              onClick={() => dispatch(setTheme(theme === 'dark' ? 'light' : 'dark'))}
              className="w-10 h-10 rounded-xl bg-white dark:bg-[#1C2633] hover:bg-slate-50 dark:hover:bg-[#222E3E] border border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#EAF0F6] flex items-center justify-center transition-colors shadow-sm"
              title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
            >
              <span className="material-symbols-outlined text-amber-500 dark:text-amber-400 text-[18px]">
                {theme === 'dark' ? 'light_mode' : 'dark_mode'}
              </span>
            </button>

            {/* Switch to Coordinator Mode Button */}
            <button
              onClick={() => dispatch(setRole('COORDINATOR'))}
              className="h-10 px-3 rounded-xl bg-[#1D4ED8] hover:bg-blue-600 text-white text-xs font-semibold flex items-center gap-1.5 transition-all shadow-md shadow-blue-500/20"
              title="Open Coordinator Operations Portal"
            >
              <span className="material-symbols-outlined text-[17px]">shield</span>
              <span className="hidden sm:inline">Coordinator Ops</span>
            </button>
          </div>
        </div>

        {/* Mobile Navigation Sub-bar */}
        <div className="md:hidden flex items-center justify-around border-t border-slate-200 dark:border-[#2A3644] py-1.5 px-2 bg-white dark:bg-[#0B1118]">
          {navItems.map((item) => {
            const isActive = currentTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => dispatch(setResidentTab(item.id))}
                className={`py-1 px-2.5 rounded-lg text-xs flex flex-col items-center gap-0.5 ${
                  isActive ? 'text-blue-600 dark:text-[#7AA7FF] font-bold' : 'text-slate-500 dark:text-[#9AA8B8]'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">{item.icon}</span>
                <span className="text-[10px]">{item.label}</span>
              </button>
            );
          })}
        </div>
      </header>

      {/* Main View Router */}
      <main className="flex-1 w-full max-w-7xl mx-auto p-4 sm:p-6">
        {currentTab === 'status' && <ResidentStatusView />}
        {currentTab === 'subscribe' && <ResidentSubscribeView />}
        {currentTab === 'report' && <ResidentReportView />}
        {currentTab === 'about' && <ResidentAboutView />}
      </main>

      {/* LOCATION SELECTION OVERLAY MODAL */}
      {isLocationModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 dark:bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-lg bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-[#2A3644]">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-red-500 dark:text-red-400">location_on</span>
                <h3 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">Select Ward / River Station</h3>
              </div>
              <button
                onClick={() => setIsLocationModalOpen(false)}
                className="text-slate-400 hover:text-slate-700 dark:text-[#9AA8B8] dark:hover:text-white"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            {/* Search Input */}
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-3 text-slate-400 dark:text-[#9AA8B8] text-[18px]">
                search
              </span>
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search Pune wards, river basins, or Indian stations..."
                className="w-full h-11 pl-10 pr-3 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-sm focus:border-blue-500 dark:focus:border-[#7AA7FF] focus:outline-none"
              />
            </div>

            {/* List of stations */}
            <div className="max-h-72 overflow-y-auto space-y-1.5 custom-scrollbar pr-1">
              {filteredStations.map((station) => {
                const isSelected = station.id === selectedWardId;
                return (
                  <div
                    key={station.id}
                    onClick={() => {
                      dispatch(setSelectedWardId(station.id));
                      setIsLocationModalOpen(false);
                    }}
                    className={`p-3 rounded-xl border cursor-pointer transition-all flex items-center justify-between ${
                      isSelected
                        ? 'bg-blue-50 dark:bg-blue-600/20 border-blue-500 text-blue-900 dark:text-white font-semibold'
                        : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
                    }`}
                  >
                    <div>
                      <div className="text-xs font-bold text-slate-900 dark:text-[#EAF0F6]">{station.name}</div>
                      <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                        {station.riverBasin} · {station.state}
                      </div>
                    </div>
                    {isSelected && (
                      <span className="material-symbols-outlined text-sm text-blue-600 dark:text-[#7AA7FF]">
                        check_circle
                      </span>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Footer */}
            <div className="pt-2 border-t border-slate-100 dark:border-[#2A3644] flex items-center justify-between text-xs text-slate-500 dark:text-[#9AA8B8]">
              <span>45 Hydrological stations monitored across India</span>
              <button
                onClick={() => setIsLocationModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-[#1C2633] hover:bg-slate-200 dark:hover:bg-[#253243] text-slate-800 dark:text-[#EAF0F6] font-semibold"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ResidentView;

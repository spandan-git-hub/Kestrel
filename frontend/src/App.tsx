import React from 'react';
import { useAppDispatch, useAppSelector } from './app/hooks';
import { setRole, UserRole } from './features/auth/authSlice';
import { setLanguage, SupportedLanguage } from './features/ui/uiSlice';
import { setSelectedLocation } from './features/map/mapSlice';
import { Shield, Radio, Activity, Globe, MapPin, Zap } from 'lucide-react';

export const App: React.FC = () => {
  const dispatch = useAppDispatch();
  const role = useAppSelector((state) => state.auth.role);
  const language = useAppSelector((state) => state.ui.language);
  const selectedLocation = useAppSelector((state) => state.map.selectedLocationId);
  const simulationMode = useAppSelector((state) => state.ui.simulationMode);

  return (
    <div className="min-h-screen bg-[#0b0f19] text-slate-100 flex flex-col">
      {/* Top Navbar */}
      <header className="border-b border-slate-800 bg-[#101726]/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-600/20 text-blue-400 rounded-lg border border-blue-500/30">
              <Radio className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-lg tracking-wider text-slate-100">KESTREL</span>
                <span className="text-xs px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20 font-medium">
                  PROTOTYPE
                </span>
              </div>
              <p className="text-xs text-slate-400">Multi-Hazard Alert Relay · India</p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {/* Language Selector */}
            <div className="flex items-center space-x-1 bg-slate-900 border border-slate-800 rounded-lg p-1 text-xs">
              <Globe className="w-3.5 h-3.5 text-slate-400 ml-1" />
              {(['en', 'hi', 'mr'] as SupportedLanguage[]).map((lang) => (
                <button
                  key={lang}
                  onClick={() => dispatch(setLanguage(lang))}
                  className={`px-2 py-1 rounded transition-colors ${
                    language === lang
                      ? 'bg-blue-600 text-white font-medium shadow-sm'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {lang.toUpperCase()}
                </button>
              ))}
            </div>

            {/* Role Switcher */}
            <div className="flex items-center space-x-1 bg-slate-900 border border-slate-800 rounded-lg p-1 text-xs">
              {(['COORDINATOR', 'RESIDENT'] as UserRole[]).map((r) => (
                <button
                  key={r}
                  onClick={() => dispatch(setRole(r))}
                  className={`px-2.5 py-1 rounded transition-colors font-medium ${
                    role === r
                      ? 'bg-emerald-600 text-white shadow-sm'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {r === 'COORDINATOR' ? 'Coordinator' : 'Resident'}
                </button>
              ))}
            </div>
          </div>
        </div>
      </header>

      {/* Main Content Scaffold */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 space-y-6">
        {/* Architecture Status Banner */}
        <div className="bg-gradient-to-r from-blue-950/40 via-slate-900/60 to-purple-950/40 border border-blue-800/30 rounded-xl p-5 shadow-lg">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center space-x-2">
                <Shield className="w-4 h-4 text-emerald-400" />
                <h1 className="text-base font-semibold text-slate-100">
                  Phase 0 Monorepo Scaffold · Redux Toolkit + RTK Query Active
                </h1>
              </div>
              <p className="text-sm text-slate-400">
                Connected to Spring Cloud Gateway (<span className="text-blue-400 font-mono">http://localhost:8080/api/v1</span>). All 5 client slices configured.
              </p>
            </div>
            <div className="flex items-center space-x-3 text-xs">
              <span className="flex items-center px-3 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 font-medium">
                <span className="w-2 h-2 rounded-full bg-emerald-500 mr-2 animate-ping" />
                Store Initialized
              </span>
            </div>
          </div>
        </div>

        {/* State Monitor Grid */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
            <div className="flex items-center space-x-2 text-slate-400 text-xs">
              <Zap className="w-4 h-4 text-yellow-400" />
              <span>Current Role</span>
            </div>
            <div className="text-lg font-bold text-slate-100">{role}</div>
            <div className="text-xs text-slate-500">authSlice.role</div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
            <div className="flex items-center space-x-2 text-slate-400 text-xs">
              <Globe className="w-4 h-4 text-blue-400" />
              <span>Active Language</span>
            </div>
            <div className="text-lg font-bold text-slate-100 uppercase">{language}</div>
            <div className="text-xs text-slate-500">uiSlice.language</div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
            <div className="flex items-center space-x-2 text-slate-400 text-xs">
              <MapPin className="w-4 h-4 text-emerald-400" />
              <span>Selected Location</span>
            </div>
            <div className="text-lg font-bold text-slate-100 truncate">{selectedLocation || 'None'}</div>
            <div className="text-xs text-slate-500">mapSlice.selectedLocationId</div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
            <div className="flex items-center space-x-2 text-slate-400 text-xs">
              <Activity className="w-4 h-4 text-purple-400" />
              <span>Simulation Mode</span>
            </div>
            <div className="text-lg font-bold text-slate-100 truncate">{simulationMode || 'None'}</div>
            <div className="text-xs text-slate-500">uiSlice.simulationMode</div>
          </div>
        </div>

        {/* Quick Location Switcher for testing mapSlice dispatch */}
        <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-4 space-y-3">
          <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Test Slice Dispatch (Pilot Locations)
          </div>
          <div className="flex flex-wrap gap-2">
            {[
              { id: 'mumbai-kurla', name: 'Mumbai Kurla (Mithi)' },
              { id: 'patna-ward-12', name: 'Patna Ward 12 (Ganges)' },
              { id: 'guwahati-brahmaputra', name: 'Guwahati (Brahmaputra)' },
              { id: 'cuttack-mahanadi', name: 'Cuttack (Mahanadi)' },
            ].map((loc) => (
              <button
                key={loc.id}
                onClick={() => dispatch(setSelectedLocation(loc.id))}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
                  selectedLocation === loc.id
                    ? 'bg-blue-600/20 border-blue-500 text-blue-300'
                    : 'bg-slate-800/50 border-slate-700 text-slate-400 hover:text-slate-200'
                }`}
              >
                {loc.name}
              </button>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
};

export default App;

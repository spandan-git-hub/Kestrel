import React from 'react';
import { useAppDispatch, useAppSelector } from './app/hooks';
import { setRole, UserRole } from './features/auth/authSlice';
import { setLanguage, SupportedLanguage } from './features/ui/uiSlice';
import { CoordinatorDashboard, ResidentView, LiveDispatchDrawer } from './views';
import { ToastContainer, ErrorBoundary } from './components';
import { Radio, Globe, Shield, Users } from 'lucide-react';

export const App: React.FC = () => {
  const dispatch = useAppDispatch();
  const role = useAppSelector((state) => state.auth.role);
  const language = useAppSelector((state) => state.ui.language);

  return (
    <div className="min-h-screen bg-[#0b0f19] text-slate-100 flex flex-col font-sans selection:bg-blue-600 selection:text-white">
      {/* Top Main Navigation Bar */}
      <header className="border-b border-slate-800 bg-[#101726]/90 backdrop-blur-md sticky top-0 z-50 shadow-md">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          {/* Logo & Product Brand */}
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-blue-600 to-indigo-600 text-white rounded-xl shadow-md shadow-blue-500/20">
              <Radio className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-extrabold text-lg tracking-wider text-slate-100">KESTREL</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-blue-500/15 text-blue-400 border border-blue-500/30 font-semibold tracking-wide">
                  MICROSERVICES PROTOTYPE
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Multi-Hazard Early Warning & Alert Relay · India
              </p>
            </div>
          </div>

          {/* Right Controls: Language & Role Switcher */}
          <div className="flex items-center space-x-3 sm:space-x-4">
            {/* Language Selector */}
            <div className="flex items-center space-x-1 bg-slate-900/90 border border-slate-800 rounded-xl p-1 text-xs">
              <Globe className="w-3.5 h-3.5 text-slate-400 ml-1.5 mr-0.5" />
              {(['en', 'hi', 'mr'] as SupportedLanguage[]).map((lang) => (
                <button
                  key={lang}
                  id={`btn-lang-${lang}`}
                  onClick={() => dispatch(setLanguage(lang))}
                  className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all ${
                    language === lang
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {lang === 'en' ? 'EN' : lang === 'hi' ? 'हिंदी' : 'मराठी'}
                </button>
              ))}
            </div>

            {/* Role Switcher */}
            <div className="flex items-center space-x-1 bg-slate-900/90 border border-slate-800 rounded-xl p-1 text-xs">
              {(['COORDINATOR', 'RESIDENT'] as UserRole[]).map((r) => {
                const isActive = role === r;
                return (
                  <button
                    key={r}
                    id={`btn-role-${r.toLowerCase()}`}
                    onClick={() => dispatch(setRole(r))}
                    className={`flex items-center space-x-1.5 px-3 py-1 rounded-lg font-semibold transition-all ${
                      isActive
                        ? r === 'COORDINATOR'
                          ? 'bg-blue-600 text-white shadow-sm'
                          : 'bg-purple-600 text-white shadow-sm'
                        : 'text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {r === 'COORDINATOR' ? (
                      <>
                        <Shield className="w-3.5 h-3.5" />
                        <span>Coordinator</span>
                      </>
                    ) : (
                      <>
                        <Users className="w-3.5 h-3.5" />
                        <span>Resident Public</span>
                      </>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </header>

      {/* Main View Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 pb-24 space-y-6">
        <ErrorBoundary fallbackTitle="View Failed to Render">
          {role === 'COORDINATOR' ? <CoordinatorDashboard /> : <ResidentView />}
        </ErrorBoundary>
      </main>

      {/* Persistent Live Dispatch Drawer at screen bottom */}
      <LiveDispatchDrawer />

      {/* Toast Feedback Notifications Container */}
      <ToastContainer />
    </div>
  );
};

export default App;

import React, { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from './app/hooks';
import { setRole, UserRole } from './features/auth/authSlice';
import { setLanguage, setTheme, SupportedLanguage } from './features/ui/uiSlice';
import { CoordinatorDashboard, ResidentView, LiveDispatchDrawer } from './views';
import { ToastContainer, ErrorBoundary } from './components';
import { Radio, Globe, Shield, Users, Sun, Moon } from 'lucide-react';

export const App: React.FC = () => {
  const dispatch = useAppDispatch();
  const role = useAppSelector((state) => state.auth.role);
  const language = useAppSelector((state) => state.ui.language);
  const theme = useAppSelector((state) => state.ui.theme);

  // Sync theme with HTML class
  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
      document.documentElement.style.colorScheme = 'dark';
    } else {
      document.documentElement.classList.remove('dark');
      document.documentElement.style.colorScheme = 'light';
    }
  }, [theme]);

  return (
    <div
      className={`min-h-screen flex flex-col font-sans selection:bg-blue-600 selection:text-white transition-colors duration-200 ${
        theme === 'dark' ? 'bg-[#0b0f19] text-slate-100' : 'bg-slate-50 text-slate-900'
      }`}
    >
      {/* Top Main Navigation Bar */}
      <header
        className={`border-b backdrop-blur-md sticky top-0 z-[2000] shadow-sm transition-colors duration-200 ${
          theme === 'dark'
            ? 'border-slate-800 bg-[#101726]/90 text-slate-100'
            : 'border-slate-200 bg-white/95 text-slate-900'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          {/* Logo & Product Brand */}
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-blue-600 to-indigo-600 text-white rounded-xl shadow-md shadow-blue-500/20">
              <Radio className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span
                  className={`font-extrabold text-lg tracking-wider ${
                    theme === 'dark' ? 'text-slate-100' : 'text-slate-900'
                  }`}
                >
                  KESTREL
                </span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-blue-500/15 text-blue-500 border border-blue-500/30 font-semibold tracking-wide">
                  MICROSERVICES PROTOTYPE
                </span>
              </div>
              <p className={`text-xs ${theme === 'dark' ? 'text-slate-400' : 'text-slate-500'}`}>
                Multi-Hazard Early Warning & Alert Relay · India
              </p>
            </div>
          </div>

          {/* Right Controls: Theme, Language & Role Switcher */}
          <div className="flex items-center space-x-2 sm:space-x-3">
            {/* Theme Toggle (Dark / Light) */}
            <button
              id="btn-theme-toggle"
              onClick={() => dispatch(setTheme(theme === 'dark' ? 'light' : 'dark'))}
              title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
              className={`flex items-center space-x-1.5 border rounded-xl px-2.5 py-1.5 text-xs transition-all shadow-sm ${
                theme === 'dark'
                  ? 'bg-slate-900/90 border-slate-800 text-slate-300 hover:text-white'
                  : 'bg-white border-slate-200 text-slate-700 hover:text-slate-900'
              }`}
            >
              {theme === 'dark' ? (
                <>
                  <Sun className="w-3.5 h-3.5 text-amber-400" />
                  <span className="font-semibold text-xs hidden sm:inline">Light</span>
                </>
              ) : (
                <>
                  <Moon className="w-3.5 h-3.5 text-blue-600" />
                  <span className="font-semibold text-xs hidden sm:inline">Dark</span>
                </>
              )}
            </button>

            {/* Language Selector */}
            <div
              className={`flex items-center space-x-1 border rounded-xl p-1 text-xs ${
                theme === 'dark' ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'
              }`}
            >
              <Globe
                className={`w-3.5 h-3.5 ml-1.5 mr-0.5 ${
                  theme === 'dark' ? 'text-slate-400' : 'text-slate-500'
                }`}
              />
              {(['en', 'hi', 'mr'] as SupportedLanguage[]).map((lang) => (
                <button
                  key={lang}
                  id={`btn-lang-${lang}`}
                  onClick={() => dispatch(setLanguage(lang))}
                  className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all ${
                    language === lang
                      ? 'bg-blue-600 text-white shadow-sm'
                      : theme === 'dark'
                      ? 'text-slate-400 hover:text-slate-200'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {lang === 'en' ? 'EN' : lang === 'hi' ? 'हिंदी' : 'मराठी'}
                </button>
              ))}
            </div>

            {/* Role Switcher */}
            <div
              className={`flex items-center space-x-1 border rounded-xl p-1 text-xs ${
                theme === 'dark' ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'
              }`}
            >
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
                        : theme === 'dark'
                        ? 'text-slate-400 hover:text-slate-200'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    {r === 'COORDINATOR' ? (
                      <>
                        <Shield className="w-3.5 h-3.5" />
                        <span className="hidden sm:inline">Coordinator</span>
                        <span className="sm:hidden">Coord</span>
                      </>
                    ) : (
                      <>
                        <Users className="w-3.5 h-3.5" />
                        <span className="hidden sm:inline">Resident Public</span>
                        <span className="sm:hidden">Resident</span>
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

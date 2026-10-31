import React, { useEffect } from 'react';
import { useAppSelector } from './app/hooks';
import { CoordinatorDashboard, ResidentView, LiveDispatchDrawer, BroadcastModal } from './views';
import { ToastContainer, ErrorBoundary } from './components';

export const App: React.FC = () => {
  const role = useAppSelector((state) => state.auth.role);
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

  return (
    <div
      className={`min-h-screen flex flex-col font-sans selection:bg-blue-600 selection:text-white transition-colors duration-200 ${
        theme === 'dark' ? 'bg-[#0B1118] text-[#EAF0F6]' : 'bg-[#F6F8FA] text-[#0F172A]'
      }`}
    >
      <ErrorBoundary fallbackTitle="View Failed to Render">
        {role === 'COORDINATOR' ? <CoordinatorDashboard /> : <ResidentView />}
      </ErrorBoundary>

      {/* Persistent Live Dispatch Drawer at screen bottom */}
      <LiveDispatchDrawer />

      {/* Broadcast Modal Dialog when triggered from map/shortcuts */}
      <BroadcastModal />

      {/* Toast Feedback Notifications Container */}
      <ToastContainer />
    </div>
  );
};

export default App;

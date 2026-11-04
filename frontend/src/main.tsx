import React from 'react';
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import { store } from './app/store';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </React.StrictMode>
);

// Register Kestrel Service Worker for Web Push & Offline Alerting
if (typeof window !== 'undefined' && 'serviceWorker' in navigator) {
  const registerSw = () => {
    navigator.serviceWorker
      .register('/sw.js')
      .then((reg) => {
        console.log('[Kestrel SW] ServiceWorker active with scope:', reg.scope);
      })
      .catch((err) => {
        console.warn('[Kestrel SW] ServiceWorker registration fallback:', err);
      });
  };

  if (document.readyState === 'complete' || document.readyState === 'interactive') {
    registerSw();
  } else {
    window.addEventListener('load', registerSw);
  }
}

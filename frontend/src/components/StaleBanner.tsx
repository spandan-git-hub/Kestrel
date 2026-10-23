import React from 'react';
import { AlertTriangle, RefreshCw, ShieldAlert } from 'lucide-react';
import type { SupportedLanguage } from '../types/api';

export interface StaleBannerProps {
  isStale?: boolean;
  dataAgeSeconds?: number | null;
  dataAsOf?: string | null;
  thresholdHours?: number;
  onRefresh?: () => void;
  isRefreshing?: boolean;
  language?: SupportedLanguage;
  className?: string;
}

const TEXTS: Record<
  SupportedLanguage,
  {
    title: string;
    description: string;
    refreshBtn: string;
    refreshing: string;
    lastSynced: string;
  }
> = {
  en: {
    title: 'STALE MODEL DATA WARNING (>36h)',
    description:
      'Numerical hydrological ensemble forecasts have not updated in over 36 hours. Current river discharge may differ significantly from model estimates. Verify with resident ground reports or river gauges before issuing alerts.',
    refreshBtn: 'Sync Live Forecast',
    refreshing: 'Syncing Forecast...',
    lastSynced: 'Last updated:',
  },
  hi: {
    title: 'पुराने मॉडल डेटा की चेतावनी (>36 घंटे)',
    description:
      'हाइड्रोलॉजिकल मॉडल पूर्वानुमान 36 घंटे से अधिक समय से अपडेट नहीं हुआ है। नदी का वास्तविक जलस्तर मॉडल अनुमानों से भिन्न हो सकता है। आपातकालीन निर्णय लेने से पूर्व जमीनी रिपोर्ट या स्थानीय गेज से सत्यापन करें।',
    refreshBtn: 'लाइव पूर्वानुमान सिंक करें',
    refreshing: 'सिंक हो रहा है...',
    lastSynced: 'अंतिम अपडेट:',
  },
  mr: {
    title: 'कालबाह्य मॉडेल डेटा इशारा (>36 तास)',
    description:
      'जलविज्ञान मॉडेल अंदाज ३६ तासांपेक्षा जास्त वेळेपासून अद्ययावत झालेला नाही. नदीचा प्रत्यक्ष विसर्ग मॉडेलच्या अंदाजापेक्षा भिन्न असू शकतो. कोणताही निर्णय घेण्यापूर्वी प्रत्यक्ष तपासणी किंवा स्थानिक गेज अहवाल तपासा.',
    refreshBtn: 'थेट अंदाज सिंक करा',
    refreshing: 'सिंक होत आहे...',
    lastSynced: 'शेवटचे अपडेट:',
  },
};

export const StaleBanner: React.FC<StaleBannerProps> = ({
  isStale = false,
  dataAgeSeconds,
  dataAsOf,
  thresholdHours = 36,
  onRefresh,
  isRefreshing = false,
  language = 'en',
  className = '',
}) => {
  const isDataStale =
    isStale || (typeof dataAgeSeconds === 'number' && dataAgeSeconds >= thresholdHours * 3600);

  if (!isDataStale) {
    return null;
  }

  const t = TEXTS[language] || TEXTS.en;

  const formattedDate = dataAsOf
    ? new Date(dataAsOf).toLocaleString(
        language === 'hi' || language === 'mr' ? 'hi-IN' : 'en-IN',
        {
          dateStyle: 'medium',
          timeStyle: 'short',
        }
      )
    : null;

  return (
    <div
      role="alert"
      className={`relative overflow-hidden rounded-xl border border-red-500/40 bg-gradient-to-r from-red-950/40 via-amber-950/20 to-red-950/40 p-4 backdrop-blur-md shadow-lg shadow-red-950/30 text-slate-100 ${className}`}
    >
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-start space-x-3.5">
          <div className="p-2.5 rounded-lg bg-red-500/20 text-red-400 border border-red-500/30 shrink-0 mt-0.5">
            <ShieldAlert className="w-5 h-5 animate-pulse" />
          </div>
          <div className="space-y-1">
            <div className="flex items-center space-x-2">
              <span className="font-bold text-sm tracking-wide text-red-400 flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4 text-red-400" />
                {t.title}
              </span>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed max-w-3xl">
              {t.description}
            </p>
            {formattedDate && (
              <p className="text-[11px] text-slate-400 font-mono">
                {t.lastSynced} <span className="text-slate-200">{formattedDate}</span>
              </p>
            )}
          </div>
        </div>

        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="shrink-0 inline-flex items-center space-x-2 px-3.5 py-2 rounded-lg bg-red-600/30 hover:bg-red-600/40 border border-red-500/50 text-red-200 text-xs font-medium transition-all shadow-sm active:scale-95 disabled:opacity-50 disabled:pointer-events-none"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
            <span>{isRefreshing ? t.refreshing : t.refreshBtn}</span>
          </button>
        )}
      </div>
    </div>
  );
};

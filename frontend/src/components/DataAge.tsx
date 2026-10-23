import React from 'react';
import { Clock, AlertTriangle } from 'lucide-react';
import type { SupportedLanguage } from '../types/api';

export interface DataAgeProps {
  timestamp?: string | number | Date | null;
  seconds?: number | null;
  isStale?: boolean;
  thresholdHours?: number;
  language?: SupportedLanguage;
  showIcon?: boolean;
  className?: string;
}

function calculateSeconds(
  timestamp?: string | number | Date | null,
  seconds?: number | null
): number {
  if (typeof seconds === 'number' && !isNaN(seconds) && seconds >= 0) {
    return Math.floor(seconds);
  }
  if (!timestamp) {
    return 0;
  }
  const date = typeof timestamp === 'string' || typeof timestamp === 'number'
    ? new Date(timestamp)
    : timestamp;
  const now = Date.now();
  const diffMs = now - date.getTime();
  return Math.max(0, Math.floor(diffMs / 1000));
}

function formatAge(ageSec: number, lang: SupportedLanguage): string {
  if (ageSec < 60) {
    if (lang === 'hi') return 'अभी अपडेट किया गया';
    if (lang === 'mr') return 'आत्ताच अद्ययावत';
    return 'Updated just now';
  }
  const mins = Math.floor(ageSec / 60);
  if (mins < 60) {
    if (lang === 'hi') return `${mins} मिनट पूर्व अपडेट`;
    if (lang === 'mr') return `${mins} मि. पूर्वी अद्ययावत`;
    return `Updated ${mins}m ago`;
  }
  const hours = Math.floor(mins / 60);
  if (hours < 24) {
    if (lang === 'hi') return `${hours} घंटे पूर्व अपडेट`;
    if (lang === 'mr') return `${hours} तास पूर्वी अद्ययावत`;
    return `Updated ${hours}h ago`;
  }
  const days = Math.floor(hours / 24);
  if (lang === 'hi') return `${days} दिन पूर्व अपडेट`;
  if (lang === 'mr') return `${days} दिवस पूर्वी अद्ययावत`;
  return `Updated ${days}d ago`;
}

export const DataAge: React.FC<DataAgeProps> = ({
  timestamp,
  seconds,
  isStale: isStaleProp,
  thresholdHours = 36,
  language = 'en',
  showIcon = true,
  className = '',
}) => {
  const ageSec = calculateSeconds(timestamp, seconds);
  const thresholdSec = thresholdHours * 3600;
  const isStale = isStaleProp || ageSec >= thresholdSec;

  // Determine freshness tier
  let dotColor = 'bg-emerald-400';
  let textColor = 'text-slate-400';
  let pulse = false;

  if (isStale) {
    dotColor = 'bg-red-500';
    textColor = 'text-red-400 font-medium';
    pulse = true;
  } else if (ageSec > 24 * 3600) {
    dotColor = 'bg-orange-400';
    textColor = 'text-orange-400';
  } else if (ageSec > 6 * 3600) {
    dotColor = 'bg-amber-400';
    textColor = 'text-amber-400';
  }

  const formattedText = formatAge(ageSec, language);

  return (
    <div className={`inline-flex items-center space-x-1.5 text-xs ${textColor} ${className}`}>
      {showIcon && (
        isStale ? (
          <AlertTriangle className="w-3.5 h-3.5 text-red-400 shrink-0" />
        ) : (
          <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
        )
      )}
      <span className="relative flex h-2 w-2 items-center justify-center">
        {pulse && (
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75" />
        )}
        <span className={`inline-flex rounded-full h-1.5 w-1.5 ${dotColor}`} />
      </span>
      <span>{formattedText}</span>
      {isStale && (
        <span className="ml-1 px-1.5 py-0.2 rounded bg-red-500/20 text-red-400 border border-red-500/30 text-[10px] uppercase tracking-wider font-semibold">
          {language === 'hi' ? 'पुराना डेटा' : language === 'mr' ? 'कालबाह्य डेटा' : 'Stale (>36h)'}
        </span>
      )}
    </div>
  );
};

import React from 'react';
import { ShieldCheck, AlertTriangle, AlertCircle, Flame } from 'lucide-react';
import type { Severity, SupportedLanguage } from '../types/api';

export interface SeverityBadgeProps {
  severity: Severity;
  size?: 'xs' | 'sm' | 'md' | 'lg';
  showIcon?: boolean;
  showPulse?: boolean;
  customText?: string;
  language?: SupportedLanguage;
  className?: string;
}

const SEVERITY_CONFIG: Record<
  Severity,
  {
    icon: typeof ShieldCheck;
    containerClass: string;
    iconClass: string;
    labels: Record<SupportedLanguage, string>;
  }
> = {
  NONE: {
    icon: ShieldCheck,
    containerClass: 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400',
    iconClass: 'text-emerald-400',
    labels: {
      en: 'Normal / Safe',
      hi: 'सुरक्षित / सामान्य',
      mr: 'सुरक्षित / सामान्य',
    },
  },
  WATCH: {
    icon: AlertTriangle,
    containerClass: 'bg-amber-500/15 border-amber-500/30 text-amber-400',
    iconClass: 'text-amber-400',
    labels: {
      en: 'Watch: 2-Yr Risk',
      hi: 'सतर्कता: 2-वर्षीय जोखिम',
      mr: 'दक्षता: 2-वर्षीय धोका',
    },
  },
  WARNING: {
    icon: AlertCircle,
    containerClass: 'bg-orange-500/15 border-orange-500/40 text-orange-400',
    iconClass: 'text-orange-400',
    labels: {
      en: 'Warning: 5-Yr Flood',
      hi: 'चेतावनी: 5-वर्षीय बाढ़',
      mr: 'धोका इशारा: 5-वर्षीय पूर',
    },
  },
  SEVERE: {
    icon: Flame,
    containerClass: 'bg-red-500/20 border-red-500/50 text-red-400 shadow-sm shadow-red-950/50',
    iconClass: 'text-red-400',
    labels: {
      en: 'Severe: 20-Yr Inundation',
      hi: 'गंभीर: 20-वर्षीय जलप्लावन',
      mr: 'अतिगंभीर: 20-वर्षीय महापूर',
    },
  },
};

const SIZE_CONFIG = {
  xs: {
    padding: 'px-1.5 py-0.5 text-[10px]',
    iconSize: 'w-3 h-3',
    gap: 'gap-1',
  },
  sm: {
    padding: 'px-2 py-1 text-xs',
    iconSize: 'w-3.5 h-3.5',
    gap: 'gap-1.5',
  },
  md: {
    padding: 'px-2.5 py-1.5 text-xs',
    iconSize: 'w-4 h-4',
    gap: 'gap-1.5',
  },
  lg: {
    padding: 'px-3.5 py-2 text-sm',
    iconSize: 'w-5 h-5',
    gap: 'gap-2',
  },
};

export const SeverityBadge: React.FC<SeverityBadgeProps> = ({
  severity,
  size = 'md',
  showIcon = true,
  showPulse = true,
  customText,
  language = 'en',
  className = '',
}) => {
  const config = SEVERITY_CONFIG[severity] || SEVERITY_CONFIG.NONE;
  const sizeStyles = SIZE_CONFIG[size];
  const Icon = config.icon;
  const label = customText || config.labels[language] || config.labels.en;

  const isSevere = severity === 'SEVERE';

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full border tracking-wide transition-all ${config.containerClass} ${sizeStyles.padding} ${sizeStyles.gap} ${className}`}
      data-severity={severity}
    >
      {showIcon && (
        <span className="relative flex items-center justify-center">
          {isSevere && showPulse && (
            <span className="absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75 animate-ping" />
          )}
          <Icon className={`${sizeStyles.iconSize} ${config.iconClass} shrink-0`} />
        </span>
      )}
      <span>{label}</span>
    </span>
  );
};

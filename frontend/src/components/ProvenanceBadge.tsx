import React from 'react';
import { Cpu, Users, CheckCircle2 } from 'lucide-react';
import type { Provenance, SupportedLanguage } from '../types/api';

export interface ProvenanceBadgeProps {
  provenance: Provenance;
  size?: 'xs' | 'sm' | 'md' | 'lg';
  showIcon?: boolean;
  language?: SupportedLanguage;
  className?: string;
}

const PROVENANCE_CONFIG: Record<
  Provenance,
  {
    icon: typeof Cpu;
    containerClass: string;
    iconClass: string;
    labels: Record<SupportedLanguage, string>;
    description: Record<SupportedLanguage, string>;
  }
> = {
  MODEL_ONLY: {
    icon: Cpu,
    containerClass: 'bg-blue-50 dark:bg-blue-500/10 border-blue-200 dark:border-blue-500/30 text-blue-700 dark:text-blue-300',
    iconClass: 'text-blue-600 dark:text-blue-400',
    labels: {
      en: 'Model Guidance Only',
      hi: 'केवल मॉडल मार्गदर्शन',
      mr: 'केवळ मॉडेल अंदाज',
    },
    description: {
      en: 'Derived purely from numerical hydrological ensemble forecasts',
      hi: 'केवल संख्यात्मक हाइड्रोलॉजिकल मॉडल पूर्वानुमानों पर आधारित',
      mr: 'केवळ संख्यात्मक जलविज्ञान मॉडेल अंदाजांवर आधारित',
    },
  },
  MODEL_PLUS_REPORTS: {
    icon: Users,
    containerClass: 'bg-purple-50 dark:bg-purple-500/15 border-purple-200 dark:border-purple-500/40 text-purple-700 dark:text-purple-200 shadow-xs ring-1 ring-purple-200 dark:ring-purple-500/20',
    iconClass: 'text-purple-600 dark:text-purple-300',
    labels: {
      en: 'Model + Ground Reports',
      hi: 'मॉडल + जमीनी रिपोर्ट',
      mr: 'मॉडेल + प्रत्यक्ष अहवाल',
    },
    description: {
      en: 'Corroborated by verified community observations from local residents',
      hi: 'स्थानीय निवासियों के सत्यापित जमीनी अवलोकनों द्वारा पुष्ट',
      mr: 'स्थानिक नागरिकांच्या प्रत्यक्ष तपासणी व अहवालांद्वारे पुष्ट',
    },
  },
  OFFICIAL_CONFIRMED: {
    icon: CheckCircle2,
    containerClass: 'bg-emerald-50 dark:bg-emerald-500/15 border-emerald-200 dark:border-emerald-500/40 text-emerald-700 dark:text-emerald-200 ring-1 ring-emerald-200 dark:ring-emerald-500/20',
    iconClass: 'text-emerald-600 dark:text-emerald-300',
    labels: {
      en: 'Coordinator Confirmed',
      hi: 'समन्वयक द्वारा प्रमाणित',
      mr: 'समन्वयकांद्वारे पुष्टी',
    },
    description: {
      en: 'Reviewed and confirmed by municipal disaster management authority',
      hi: 'नगर निगम आपदा प्रबंधन समन्वयकों द्वारा समीक्षित एवं सत्यापित',
      mr: 'महापालिका आपत्ती व्यवस्थापन समन्वयकांद्वारे सत्यापित',
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
    padding: 'px-2 py-0.5 text-xs',
    iconSize: 'w-3.5 h-3.5',
    gap: 'gap-1.5',
  },
  md: {
    padding: 'px-2.5 py-1 text-xs',
    iconSize: 'w-4 h-4',
    gap: 'gap-1.5',
  },
  lg: {
    padding: 'px-3 py-1.5 text-sm',
    iconSize: 'w-4.5 h-4.5',
    gap: 'gap-2',
  },
};

export const ProvenanceBadge: React.FC<ProvenanceBadgeProps> = ({
  provenance,
  size = 'md',
  showIcon = true,
  language = 'en',
  className = '',
}) => {
  const config = PROVENANCE_CONFIG[provenance] || PROVENANCE_CONFIG.MODEL_ONLY;
  const sizeStyles = SIZE_CONFIG[size];
  const Icon = config.icon;
  const label = config.labels[language] || config.labels.en;
  const tooltip = config.description[language] || config.description.en;

  return (
    <span
      title={tooltip}
      className={`inline-flex items-center font-medium rounded-full border transition-all cursor-help ${config.containerClass} ${sizeStyles.padding} ${sizeStyles.gap} ${className}`}
      data-provenance={provenance}
    >
      {showIcon && <Icon className={`${sizeStyles.iconSize} ${config.iconClass} shrink-0`} />}
      <span>{label}</span>
    </span>
  );
};

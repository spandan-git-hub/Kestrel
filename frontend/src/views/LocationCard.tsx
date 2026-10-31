import React from 'react';
import { useAppDispatch } from '../app/hooks';
import { setSelectedLocation } from '../features/map/mapSlice';
import { openBroadcastModal } from '../features/broadcast/draftSlice';
import { SeverityBadge, ProvenanceBadge, DataAge } from '../components';
import { useGetReportsForLocationQuery } from '../api/fanoutApi';
import type { LocationAlertStatusResponse, LocationDto, SupportedLanguage } from '../types/api';
import {
  Radio,
  Share2,
  Waves,
  CheckCircle2,
} from 'lucide-react';

interface LocationCardProps {
  location: LocationDto;
  status?: LocationAlertStatusResponse;
  isSelected: boolean;
  language: SupportedLanguage;
}

export const LocationCard: React.FC<LocationCardProps> = ({
  location,
  status,
  isSelected,
  language,
}) => {
  const dispatch = useAppDispatch();
  const { data: reports } = useGetReportsForLocationQuery(location.id);

  const severity = status?.severity || 'NONE';
  const provenance = status?.provenance || 'MODEL_ONLY';
  const reportCount = reports?.length || 0;

  const handleSelect = () => {
    dispatch(setSelectedLocation(location.id));
  };

  const handleOpenBroadcast = (e: React.MouseEvent) => {
    e.stopPropagation();
    dispatch(
      openBroadcastModal({
        locationId: location.id,
        hazard: 'FLOOD',
        severity: severity,
        provenance: provenance,
      })
    );
  };

  const handleQuickWhatsApp = (e: React.MouseEvent) => {
    e.stopPropagation();
    const alertMsg = `⚠️ KESTREL FLOOD ADVISORY: ${location.name} (${location.riverBasin}). Status: ${severity}. Monitored via GloFAS ensemble. [PROVENANCE: ${provenance}]`;
    const shareUrl = `https://wa.me/?text=${encodeURIComponent(alertMsg)}`;
    window.open(shareUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <div
      onClick={handleSelect}
      className={`group relative rounded-2xl p-5 border transition-all duration-200 cursor-pointer ${
        isSelected
          ? 'bg-gradient-to-br from-slate-900 via-blue-950/30 to-slate-900 border-blue-500/70 shadow-lg shadow-blue-500/10 ring-1 ring-blue-500/30'
          : 'bg-[#101726]/85 hover:bg-[#131d33] border-slate-800/80 hover:border-slate-700 shadow-md'
      }`}
    >
      {/* Top Header Row */}
      <div className="flex items-start justify-between gap-3 mb-3.5">
        <div className="space-y-1 min-w-0">
          <div className="flex items-center space-x-1.5 text-xs text-blue-400 font-medium">
            <Waves className="w-3.5 h-3.5 shrink-0" />
            <span className="truncate">{location.riverBasin}</span>
          </div>
          <h4 className="text-sm sm:text-base font-bold text-slate-100 truncate group-hover:text-blue-300 transition-colors">
            {location.name}
          </h4>
        </div>

        {/* Selected Pin Indicator */}
        {isSelected && (
          <span className="shrink-0 flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-500/20 text-blue-300 border border-blue-500/30">
            <CheckCircle2 className="w-3 h-3" />
            <span>Active</span>
          </span>
        )}
      </div>

      {/* Badges Row */}
      <div className="flex flex-wrap items-center gap-2 mb-4">
        <SeverityBadge severity={severity} size="sm" language={language} />
        <ProvenanceBadge provenance={provenance} size="sm" language={language} />
      </div>

      {/* Discharge Thresholds & Stats */}
      <div className="grid grid-cols-3 gap-2 p-2.5 rounded-xl bg-slate-950/60 border border-slate-800/60 text-center mb-4">
        <div className="space-y-0.5">
          <div className="text-[10px] text-amber-400 font-semibold">T2 (Watch)</div>
          <div className="text-xs font-bold text-slate-200">{location.threshold2yr} m³/s</div>
        </div>
        <div className="space-y-0.5 border-x border-slate-800">
          <div className="text-[10px] text-orange-400 font-semibold">T5 (Warn)</div>
          <div className="text-xs font-bold text-slate-200">{location.threshold5yr} m³/s</div>
        </div>
        <div className="space-y-0.5">
          <div className="text-[10px] text-rose-400 font-semibold">T20 (Severe)</div>
          <div className="text-xs font-bold text-slate-200">{location.threshold20yr} m³/s</div>
        </div>
      </div>

      {/* Footer Metrics & Actions */}
      <div className="flex items-center justify-between pt-2 border-t border-slate-800/80 text-xs">
        {/* Left: Freshness & Reports */}
        <div className="space-y-1">
          <DataAge seconds={status?.dataAgeSeconds ?? 120} isStale={status?.isStale} language={language} />
          {reportCount > 0 ? (
            <div className="flex items-center space-x-1 text-[11px] font-medium text-purple-300">
              <span className="w-1.5 h-1.5 rounded-full bg-purple-400 animate-ping" />
              <span>{reportCount} Ground Report{reportCount > 1 ? 's' : ''} Received</span>
            </div>
          ) : (
            <div className="text-[10px] text-slate-500">Zero ground reports</div>
          )}
        </div>

        {/* Right: Quick Action Buttons */}
        <div className="flex items-center space-x-1.5">
          <button
            onClick={handleQuickWhatsApp}
            title="Share current status to WhatsApp"
            className="p-1.5 rounded-lg bg-[#25D366]/10 text-[#25D366] border border-[#25D366]/20 hover:bg-[#25D366]/20 transition-colors"
          >
            <Share2 className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={handleOpenBroadcast}
            className="inline-flex items-center space-x-1.5 px-2.5 py-1.5 rounded-lg font-bold text-[11px] bg-gradient-to-r from-orange-600 to-red-600 hover:from-orange-500 hover:to-red-500 text-white shadow-sm transition-all cursor-pointer"
          >
            <Radio className="w-3 h-3" />
            <span>Broadcast Alert</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default LocationCard;

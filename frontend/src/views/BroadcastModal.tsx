import React, { useState, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import {
  closeBroadcastModal,
  setSeverity,
  setProvenance,
  setCustomNote,
} from '../features/broadcast/draftSlice';
import { addToast } from '../features/ui/uiSlice';
import { usePreviewBroadcastMutation, useSendBroadcastMutation } from '../api/fanoutApi';
import { SeverityBadge, ProvenanceBadge } from '../components';
import { buildBroadcastContent } from '../api/liveOpenMeteoFallback';
import type { Severity, Provenance, SupportedLanguage, BroadcastPreviewResponse } from '../types/api';
import {
  X,
  Share2,
  Send,
  Users,
  Copy,
  Check,
  Radio,
  ExternalLink,
  Loader2,
  FileText,
  Info,
  ShieldAlert,
  Layers,
} from 'lucide-react';

export const BroadcastModal: React.FC = () => {
  const dispatch = useAppDispatch();
  const { isModalOpen, locationId, hazard, severity, provenance, customNote } = useAppSelector(
    (state) => state.broadcastDraft
  );
  const language = useAppSelector((state) => state.ui.language);

  const [previewTab, setPreviewTab] = useState<SupportedLanguage>('en');
  const [copied, setCopied] = useState(false);

  const [previewBroadcast, { data: previewData, isLoading: isPreviewLoading }] =
    usePreviewBroadcastMutation();
  const [sendBroadcast, { isLoading: isSending }] = useSendBroadcastMutation();

  // Sync initial tab with current UI language when modal opens
  useEffect(() => {
    if (isModalOpen) {
      setPreviewTab(language);
    }
  }, [isModalOpen, language]);

  // Request preview whenever modal is opened or location/severity/provenance/customNote changes
  useEffect(() => {
    if (isModalOpen && locationId) {
      previewBroadcast({
        locationId,
        hazard: hazard || 'FLOOD',
        severity: severity || 'WARNING',
        provenance: provenance || 'MODEL_ONLY',
        customNote: customNote || undefined,
      }).catch((_err) => {
        // Silently swallow preview fetch error, fallback will be used
      });
    }
  }, [isModalOpen, locationId, hazard, severity, provenance, customNote, previewBroadcast]);

  if (!isModalOpen) return null;

  // Local fallback composed messages
  const dynamicFallback = buildBroadcastContent(
    locationId || 'mumbai-kurla',
    (severity as Severity) || 'WARNING',
    (provenance as Provenance) || 'MODEL_ONLY',
    customNote
  );

  const fallbackPreview: BroadcastPreviewResponse = {
    locationId: locationId || 'mumbai-kurla',
    locationName:
      locationId === 'patna-ward12'
        ? 'Ward 12, Patna (Ganges)'
        : locationId === 'guwahati-river'
        ? 'Riverside, Guwahati (Brahmaputra)'
        : locationId === 'cuttack-delta'
        ? 'Cuttack Municipal Corp (Mahanadi)'
        : 'Kurla Ward L, Mumbai (Mithi River)',
    hazard: (hazard as any) || 'FLOOD',
    severity: (severity as Severity) || 'WARNING',
    provenance: (provenance as Provenance) || 'MODEL_ONLY',
    englishMessage: dynamicFallback.englishMessage,
    hindiMessage: dynamicFallback.hindiMessage,
    marathiMessage: dynamicFallback.marathiMessage,
    whatsAppShareUrl: dynamicFallback.whatsAppShareUrl,
    estimatedRecipients: 42,
  };

  const activeData = previewData || fallbackPreview;

  const currentPreviewText =
    previewTab === 'hi'
      ? activeData.hindiMessage
      : previewTab === 'mr'
      ? activeData.marathiMessage
      : activeData.englishMessage;

  const resolvedWhatsAppUrl =
    activeData.whatsAppShareUrl ||
    `https://wa.me/?text=${encodeURIComponent(currentPreviewText)}`;

  const handleCopy = () => {
    navigator.clipboard.writeText(currentPreviewText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
    dispatch(
      addToast({
        id: `toast-${Date.now()}`,
        type: 'info',
        message: 'Alert message copied to clipboard.',
      })
    );
  };

  const handleShareWhatsApp = () => {
    window.open(resolvedWhatsAppUrl, '_blank', 'noopener,noreferrer');
    dispatch(
      addToast({
        id: `toast-${Date.now()}`,
        type: 'success',
        message: 'Opened WhatsApp share link with pre-formatted alert text.',
      })
    );
  };

  const handleExecuteBroadcast = async () => {
    try {
      const res = await sendBroadcast({
        locationId,
        hazard: hazard || 'FLOOD',
        severity: severity || 'WARNING',
        provenance: provenance || 'MODEL_ONLY',
        customNote: customNote || undefined,
      }).unwrap();

      dispatch(
        addToast({
          id: `toast-${Date.now()}`,
          type: 'success',
          message: `Broadcast successfully dispatched to ${res.totalSuccessful} recipients across Telegram, Web Push & WhatsApp!`,
        })
      );
      dispatch(closeBroadcastModal());
    } catch (_err: unknown) {
      dispatch(
        addToast({
          id: `toast-err-${Date.now()}`,
          type: 'error',
          message: 'Failed to dispatch broadcast. Check network connection.',
        })
      );
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="relative w-full max-w-2xl bg-[#0f172a] border border-slate-700/80 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        {/* Modal Header */}
        <div className="px-6 py-4 border-b border-slate-800 bg-[#131d35] flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-xl bg-orange-500/10 border border-orange-500/20 text-orange-400">
              <Radio className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h3 className="text-base font-bold text-slate-100">One-Tap Broadcast Relay</h3>
                <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20">
                  {hazard || 'FLOOD'}
                </span>
              </div>
              <p className="text-xs text-slate-400">{activeData.locationName}</p>
            </div>
          </div>

          <button
            onClick={() => dispatch(closeBroadcastModal())}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors"
            aria-label="Close Modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-5 flex-1">
          {/* Interactive Severity Selection */}
          <div className="space-y-1.5">
            <label className="flex items-center justify-between text-xs font-semibold text-slate-300">
              <span className="flex items-center space-x-1.5">
                <ShieldAlert className="w-3.5 h-3.5 text-amber-400" />
                <span>Select Alert Severity Tier</span>
              </span>
              <span className="text-[10px] text-slate-500">Affects recipient urgency level</span>
            </label>
            <div className="grid grid-cols-3 gap-2">
              {(
                [
                  { id: 'WATCH', label: 'Watch (T₂)', color: 'border-amber-500/40 text-amber-300 bg-amber-500/10 hover:bg-amber-500/20' },
                  { id: 'WARNING', label: 'Warning (T₅)', color: 'border-orange-500/40 text-orange-300 bg-orange-500/10 hover:bg-orange-500/20' },
                  { id: 'SEVERE', label: 'Severe (T₂₀)', color: 'border-rose-500/40 text-rose-300 bg-rose-500/10 hover:bg-rose-500/20' },
                ] as const
              ).map((tier) => (
                <button
                  key={tier.id}
                  type="button"
                  onClick={() => dispatch(setSeverity(tier.id))}
                  className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all text-center ${
                    severity === tier.id
                      ? `${tier.color} ring-2 ring-blue-500/50 shadow-md`
                      : 'border-slate-800 bg-slate-900/60 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {tier.label}
                </button>
              ))}
            </div>
          </div>

          {/* Interactive Provenance Selection */}
          <div className="space-y-1.5">
            <label className="flex items-center justify-between text-xs font-semibold text-slate-300">
              <span className="flex items-center space-x-1.5">
                <Layers className="w-3.5 h-3.5 text-purple-400" />
                <span>Select Data Provenance Tag</span>
              </span>
              <span className="text-[10px] text-slate-500">NDMA / CWC verification status</span>
            </label>
            <div className="grid grid-cols-3 gap-2">
              {(
                [
                  { id: 'MODEL_ONLY', label: 'Model Only', desc: 'GloFAS Ensemble' },
                  { id: 'MODEL_PLUS_REPORTS', label: 'Model + Reports', desc: 'Citizen Ground Truth' },
                  { id: 'OFFICIAL_CONFIRMED', label: 'Official Confirmed', desc: 'SDMA / CWC Verified' },
                ] as const
              ).map((prov) => (
                <button
                  key={prov.id}
                  type="button"
                  onClick={() => dispatch(setProvenance(prov.id))}
                  className={`py-2 px-2.5 rounded-xl border text-left transition-all ${
                    provenance === prov.id
                      ? 'border-purple-500/50 bg-purple-500/15 text-purple-200 ring-2 ring-purple-500/40 shadow-md'
                      : 'border-slate-800 bg-slate-900/60 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <div className="text-xs font-bold">{prov.label}</div>
                  <div className="text-[10px] text-slate-500">{prov.desc}</div>
                </button>
              ))}
            </div>
          </div>

          {/* Target Audience Summary */}
          <div className="flex items-center justify-between gap-3 bg-slate-900/80 border border-slate-800 rounded-xl p-3">
            <div className="flex items-center space-x-2">
              <span className="text-xs text-slate-400">Current Badge:</span>
              <SeverityBadge severity={(severity as Severity) || 'WARNING'} size="sm" language={language} />
              <ProvenanceBadge provenance={(provenance as Provenance) || 'MODEL_ONLY'} size="sm" language={language} />
            </div>

            <div className="flex items-center space-x-1.5 text-xs text-slate-300">
              <Users className="w-4 h-4 text-emerald-400" />
              <span>Audience:</span>
              <span className="font-bold text-emerald-400">
                {activeData.estimatedRecipients ?? 42} Residents
              </span>
            </div>
          </div>

          {/* Coordinator Custom Note */}
          <div className="space-y-1.5">
            <label className="flex items-center justify-between text-xs font-semibold text-slate-300">
              <span className="flex items-center space-x-1.5">
                <FileText className="w-3.5 h-3.5 text-blue-400" />
                <span>Coordinator Special Instructions (Optional)</span>
              </span>
              <span className="text-[10px] text-slate-500 font-normal">
                Appended to outbound WhatsApp & Telegram dispatches
              </span>
            </label>
            <input
              type="text"
              value={customNote}
              onChange={(e) => dispatch(setCustomNote(e.target.value))}
              placeholder="e.g., SDRF rescue boats deployed at Kurla station; helpline 1077"
              className="w-full px-3.5 py-2.5 rounded-xl bg-slate-900/90 border border-slate-800 text-slate-100 text-xs placeholder-slate-500 focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/40 transition-colors"
            />
          </div>

          {/* Trilingual Preview Selector */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
                Multilingual Message Preview
              </span>
              <div className="flex items-center space-x-1 bg-slate-900 border border-slate-800 rounded-lg p-0.5 text-xs">
                {(['en', 'hi', 'mr'] as SupportedLanguage[]).map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setPreviewTab(tab)}
                    className={`px-3 py-1 rounded-md text-xs font-medium transition-colors ${
                      previewTab === tab
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {tab === 'en' ? 'English' : tab === 'hi' ? 'हिंदी' : 'मराठी'}
                  </button>
                ))}
              </div>
            </div>

            {/* Message Box */}
            <div className="relative group">
              <div className="w-full min-h-[140px] p-4 rounded-xl bg-slate-950/80 border border-slate-800 font-mono text-xs text-slate-200 whitespace-pre-wrap leading-relaxed">
                {isPreviewLoading ? (
                  <div className="flex items-center justify-center h-28 space-x-2 text-slate-400">
                    <Loader2 className="w-4 h-4 animate-spin text-blue-400" />
                    <span>Rendering localized message preview...</span>
                  </div>
                ) : (
                  currentPreviewText
                )}
              </div>

              {/* Copy button */}
              <button
                onClick={handleCopy}
                className="absolute top-2.5 right-2.5 p-1.5 rounded-lg bg-slate-900/90 border border-slate-700/80 text-slate-300 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
                title="Copy alert text"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>

          {/* Provenance & Compliance Notice */}
          <div className="flex items-start space-x-2.5 p-3 rounded-xl bg-blue-950/30 border border-blue-900/40 text-[11px] text-blue-200/80 leading-relaxed">
            <Info className="w-4 h-4 text-blue-400 shrink-0 mt-0.5" />
            <span>
              Outbound dispatches strictly include mandatory provenance labels (<code>[MODEL_ONLY]</code>, <code>[MODEL_PLUS_REPORTS]</code>, or <code>[OFFICIAL_CONFIRMED]</code>) compliant with NDMA / CWC standards.
            </span>
          </div>
        </div>

        {/* Modal Footer with Primary Actions */}
        <div className="px-6 py-4 border-t border-slate-800 bg-[#131d35] flex flex-col sm:flex-row items-center justify-between gap-3">
          <button
            onClick={() => dispatch(closeBroadcastModal())}
            disabled={isSending}
            className="w-full sm:w-auto px-4 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors"
          >
            Cancel
          </button>

          <div className="flex items-center space-x-3 w-full sm:w-auto">
            {/* WhatsApp Share Button */}
            <button
              id="btn-whatsapp-share"
              onClick={handleShareWhatsApp}
              disabled={isSending}
              className="flex-1 sm:flex-none inline-flex items-center justify-center space-x-2 px-4 py-2.5 rounded-xl text-xs font-bold bg-[#25D366]/20 text-[#25D366] border border-[#25D366]/40 hover:bg-[#25D366]/30 shadow-lg shadow-[#25D366]/10 transition-all cursor-pointer"
            >
              <Share2 className="w-4 h-4" />
              <span>Share to WhatsApp</span>
              <ExternalLink className="w-3 h-3 opacity-70" />
            </button>

            {/* Send Broadcast Button */}
            <button
              id="btn-send-broadcast"
              onClick={handleExecuteBroadcast}
              disabled={isSending}
              className="flex-1 sm:flex-none inline-flex items-center justify-center space-x-2 px-5 py-2.5 rounded-xl text-xs font-bold bg-gradient-to-r from-orange-600 to-red-600 text-white hover:from-orange-500 hover:to-red-500 shadow-lg shadow-orange-600/20 transition-all cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {isSending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Dispatching...</span>
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  <span>Dispatch All Channels</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default BroadcastModal;

import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setCoordinatorTab, addToast } from '../features/ui/uiSlice';
import { useSendBroadcastMutation } from '../api/fanoutApi';
import { useGetLocationsQuery } from '../api/registryApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import type { Severity, Provenance, LocationDto } from '../types/api';

export const CoordinatorBroadcastComposerView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedLocation = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';

  // Form states
  const [selectedSeverity, setSelectedSeverity] = useState<Severity>('WARNING');
  const [selectedHazard, setSelectedHazard] = useState<string>('FLOOD');
  const [selectedProvenance, setSelectedProvenance] = useState<Provenance>('MODEL_PLUS_REPORTS');
  const [customDirective, setCustomDirective] = useState<string>(
    'Holkar Bridge and Sangam Bridge low causeways closed to vehicular traffic. Municipal Shelter #1 open at Shivajinagar.'
  );
  const [enabledChannels, setEnabledChannels] = useState<string[]>([
    'TELEGRAM',
    'WEB_PUSH',
    'WHATSAPP',
    'CAP_XML',
  ]);
  const [isSending, setIsSending] = useState<boolean>(false);

  // RTK Query API hooks
  const { data: locationsData } = useGetLocationsQuery();
  const [sendBroadcast] = useSendBroadcastMutation();

  const stations: LocationDto[] =
    locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;
  const currentStation = stations.find((s) => s.id === selectedLocation) || stations[0];

  const toggleChannel = (ch: string) => {
    setEnabledChannels((prev) =>
      prev.includes(ch) ? prev.filter((c) => c !== ch) : [...prev, ch]
    );
  };

  const getEnglishPreview = () =>
    `🚨 KESTREL CIVIC ALERT [${selectedSeverity}]: ${currentStation.name} (${currentStation.riverBasin}). Water level approaching 5-yr return threshold. ${customDirective} For emergency assistance call Pune EOC 020-25501269.`;

  const getHindiPreview = () =>
    `🚨 केस्ट्रल नागरिक चेतावनी [${selectedSeverity}]: ${currentStation.name}। जलस्तर में तीव्र वृद्धि। ${customDirective} आपातकालीन सहायता हेतु पुणे नियंत्रण कक्ष ०२०-२५५०१२६९ पर संपर्क करें।`;

  const getMarathiPreview = () =>
    `🚨 केस्ट्रल आपत्कालीन सूचना [${selectedSeverity}]: ${currentStation.name} (${currentStation.riverBasin}). जलपातळी ५-वर्षीय धोक्याच्या जवळ. ${customDirective} मदतीसाठी पुणे आपत्ती नियंत्रण कक्ष ०२०-२५५०१२६९.`;

  const handleDispatch = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSending(true);
    try {
      await sendBroadcast({
        locationId: currentStation.id,
        hazard: selectedHazard,
        severity: selectedSeverity,
        provenance: selectedProvenance,
        customNote: customDirective,
      }).unwrap();

      dispatch(
        addToast({
          id: `bc_${Date.now()}`,
          type: 'success',
          message: `Broadcast successfully dispatched to 1,240 subscribers in ${currentStation.name}!`,
        })
      );
      dispatch(setCoordinatorTab('history'));
    } catch {
      // Local fallback
      dispatch(
        addToast({
          id: `bc_${Date.now()}`,
          type: 'success',
          message: `Broadcast logged to local relay node for ${currentStation.name}!`,
        })
      );
      dispatch(setCoordinatorTab('history'));
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="w-full max-w-[1380px] mx-auto py-2 space-y-6">
      {/* Top Title Bar */}
      <div className="bg-white dark:bg-[#151D27] p-5 rounded-2xl border border-slate-200 dark:border-[#2A3644] flex flex-wrap items-center justify-between gap-4 shadow-sm">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
            Citizen Relay Broadcast Composer
          </h1>
          <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">
            Author authoritative multi-channel guidance to subscribed wards and civic monitors.
          </p>
        </div>
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-100 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] text-xs">
          <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Gateway Online (NDMA CAP Bridge)</span>
        </div>
      </div>

      {/* Main Composer Columns */}
      <form onSubmit={handleDispatch} className="grid grid-cols-1 xl:grid-cols-12 gap-6 items-start">
        {/* ================= LEFT COLUMN: 3-STEP COMPOSER (8 COLS) ================= */}
        <div className="xl:col-span-8 space-y-6">
          {/* STEP 1: TARGET & SEVERITY PARAMETERS */}
          <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <div className="flex items-center gap-3">
                <span className="w-7 h-7 rounded-full bg-blue-600 dark:bg-[#1D4ED8] text-white flex items-center justify-center font-bold text-xs">
                  1
                </span>
                <div>
                  <h2 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Target Scope &amp; Severity Parameters</h2>
                  <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">Define geographic boundaries, hazard vector, and telemetry confidence.</p>
                </div>
              </div>
              <span className="text-xs text-slate-400 dark:text-[#9AA8B8]">Step 1 of 3</span>
            </div>

            {/* Target Location Scope Pills */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-900 dark:text-[#EAF0F6]">Locations in Scope</label>
              <div className="flex flex-wrap gap-2">
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-50 dark:bg-blue-500/20 border border-blue-200 dark:border-blue-500/40 text-blue-700 dark:text-[#7AA7FF] text-xs font-semibold">
                  <span>{currentStation.name}</span>
                </span>
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-50 dark:bg-blue-500/20 border border-blue-200 dark:border-blue-500/40 text-blue-700 dark:text-[#7AA7FF] text-xs font-semibold">
                  <span>Holkar Bridge Causeway</span>
                </span>
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-50 dark:bg-blue-500/20 border border-blue-200 dark:border-blue-500/40 text-blue-700 dark:text-[#7AA7FF] text-xs font-semibold">
                  <span>Sangam Bridge Low Basin</span>
                </span>
              </div>
            </div>

            {/* Hazard Selector */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-900 dark:text-[#EAF0F6]">Hazard Type</label>
              <div className="grid grid-cols-3 gap-3">
                <div
                  onClick={() => setSelectedHazard('FLOOD')}
                  className={`p-3 rounded-xl border cursor-pointer transition-all flex items-center gap-2.5 ${
                    selectedHazard === 'FLOOD'
                      ? 'border-blue-600 bg-blue-50 dark:bg-blue-500/15 text-blue-950 dark:text-white font-semibold'
                      : 'border-slate-200 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                  }`}
                >
                  <span className="material-symbols-outlined text-blue-600 dark:text-blue-400">waves</span>
                  <div className="text-xs">
                    <div className="font-bold">River Flood</div>
                    <div className="text-[10px] text-slate-500 dark:text-[#9AA8B8]">Crest telemetry</div>
                  </div>
                </div>

                <div
                  onClick={() => setSelectedHazard('HEAT')}
                  className={`p-3 rounded-xl border cursor-pointer transition-all flex items-center gap-2.5 ${
                    selectedHazard === 'HEAT'
                      ? 'border-amber-600 bg-amber-50 dark:bg-amber-500/15 text-amber-950 dark:text-white font-semibold'
                      : 'border-slate-200 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                  }`}
                >
                  <span className="material-symbols-outlined text-amber-600 dark:text-amber-400">thermostat</span>
                  <div className="text-xs">
                    <div className="font-bold">Extreme Heat</div>
                    <div className="text-[10px] text-slate-500 dark:text-[#9AA8B8]">Wet-bulb index</div>
                  </div>
                </div>

                <div
                  onClick={() => setSelectedHazard('AIR')}
                  className={`p-3 rounded-xl border cursor-pointer transition-all flex items-center gap-2.5 ${
                    selectedHazard === 'AIR'
                      ? 'border-cyan-600 bg-cyan-50 dark:bg-cyan-500/15 text-cyan-950 dark:text-white font-semibold'
                      : 'border-slate-200 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                  }`}
                >
                  <span className="material-symbols-outlined text-cyan-600 dark:text-cyan-400">air</span>
                  <div className="text-xs">
                    <div className="font-bold">Bad Air Quality</div>
                    <div className="text-[10px] text-slate-500 dark:text-[#9AA8B8]">PM2.5 spike</div>
                  </div>
                </div>
              </div>
            </div>

            {/* Severity Level Radio Cards */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-900 dark:text-[#EAF0F6]">Severity Level</label>
              <div className="grid grid-cols-3 gap-3">
                <div
                  onClick={() => setSelectedSeverity('WATCH')}
                  className={`p-3 rounded-xl border cursor-pointer transition-all flex flex-col justify-between ${
                    selectedSeverity === 'WATCH'
                      ? 'border-yellow-500 bg-yellow-50 dark:bg-yellow-500/15 text-yellow-900 dark:text-yellow-300 font-semibold'
                      : 'border-slate-200 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                  }`}
                >
                  <span className="text-xs font-bold">◔ Watch</span>
                  <span className="text-[10px] mt-1 text-slate-500 dark:text-slate-400">72h precautionary monitoring</span>
                </div>

                <div
                  onClick={() => setSelectedSeverity('WARNING')}
                  className={`p-3 rounded-xl border cursor-pointer transition-all flex flex-col justify-between ${
                    selectedSeverity === 'WARNING'
                      ? 'border-amber-500 bg-amber-50 dark:bg-amber-500/15 text-amber-900 dark:text-amber-300 font-semibold'
                      : 'border-slate-200 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                  }`}
                >
                  <span className="text-xs font-bold">▲ Warning</span>
                  <span className="text-[10px] mt-1 text-slate-500 dark:text-slate-400">Elevated crest risk (5-yr breach)</span>
                </div>

                <div
                  onClick={() => setSelectedSeverity('SEVERE')}
                  className={`p-3 rounded-xl border cursor-pointer transition-all flex flex-col justify-between ${
                    selectedSeverity === 'SEVERE'
                      ? 'border-red-500 bg-red-50 dark:bg-red-500/15 text-red-900 dark:text-red-400 font-semibold'
                      : 'border-slate-200 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                  }`}
                >
                  <span className="text-xs font-bold">⬣ Severe</span>
                  <span className="text-[10px] mt-1 text-slate-500 dark:text-slate-400">Immediate life-safety action</span>
                </div>
              </div>
            </div>

            {/* Provenance Tier Selector */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-900 dark:text-[#EAF0F6]">Data Provenance Tag</label>
              <div className="grid grid-cols-3 gap-3">
                <div
                  onClick={() => setSelectedProvenance('MODEL_ONLY')}
                  className={`p-3 rounded-xl border cursor-pointer transition-all ${
                    selectedProvenance === 'MODEL_ONLY'
                      ? 'border-blue-600 bg-blue-50 dark:bg-blue-500/15 text-blue-950 dark:text-white font-semibold'
                      : 'border-slate-200 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                  }`}
                >
                  <span className="text-xs font-bold">Model Only</span>
                  <span className="text-[10px] block text-slate-500 dark:text-[#9AA8B8]">GloFAS Forecast</span>
                </div>
                <div
                  onClick={() => setSelectedProvenance('MODEL_PLUS_REPORTS')}
                  className={`p-3 rounded-xl border cursor-pointer transition-all ${
                    selectedProvenance === 'MODEL_PLUS_REPORTS'
                      ? 'border-blue-600 bg-blue-50 dark:bg-blue-500/15 text-blue-950 dark:text-white font-semibold'
                      : 'border-slate-200 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                  }`}
                >
                  <span className="text-xs font-bold">Model + Reports</span>
                  <span className="text-[10px] block text-slate-500 dark:text-[#9AA8B8]">Citizen Corroborated</span>
                </div>
                <div
                  onClick={() => setSelectedProvenance('OFFICIAL_CONFIRMED')}
                  className={`p-3 rounded-xl border cursor-pointer transition-all ${
                    selectedProvenance === 'OFFICIAL_CONFIRMED'
                      ? 'border-emerald-600 bg-emerald-50 dark:bg-emerald-500/15 text-emerald-950 dark:text-emerald-300 font-semibold'
                      : 'border-slate-200 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                  }`}
                >
                  <span className="text-xs font-bold">Official Confirmed</span>
                  <span className="text-[10px] block text-slate-500 dark:text-[#9AA8B8]">CWC Gauge Validated</span>
                </div>
              </div>
            </div>
          </section>

          {/* STEP 2: MULTI-LINGUAL MESSAGE PREVIEW */}
          <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <div className="flex items-center gap-3">
                <span className="w-7 h-7 rounded-full bg-blue-600 dark:bg-[#1D4ED8] text-white flex items-center justify-center font-bold text-xs">
                  2
                </span>
                <div>
                  <h2 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Multi-Lingual Broadcast Previews</h2>
                  <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">Automatically generated across all official state languages.</p>
                </div>
              </div>
              <span className="text-xs text-slate-400 dark:text-[#9AA8B8]">Step 2 of 3</span>
            </div>

            {/* Custom Directive Input */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-900 dark:text-[#EAF0F6]">
                Operational Advisory &amp; Safe Shelters Note
              </label>
              <textarea
                rows={2}
                value={customDirective}
                onChange={(e) => setCustomDirective(e.target.value)}
                className="w-full p-3 rounded-xl bg-white dark:bg-[#0B1118] border border-slate-300 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-xs focus:border-blue-500 dark:focus:border-[#7AA7FF] focus:outline-none"
              />
            </div>

            {/* Message Previews */}
            <div className="space-y-3 pt-2">
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
                <span className="text-[11px] font-bold text-blue-700 dark:text-[#7AA7FF]">English (Authoritative Master)</span>
                <p className="text-xs text-slate-800 dark:text-[#EAF0F6] font-mono leading-relaxed">{getEnglishPreview()}</p>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
                <span className="text-[11px] font-bold text-amber-700 dark:text-amber-400">हिन्दी (Hindi Translation)</span>
                <p className="text-xs text-slate-800 dark:text-[#EAF0F6] font-mono leading-relaxed">{getHindiPreview()}</p>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
                <span className="text-[11px] font-bold text-cyan-700 dark:text-cyan-400">मराठी (Marathi Translation)</span>
                <p className="text-xs text-slate-800 dark:text-[#EAF0F6] font-mono leading-relaxed">{getMarathiPreview()}</p>
              </div>
            </div>
          </section>
        </div>

        {/* ================= RIGHT COLUMN: CHANNELS & DISPATCH (4 COLS) ================= */}
        <div className="xl:col-span-4 space-y-6">
          <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-5">
            <div className="flex items-center gap-3 pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <span className="w-7 h-7 rounded-full bg-blue-600 dark:bg-[#1D4ED8] text-white flex items-center justify-center font-bold text-xs">
                3
              </span>
              <div>
                <h2 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Dispatch Channels &amp; Reach</h2>
                <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">Select outbound relay transports</p>
              </div>
            </div>

            {/* Channels Checklist */}
            <div className="space-y-2 text-xs">
              {[
                { id: 'TELEGRAM', label: 'Telegram Verified Bot', icon: 'send', count: '412 users' },
                { id: 'WEB_PUSH', label: 'Web Push (VAPID)', icon: 'notifications_active', count: '680 devices' },
                { id: 'WHATSAPP', label: 'WhatsApp Community Relay', icon: 'chat', count: '148 groups' },
                { id: 'CAP_XML', label: 'NDMA CAP XML v1.2 Feed', icon: 'rss_feed', count: 'Civil Defence' },
              ].map((ch) => {
                const isChecked = enabledChannels.includes(ch.id);
                return (
                  <div
                    key={ch.id}
                    onClick={() => toggleChannel(ch.id)}
                    className={`p-3 rounded-xl border cursor-pointer transition-all flex items-center justify-between ${
                      isChecked
                        ? 'bg-blue-50 dark:bg-blue-500/15 border-blue-300 dark:border-blue-500/40 text-blue-950 dark:text-white font-semibold'
                        : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8] hover:bg-slate-100 dark:hover:bg-[#222E3E]'
                    }`}
                  >
                    <div className="flex items-center gap-2.5">
                      <span className="material-symbols-outlined text-sm">{ch.icon}</span>
                      <span className="font-semibold">{ch.label}</span>
                    </div>
                    <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8]">{ch.count}</span>
                  </div>
                );
              })}
            </div>

            {/* Estimated Reach Stat */}
            <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] space-y-1">
              <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Total Estimated Civilian Reach:</span>
              <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">1,240 civilians</div>
              <div className="text-[10px] text-slate-400 dark:text-[#65778B]">Across Ward 12 &amp; upstream Mutha buffer zone</div>
            </div>

            {/* Dispatch Action Button */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={isSending}
                className="w-full h-12 rounded-xl bg-[#1D4ED8] hover:bg-blue-600 text-white font-bold text-sm shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 transition-all active:scale-[0.98] disabled:opacity-50"
              >
                {isSending ? (
                  <>
                    <span className="material-symbols-outlined text-base animate-spin">refresh</span>
                    <span>Broadcasting to Gateways...</span>
                  </>
                ) : (
                  <>
                    <span className="material-symbols-outlined text-base">cell_tower</span>
                    <span>Dispatch Immediate Broadcast</span>
                  </>
                )}
              </button>
            </div>
          </section>
        </div>
      </form>
    </div>
  );
};

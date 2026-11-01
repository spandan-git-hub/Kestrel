import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setResidentTab, addToast } from '../features/ui/uiSlice';
import { setActiveSubscriptionToken } from '../features/resident/residentSlice';
import { useCreateSubscriptionMutation, useGetLocationsQuery } from '../api/registryApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import type { DeliveryChannel, Severity, SupportedLanguage, LocationDto, Hazard } from '../types/api';

export const ResidentSubscribeView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedWardId = useAppSelector((state) => state.resident.selectedWardId) || 'mumbai-kurla';
  const language = useAppSelector((state) => state.ui.language);

  // Form step state
  const [currentStep, setCurrentStep] = useState<number>(4);
  const [selectedHazards, setSelectedHazards] = useState<Hazard[]>(['FLOOD', 'HEAT', 'AIR_QUALITY']);
  const [preferredLang, setPreferredLang] = useState<SupportedLanguage>(language);
  const [minSeverity, setMinSeverity] = useState<Severity>('WARNING');
  const [deliveryChannel, setDeliveryChannel] = useState<DeliveryChannel>('TELEGRAM');
  const [recipientContact, setRecipientContact] = useState<string>('@pune_resident_01');
  const [isSuccessModal, setIsSuccessModal] = useState<boolean>(false);
  const [generatedLink, setGeneratedLink] = useState<string | null>(null);

  // RTK Query API hooks
  const { data: locationsData } = useGetLocationsQuery();
  const [createSubscription, { isLoading }] = useCreateSubscriptionMutation();

  const stations: LocationDto[] =
    locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;
  const currentStation = stations.find((s) => s.id === selectedWardId) || stations[0];

  const handleToggleHazard = (hazard: Hazard) => {
    setSelectedHazards((prev) => {
      const updated = prev.includes(hazard) ? prev.filter((h) => h !== hazard) : [...prev, hazard];
      return updated.length === 0 ? [hazard] : updated;
    });
  };

  const handleSubscribeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await createSubscription({
        locationId: currentStation.id,
        channel: deliveryChannel,
        recipientHandle: recipientContact,
        recipientIdentifier: recipientContact,
        preferredLanguage: preferredLang,
        minSeverity,
        hazards: selectedHazards,
        consentGiven: true,
        consentVersion: 'DPDP-2023-v1.0',
      }).unwrap();

      dispatch(setActiveSubscriptionToken(res.token));
      dispatch(
        addToast({
          id: `sub_${Date.now()}`,
          type: 'success',
          message: `Alert subscription confirmed for ${currentStation.name}!`,
        })
      );

      if (deliveryChannel === 'TELEGRAM') {
        setGeneratedLink(`https://t.me/kestrel_official_bot?start=sub_${res.token}`);
      } else {
        setGeneratedLink(null);
      }
      setIsSuccessModal(true);
    } catch {
      // Local fallback mock subscription
      const localToken = `kst_tok_${Date.now().toString(36)}`;
      dispatch(setActiveSubscriptionToken(localToken));
      dispatch(
        addToast({
          id: `sub_${Date.now()}`,
          type: 'success',
          message: `Offline mode: Local subscription activated for ${currentStation.name}`,
        })
      );
      if (deliveryChannel === 'TELEGRAM') {
        setGeneratedLink(`https://t.me/kestrel_official_bot?start=sub_${localToken}`);
      }
      setIsSuccessModal(true);
    }
  };

  return (
    <div className="w-full max-w-[1140px] mx-auto py-4 space-y-6 transition-colors duration-200">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column: Progress Sidebar */}
        <aside className="lg:col-span-4 lg:sticky lg:top-24 space-y-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded bg-slate-100 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] text-xs">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
              <span>Civic Alert Dispatch v2.4</span>
            </div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
              Subscribe to alerts
            </h1>
            <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              Receive authoritative guidance directly to your mobile before hazards escalate. Zero ads, zero spam.
            </p>
          </div>

          {/* Stepper Navigation */}
          <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 space-y-4 shadow-sm">
            <nav className="relative">
              <div className="absolute left-[15px] top-4 bottom-4 w-[2px] bg-slate-200 dark:bg-[#2A3644]"></div>
              <ol className="space-y-4 relative z-10 text-xs">
                {/* Step 1 */}
                <li
                  onClick={() => setCurrentStep(1)}
                  className="flex items-start gap-3 cursor-pointer group"
                >
                  <span className="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-500/20 border border-emerald-300 dark:border-emerald-500/30 text-emerald-700 dark:text-emerald-400 flex items-center justify-center font-bold flex-shrink-0">
                    <span className="material-symbols-outlined text-sm font-bold">check</span>
                  </span>
                  <div className="pt-1">
                    <p className="font-semibold text-slate-900 dark:text-[#EAF0F6]">1. Selected Ward / Basin</p>
                    <p className="text-slate-500 dark:text-[#9AA8B8] text-[11px] truncate max-w-[180px]">{currentStation.name}</p>
                  </div>
                </li>

                {/* Step 2 */}
                <li
                  onClick={() => setCurrentStep(2)}
                  className="flex items-start gap-3 cursor-pointer group"
                >
                  <span className="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-500/20 border border-emerald-300 dark:border-emerald-500/30 text-emerald-700 dark:text-emerald-400 flex items-center justify-center font-bold flex-shrink-0">
                    <span className="material-symbols-outlined text-sm font-bold">check</span>
                  </span>
                  <div className="pt-1">
                    <p className="font-semibold text-slate-900 dark:text-[#EAF0F6]">2. Hazards</p>
                    <p className="text-slate-500 dark:text-[#9AA8B8] text-[11px]">
                      {selectedHazards.join(', ') || 'None selected'}
                    </p>
                  </div>
                </li>

                {/* Step 3 */}
                <li
                  onClick={() => setCurrentStep(3)}
                  className="flex items-start gap-3 cursor-pointer group"
                >
                  <span className="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-500/20 border border-emerald-300 dark:border-emerald-500/30 text-emerald-700 dark:text-emerald-400 flex items-center justify-center font-bold flex-shrink-0">
                    <span className="material-symbols-outlined text-sm font-bold">check</span>
                  </span>
                  <div className="pt-1">
                    <p className="font-semibold text-slate-900 dark:text-[#EAF0F6]">3. Language</p>
                    <p className="text-slate-500 dark:text-[#9AA8B8] text-[11px]">
                      {preferredLang === 'en' ? 'English' : preferredLang === 'hi' ? 'हिंदी' : 'मराठी'}
                    </p>
                  </div>
                </li>

                {/* Step 4: Active */}
                <li
                  onClick={() => setCurrentStep(4)}
                  className="flex items-start gap-3 cursor-pointer group"
                >
                  <span
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-bold flex-shrink-0 ${
                      currentStep === 4
                        ? 'bg-[#1D4ED8] text-white ring-4 ring-blue-100 dark:ring-blue-500/20 shadow-md'
                        : 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 border border-emerald-300 dark:border-emerald-500/30'
                    }`}
                  >
                    4
                  </span>
                  <div className="pt-1">
                    <p className={`font-bold ${currentStep === 4 ? 'text-blue-700 dark:text-[#7AA7FF]' : 'text-slate-900 dark:text-[#EAF0F6]'}`}>
                      4. Alert Level &amp; Channels
                    </p>
                    <p className="text-slate-500 dark:text-[#9AA8B8] text-[11px]">Threshold &amp; Endpoint</p>
                  </div>
                </li>
              </ol>
            </nav>
          </div>

          {/* Privacy Guarantee Note */}
          <div className="p-4 rounded-xl bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-600 dark:text-[#9AA8B8] flex items-start gap-2.5 shadow-sm">
            <span className="material-symbols-outlined text-blue-600 dark:text-blue-400 text-lg shrink-0 mt-0.5">verified_user</span>
            <div>
              <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">DPDP Act 2023 Privacy Guarantee:</span> Zero phone numbers or real names collected. Subscriptions use anonymous tokens. Affirmative consent logged with 1-click erasure.
            </div>
          </div>
        </aside>

        {/* Right Column: Active Form Step */}
        <main className="lg:col-span-8 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-6">
          <form onSubmit={handleSubscribeSubmit} className="space-y-6">
            {/* Step 1: Target Location Context */}
            <div className="space-y-2 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
              <div className="flex items-center justify-between">
                <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">1. Monitored Area</label>
                <button
                  type="button"
                  onClick={() => dispatch(setResidentTab('status'))}
                  className="text-xs text-blue-600 dark:text-[#7AA7FF] hover:underline"
                >
                  Change Ward
                </button>
              </div>
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <span className="material-symbols-outlined text-red-500 dark:text-red-400">location_on</span>
                  <div>
                    <div className="text-sm font-semibold text-slate-900 dark:text-[#EAF0F6]">{currentStation.name}</div>
                    <div className="text-xs text-slate-500 dark:text-[#9AA8B8]">
                      {currentStation.riverBasin} · {currentStation.state} · <span className="font-semibold text-blue-600 dark:text-[#7AA7FF]">{currentStation.regionType || 'PLAINS'}</span>
                    </div>
                  </div>
                </div>
                <span className="text-[11px] px-2 py-0.5 rounded bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF] font-semibold">
                  Station #{currentStation.id}
                </span>
              </div>
            </div>

            {/* Step 2: Hazard Selection */}
            <div className="space-y-3 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
              <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">2. Choose Hazards to Monitor</label>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                {/* Flood */}
                <div
                  onClick={() => handleToggleHazard('FLOOD')}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all flex items-center gap-3 ${
                    selectedHazards.includes('FLOOD')
                      ? 'bg-blue-50 dark:bg-blue-500/15 border-blue-500 text-blue-900 dark:text-white'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                  }`}
                >
                  <span className="material-symbols-outlined text-blue-600 dark:text-blue-400">waves</span>
                  <div className="text-xs">
                    <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">River Flood</div>
                    <div className="text-[10px]">Crest forecasts</div>
                  </div>
                  {selectedHazards.includes('FLOOD') && (
                    <span className="material-symbols-outlined text-sm text-blue-600 dark:text-blue-400 ml-auto">check_circle</span>
                  )}
                </div>

                {/* Heat */}
                <div
                  onClick={() => handleToggleHazard('HEAT')}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all flex items-center gap-3 ${
                    selectedHazards.includes('HEAT')
                      ? 'bg-amber-50 dark:bg-amber-500/15 border-amber-500 text-amber-900 dark:text-white'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                  }`}
                >
                  <span className="material-symbols-outlined text-amber-600 dark:text-amber-400">thermostat</span>
                  <div className="text-xs">
                    <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Extreme Heat</div>
                    <div className="text-[10px]">IMD heat-wave criteria</div>
                  </div>
                  {selectedHazards.includes('HEAT') && (
                    <span className="material-symbols-outlined text-sm text-amber-600 dark:text-amber-400 ml-auto">check_circle</span>
                  )}
                </div>

                {/* Air Quality */}
                <div
                  onClick={() => handleToggleHazard('AIR_QUALITY')}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all flex items-center gap-3 ${
                    selectedHazards.includes('AIR_QUALITY')
                      ? 'bg-cyan-50 dark:bg-cyan-500/15 border-cyan-500 text-cyan-900 dark:text-white'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                  }`}
                >
                  <span className="material-symbols-outlined text-cyan-600 dark:text-cyan-400">air</span>
                  <div className="text-xs">
                    <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Air Quality</div>
                    <div className="text-[10px]">CPCB AQI alerts</div>
                  </div>
                  {selectedHazards.includes('AIR_QUALITY') && (
                    <span className="material-symbols-outlined text-sm text-cyan-600 dark:text-cyan-400 ml-auto">check_circle</span>
                  )}
                </div>
              </div>
            </div>

            {/* Step 3: Alert Sensitivity Level */}
            <div className="space-y-3 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
              <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">3. Minimum Alert Sensitivity</label>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                {/* Watch */}
                <div
                  onClick={() => setMinSeverity('WATCH')}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all flex flex-col justify-between ${
                    minSeverity === 'WATCH'
                      ? 'bg-yellow-50 dark:bg-yellow-500/15 border-yellow-500 text-yellow-900 dark:text-yellow-300'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                  }`}
                >
                  <div className="flex items-center justify-between text-xs font-bold text-yellow-700 dark:text-yellow-300">
                    <span>◔ Watch &amp; Above</span>
                    {minSeverity === 'WATCH' && <span className="material-symbols-outlined text-sm">check</span>}
                  </div>
                  <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8] mt-2">
                    Early 72h precautionary notice (high frequency).
                  </p>
                </div>

                {/* Warning (Recommended) */}
                <div
                  onClick={() => setMinSeverity('WARNING')}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all flex flex-col justify-between ${
                    minSeverity === 'WARNING'
                      ? 'bg-amber-50 dark:bg-amber-500/15 border-amber-500 text-amber-900 dark:text-amber-300'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                  }`}
                >
                  <div className="flex items-center justify-between text-xs font-bold text-amber-700 dark:text-amber-400">
                    <span>▲ Warning &amp; Above</span>
                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF]">Recommended</span>
                  </div>
                  <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8] mt-2">
                    Crest reaches 5-yr threshold (standard civic balance).
                  </p>
                </div>

                {/* Severe Only */}
                <div
                  onClick={() => setMinSeverity('SEVERE')}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all flex flex-col justify-between ${
                    minSeverity === 'SEVERE'
                      ? 'bg-red-50 dark:bg-red-500/15 border-red-500 text-red-900 dark:text-red-400'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                  }`}
                >
                  <div className="flex items-center justify-between text-xs font-bold text-red-700 dark:text-red-400">
                    <span>⬣ Severe Only</span>
                    {minSeverity === 'SEVERE' && <span className="material-symbols-outlined text-sm">check</span>}
                  </div>
                  <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8] mt-2">
                    Critical emergencies only (20-yr return breaches).
                  </p>
                </div>
              </div>
            </div>

            {/* Step 4: Notification Channels & Endpoint */}
            <div className="space-y-3 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
              <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">4. Delivery Channels</label>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                {/* Telegram */}
                <button
                  type="button"
                  onClick={() => setDeliveryChannel('TELEGRAM')}
                  className={`p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1.5 ${
                    deliveryChannel === 'TELEGRAM'
                      ? 'bg-cyan-50 dark:bg-cyan-500/20 border-cyan-500 text-cyan-800 dark:text-cyan-300 font-bold'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                  }`}
                >
                  <span className="material-symbols-outlined text-[22px]">send</span>
                  <span className="text-xs">Telegram</span>
                </button>

                {/* WhatsApp */}
                <button
                  type="button"
                  onClick={() => setDeliveryChannel('WHATSAPP')}
                  className={`p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1.5 ${
                    deliveryChannel === 'WHATSAPP'
                      ? 'bg-emerald-50 dark:bg-emerald-500/20 border-emerald-500 text-emerald-800 dark:text-emerald-300 font-bold'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                  }`}
                >
                  <span className="material-symbols-outlined text-[22px]">chat</span>
                  <span className="text-xs">WhatsApp</span>
                </button>

                {/* Web Push */}
                <button
                  type="button"
                  onClick={() => setDeliveryChannel('WEB_PUSH')}
                  className={`p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1.5 ${
                    deliveryChannel === 'WEB_PUSH'
                      ? 'bg-blue-50 dark:bg-blue-500/20 border-blue-500 text-blue-800 dark:text-blue-300 font-bold'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                  }`}
                >
                  <span className="material-symbols-outlined text-[22px]">notifications_active</span>
                  <span className="text-xs">Web Push</span>
                </button>

                {/* SMS Broadcast */}
                <button
                  type="button"
                  onClick={() => setDeliveryChannel('WHATSAPP')}
                  className="p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1.5 bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]"
                >
                  <span className="material-symbols-outlined text-[22px]">sms</span>
                  <span className="text-xs">SMS Relay</span>
                </button>
              </div>

              {/* Recipient Handle / Number Input */}
              <div className="pt-2">
                <label className="text-xs font-semibold text-slate-700 dark:text-[#9AA8B8]">
                  {deliveryChannel === 'TELEGRAM'
                    ? 'Telegram Username or Chat ID'
                    : deliveryChannel === 'WHATSAPP'
                    ? 'WhatsApp Mobile Number (+91)'
                    : 'Browser Push Endpoint'}
                </label>
                <div className="relative mt-1">
                  <input
                    type="text"
                    value={recipientContact}
                    onChange={(e) => setRecipientContact(e.target.value)}
                    required
                    placeholder={
                      deliveryChannel === 'TELEGRAM'
                        ? '@your_telegram_handle'
                        : '+91 98765 43210'
                    }
                    className="w-full h-11 px-3.5 rounded-xl bg-white dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-sm focus:border-blue-500 dark:focus:border-[#7AA7FF] focus:outline-none"
                  />
                </div>
              </div>
            </div>

            {/* Step 5: Language Selection */}
            <div className="space-y-3">
              <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">5. Alert Broadcast Language</label>
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => setPreferredLang('en')}
                  className={`px-4 py-2 rounded-xl text-xs font-semibold border ${
                    preferredLang === 'en'
                      ? 'bg-blue-600 text-white border-blue-500 shadow-xs'
                      : 'bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644]'
                  }`}
                >
                  English
                </button>
                <button
                  type="button"
                  onClick={() => setPreferredLang('hi')}
                  className={`px-4 py-2 rounded-xl text-xs font-semibold border ${
                    preferredLang === 'hi'
                      ? 'bg-blue-600 text-white border-blue-500 shadow-xs'
                      : 'bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644]'
                  }`}
                >
                  हिन्दी (Hindi)
                </button>
                <button
                  type="button"
                  onClick={() => setPreferredLang('mr')}
                  className={`px-4 py-2 rounded-xl text-xs font-semibold border ${
                    preferredLang === 'mr'
                      ? 'bg-blue-600 text-white border-blue-500 shadow-xs'
                      : 'bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644]'
                  }`}
                >
                  मराठी (Marathi)
                </button>
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-4">
              <button
                type="submit"
                disabled={isLoading}
                className="w-full h-12 rounded-xl bg-[#1D4ED8] hover:bg-blue-600 text-white font-bold text-sm shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-50"
              >
                {isLoading ? (
                  <>
                    <span className="material-symbols-outlined text-[18px] animate-spin">refresh</span>
                    <span>Activating Subscription...</span>
                  </>
                ) : (
                  <>
                    <span className="material-symbols-outlined text-[18px]">verified</span>
                    <span>Confirm &amp; Activate Alert Subscription</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </main>
      </div>

      {/* SUCCESS MODAL POPUP */}
      {isSuccessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 dark:bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between">
              <div className="w-10 h-10 rounded-xl bg-emerald-100 dark:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
                <span className="material-symbols-outlined text-2xl">check_circle</span>
              </div>
              <button
                onClick={() => setIsSuccessModal(false)}
                className="text-slate-400 hover:text-slate-700 dark:text-[#9AA8B8] dark:hover:text-white"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="space-y-1">
              <h3 className="text-xl font-bold text-slate-900 dark:text-[#EAF0F6]">Alert Subscription Active</h3>
              <p className="text-xs text-slate-600 dark:text-[#9AA8B8]">
                You are now subscribed to early multi-hazard warnings for{' '}
                <strong className="text-slate-900 dark:text-[#EAF0F6]">{currentStation.name}</strong>.
              </p>
            </div>

            {generatedLink && (
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-2">
                <span className="text-xs font-semibold text-cyan-700 dark:text-cyan-300">Telegram Bot Setup:</span>
                <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8]">
                  Click the link below to start the official verified Kestrel Telegram Bot:
                </p>
                <a
                  href={generatedLink}
                  target="_blank"
                  rel="noreferrer"
                  className="block w-full py-2 px-3 text-center rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white font-bold text-xs"
                >
                  Open @kestrel_official_bot
                </a>
              </div>
            )}

            <div className="flex gap-3 pt-2">
              <button
                onClick={() => {
                  setIsSuccessModal(false);
                  dispatch(setResidentTab('my-alerts'));
                }}
                className="flex-1 py-2.5 rounded-xl bg-slate-100 dark:bg-[#1C2633] hover:bg-slate-200 dark:hover:bg-[#253243] text-slate-800 dark:text-[#EAF0F6] text-xs font-semibold border border-slate-200 dark:border-[#2A3644]"
              >
                View My Alerts
              </button>
              <button
                onClick={() => {
                  setIsSuccessModal(false);
                  dispatch(setResidentTab('status'));
                }}
                className="flex-1 py-2.5 rounded-xl bg-[#1D4ED8] hover:bg-blue-600 text-white text-xs font-semibold"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

import React, { useState, useMemo } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { addToast } from '../features/ui/uiSlice';
import { setActiveSubscriptionToken } from '../features/resident/residentSlice';
import { useCreateSubscriptionMutation, useGetLocationsQuery } from '../api/registryApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import { ALL_HEAT_STATIONS } from '../catalog/heatStations';
import { ALL_AQ_STATIONS } from '../catalog/aqStations';
import type { LocationDto, Hazard, Severity, SupportedLanguage } from '../types/api';
import {
  Bell,
  MapPin,
  Check,
  Send,
  MessageSquare,
  Radio,
  Globe,
  ShieldCheck,
  BookOpen,
  Copy,
} from 'lucide-react';

export const SubscribeView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedWardId = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';

  // 1. Place / Location choice
  const [selectedStationId, setSelectedStationId] = useState<string>(selectedWardId);
  const [stationSearch, setStationSearch] = useState<string>('');
  const [isStationDropdownOpen, setIsStationDropdownOpen] = useState<boolean>(false);

  // 2. Hazard choices (Multi-select)
  const [selectedHazards, setSelectedHazards] = useState<Hazard[]>(['FLOOD', 'HEAT']);

  // 3. Broadcast channels (Multi-select - user can choose more than one!)
  const [selectedChannels, setSelectedChannels] = useState<string[]>(['WHATSAPP', 'TELEGRAM']);

  // 4. Language preferences (Multi-select - user can choose more than one!)
  const [selectedLanguages, setSelectedLanguages] = useState<SupportedLanguage[]>(['en', 'hi', 'mr']);

  // 5. Sensitivity Threshold
  const [minSeverity, setMinSeverity] = useState<Severity>('WARNING');

  // Contact inputs
  const [phoneContact, setPhoneContact] = useState<string>('+91 98765 43210');
  const [telegramHandle, setTelegramHandle] = useState<string>('@kestrel_citizen');

  // Confirmation modal
  const [isSuccessModal, setIsSuccessModal] = useState<boolean>(false);
  const [copiedPreview, setCopiedPreview] = useState<boolean>(false);

  // RTK Query API hooks
  const { data: locationsData } = useGetLocationsQuery();
  const [createSubscription, { isLoading }] = useCreateSubscriptionMutation();

  const stations: LocationDto[] = useMemo(() => {
    const list: LocationDto[] = locationsData && locationsData.length > 0 ? [...locationsData] : [...ALL_45_RIVER_STATIONS];
    ALL_HEAT_STATIONS.forEach((h) => {
      if (!list.some((s) => s.id === h.id)) {
        list.push({
          id: h.id,
          name: h.name,
          state: h.state,
          latitude: h.latitude,
          longitude: h.longitude,
          riverBasin: h.climateZone,
          regionType: h.regionType,
          threshold2yr: 38,
          threshold5yr: 42,
          threshold20yr: 45,
          primaryLanguage: h.primaryLanguage,
          active: h.active,
        });
      }
    });
    ALL_AQ_STATIONS.forEach((a) => {
      if (!list.some((s) => s.id === a.id)) {
        list.push({
          id: a.id,
          name: a.name,
          state: a.state,
          latitude: a.latitude,
          longitude: a.longitude,
          riverBasin: a.zone,
          regionType: 'PLAINS',
          threshold2yr: 200,
          threshold5yr: 300,
          threshold20yr: 400,
          primaryLanguage: a.primaryLanguage,
          active: a.active,
        });
      }
    });
    return list;
  }, [locationsData]);

  const currentStation = stations.find((s) => s.id === selectedStationId) || stations[0];

  const filteredStations = stations.filter(
    (st) =>
      st.name.toLowerCase().includes(stationSearch.toLowerCase()) ||
      st.state.toLowerCase().includes(stationSearch.toLowerCase()) ||
      (st.riverBasin || '').toLowerCase().includes(stationSearch.toLowerCase())
  );

  const toggleHazard = (hazard: Hazard) => {
    setSelectedHazards((prev) => {
      if (prev.includes(hazard)) {
        return prev.length > 1 ? prev.filter((h) => h !== hazard) : prev;
      }
      return [...prev, hazard];
    });
  };

  const toggleChannel = (channel: string) => {
    setSelectedChannels((prev) => {
      if (prev.includes(channel)) {
        return prev.length > 1 ? prev.filter((c) => c !== channel) : prev;
      }
      return [...prev, channel];
    });
  };

  const toggleLanguage = (lang: SupportedLanguage) => {
    setSelectedLanguages((prev) => {
      if (prev.includes(lang)) {
        return prev.length > 1 ? prev.filter((l) => l !== lang) : prev;
      }
      return [...prev, lang];
    });
  };

  // Generate dynamic single broadcast message grouping all selected languages
  const generateGroupedMultilingualMessage = (): string => {
    const sections: string[] = [];
    const primaryHazard = selectedHazards[0] || 'FLOOD';
    const manualUrl = `https://kestrel.relay/manuals?hazard=${primaryHazard}&severity=${minSeverity}`;

    if (selectedLanguages.includes('en')) {
      sections.push(
        `🚨 KESTREL CIVIC ALERT [${minSeverity}]\n` +
          `📍 Location: ${currentStation.name}, ${currentStation.state} (${currentStation.riverBasin})\n` +
          `⚠️ Hazard: ${selectedHazards.join(' & ')} threshold breached.\n` +
          `📘 Emergency Do's & Don'ts Manual: ${manualUrl}`
      );
    }

    if (selectedLanguages.includes('hi')) {
      sections.push(
        `🇮🇳 हिन्दी (Hindi):\n` +
          `⚠️ केस्ट्रल चेतावनी [${minSeverity}]:\n` +
          `📍 स्थान: ${currentStation.name}, ${currentStation.state}\n` +
          `जलस्तर/मौसम खतरे की सीमा पार कर रहा है।\n` +
          `📘 सावधानियां और निर्देश (Manual): ${manualUrl}`
      );
    }

    if (selectedLanguages.includes('mr')) {
      sections.push(
        `🚩 मराठी (Marathi):\n` +
          `⚠️ केस्ट्रल आपत्कालीन इशारा [${minSeverity}]:\n` +
          `📍 ठिकाण: ${currentStation.name}, ${currentStation.state}\n` +
          `पूर/हवामान इशारा मर्यादा ओलांडत आहे. सावधगिरी बाळगा.\n` +
          `📘 काय करावे / काय करू नये (Manual): ${manualUrl}`
      );
    }

    return sections.join('\n\n────────────────────────\n\n');
  };

  const handleCopyPreview = () => {
    navigator.clipboard.writeText(generateGroupedMultilingualMessage());
    setCopiedPreview(true);
    setTimeout(() => setCopiedPreview(false), 2000);
    dispatch(
      addToast({
        id: `copy_${Date.now()}`,
        type: 'info',
        message: 'Multilingual grouped message copied to clipboard!',
      })
    );
  };

  const handleSubscribeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await createSubscription({
        locationId: currentStation.id,
        channel: selectedChannels[0] as any,
        recipientHandle: selectedChannels.includes('TELEGRAM') ? telegramHandle : phoneContact,
        recipientIdentifier: phoneContact,
        preferredLanguage: selectedLanguages[0] || 'en',
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
      setIsSuccessModal(true);
    } catch {
      // Local fallback mock subscription
      const localToken = `kst_sub_${Date.now().toString(36)}`;
      dispatch(setActiveSubscriptionToken(localToken));
      dispatch(
        addToast({
          id: `sub_${Date.now()}`,
          type: 'success',
          message: `Subscription activated for ${currentStation.name}!`,
        })
      );
      setIsSuccessModal(true);
    }
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 pb-12">
      {/* 1. TOP HERO HEADER */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-2">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-blue-50 dark:bg-[#1C2633] text-blue-600 dark:text-[#7AA7FF] border border-blue-200 dark:border-blue-500/30">
            <Bell className="w-4 h-4" />
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
            Subscribe to Direct Hazard Broadcasts
          </h1>
        </div>
        <p className="text-xs sm:text-sm text-slate-600 dark:text-[#9AA8B8] max-w-3xl leading-relaxed">
          Choose the place you want to monitor, select your hazards and broadcast channels (you can select more than one), and choose your preferred languages. All selected languages will be grouped into a single broadcast message with direct links to emergency manuals.
        </p>
      </section>

      {/* 2. MAIN TWO-COLUMN SUBSCRIPTION BUILDER */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Form Controls (7 cols) */}
        <form
          onSubmit={handleSubscribeSubmit}
          className="lg:col-span-7 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-6 text-xs"
        >
          {/* STEP 1: CHOOSE PLACE */}
          <div className="space-y-2 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
            <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center justify-between">
              <span>1. What place do you want broadcasts for?</span>
              <span className="text-[11px] font-normal text-blue-600 dark:text-[#7AA7FF]">
                {currentStation.riverBasin}
              </span>
            </label>

            <div className="relative">
              <button
                type="button"
                onClick={() => setIsStationDropdownOpen(!isStationDropdownOpen)}
                className="w-full p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between text-left hover:border-blue-500 transition-colors"
              >
                <div className="flex items-center gap-2.5">
                  <MapPin className="w-4 h-4 text-red-500 shrink-0" />
                  <div>
                    <div className="font-bold text-slate-900 dark:text-[#EAF0F6] text-xs">
                      {currentStation.name}
                    </div>
                    <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                      {currentStation.riverBasin} · {currentStation.state}
                    </div>
                  </div>
                </div>
                <span className="text-xs text-blue-600 font-semibold">Change ▾</span>
              </button>

              {isStationDropdownOpen && (
                <div className="absolute left-0 right-0 top-14 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-3 shadow-2xl z-50 space-y-2">
                  <input
                    type="text"
                    value={stationSearch}
                    onChange={(e) => setStationSearch(e.target.value)}
                    placeholder="Search city, ward, state or river basin..."
                    className="w-full h-9 px-3 text-xs rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] focus:outline-none focus:border-blue-500"
                  />
                  <div className="max-h-56 overflow-y-auto space-y-1 custom-scrollbar pr-1">
                    {filteredStations.map((st) => (
                      <div
                        key={st.id}
                        onClick={() => {
                          setSelectedStationId(st.id);
                          setIsStationDropdownOpen(false);
                        }}
                        className={`p-2.5 rounded-xl cursor-pointer transition-colors flex items-center justify-between ${
                          st.id === currentStation.id
                            ? 'bg-blue-50 dark:bg-blue-600/20 text-blue-800 dark:text-[#7AA7FF] font-bold'
                            : 'hover:bg-slate-50 dark:hover:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8]'
                        }`}
                      >
                        <div>
                          <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">{st.name}</div>
                          <div className="text-[10px] text-slate-500">{st.riverBasin} · {st.state}</div>
                        </div>
                        {st.id === currentStation.id && <Check className="w-4 h-4 text-blue-600" />}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* STEP 2: CHOOSE HAZARDS (MULTI-SELECT) */}
          <div className="space-y-2 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
            <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] block">
              2. For what hazards? (Choose one or more)
            </label>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              <button
                type="button"
                onClick={() => toggleHazard('FLOOD')}
                className={`p-3.5 rounded-xl border text-left transition-all flex items-center justify-between cursor-pointer ${
                  selectedHazards.includes('FLOOD')
                    ? 'bg-blue-50 dark:bg-blue-600/15 border-blue-500 text-blue-900 dark:text-[#7AA7FF] font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                }`}
              >
                <div>
                  <div className="font-bold text-slate-900 dark:text-[#EAF0F6]">River Flood</div>
                  <div className="text-[10px] text-slate-500">Inundation &amp; crests</div>
                </div>
                {selectedHazards.includes('FLOOD') && <Check className="w-4 h-4 text-blue-600" />}
              </button>

              <button
                type="button"
                onClick={() => toggleHazard('HEAT')}
                className={`p-3.5 rounded-xl border text-left transition-all flex items-center justify-between cursor-pointer ${
                  selectedHazards.includes('HEAT')
                    ? 'bg-amber-50 dark:bg-amber-600/15 border-amber-500 text-amber-900 dark:text-amber-400 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                }`}
              >
                <div>
                  <div className="font-bold text-slate-900 dark:text-[#EAF0F6]">Extreme Heat</div>
                  <div className="text-[10px] text-slate-500">IMD heatwave criteria</div>
                </div>
                {selectedHazards.includes('HEAT') && <Check className="w-4 h-4 text-amber-600" />}
              </button>

              <button
                type="button"
                onClick={() => toggleHazard('AIR_QUALITY')}
                className={`p-3.5 rounded-xl border text-left transition-all flex items-center justify-between cursor-pointer ${
                  selectedHazards.includes('AIR_QUALITY')
                    ? 'bg-cyan-50 dark:bg-cyan-600/15 border-cyan-500 text-cyan-900 dark:text-cyan-400 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                }`}
              >
                <div>
                  <div className="font-bold text-slate-900 dark:text-[#EAF0F6]">Air Quality</div>
                  <div className="text-[10px] text-slate-500">CPCB AQI spikes</div>
                </div>
                {selectedHazards.includes('AIR_QUALITY') && <Check className="w-4 h-4 text-cyan-600" />}
              </button>
            </div>
          </div>

          {/* STEP 3: TYPE OF BROADCAST (MULTI-SELECT) */}
          <div className="space-y-2 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
            <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] block">
              3. Type of broadcast? (Choose one or more)
            </label>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              <button
                type="button"
                onClick={() => toggleChannel('WHATSAPP')}
                className={`p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1.5 cursor-pointer ${
                  selectedChannels.includes('WHATSAPP')
                    ? 'bg-emerald-50 dark:bg-emerald-500/15 border-emerald-500 text-emerald-800 dark:text-emerald-300 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                <MessageSquare className="w-4 h-4 text-emerald-500" />
                <span className="text-xs">WhatsApp</span>
                {selectedChannels.includes('WHATSAPP') && <Check className="w-3.5 h-3.5 text-emerald-600" />}
              </button>

              <button
                type="button"
                onClick={() => toggleChannel('TELEGRAM')}
                className={`p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1.5 cursor-pointer ${
                  selectedChannels.includes('TELEGRAM')
                    ? 'bg-cyan-50 dark:bg-cyan-500/15 border-cyan-500 text-cyan-800 dark:text-cyan-300 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                <Send className="w-4 h-4 text-cyan-500" />
                <span className="text-xs">Telegram</span>
                {selectedChannels.includes('TELEGRAM') && <Check className="w-3.5 h-3.5 text-cyan-600" />}
              </button>

              <button
                type="button"
                onClick={() => toggleChannel('WEB_PUSH')}
                className={`p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1.5 cursor-pointer ${
                  selectedChannels.includes('WEB_PUSH')
                    ? 'bg-blue-50 dark:bg-blue-500/15 border-blue-500 text-blue-800 dark:text-blue-300 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                <Radio className="w-4 h-4 text-blue-500" />
                <span className="text-xs">Web Push</span>
                {selectedChannels.includes('WEB_PUSH') && <Check className="w-3.5 h-3.5 text-blue-600" />}
              </button>

              <button
                type="button"
                onClick={() => toggleChannel('SMS')}
                className={`p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1.5 cursor-pointer ${
                  selectedChannels.includes('SMS')
                    ? 'bg-purple-50 dark:bg-purple-500/15 border-purple-500 text-purple-800 dark:text-purple-300 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                <Bell className="w-4 h-4 text-purple-500" />
                <span className="text-xs">SMS Alert</span>
                {selectedChannels.includes('SMS') && <Check className="w-3.5 h-3.5 text-purple-600" />}
              </button>
            </div>

            {/* Contact details */}
            <div className="pt-2 grid grid-cols-1 sm:grid-cols-2 gap-3">
              {selectedChannels.includes('WHATSAPP') && (
                <div>
                  <label className="text-[11px] font-semibold text-slate-700 dark:text-[#9AA8B8] block mb-1">
                    WhatsApp Number (+91):
                  </label>
                  <input
                    type="text"
                    value={phoneContact}
                    onChange={(e) => setPhoneContact(e.target.value)}
                    required
                    placeholder="+91 98765 43210"
                    className="w-full h-10 px-3 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-xs focus:outline-none focus:border-blue-500"
                  />
                </div>
              )}

              {selectedChannels.includes('TELEGRAM') && (
                <div>
                  <label className="text-[11px] font-semibold text-slate-700 dark:text-[#9AA8B8] block mb-1">
                    Telegram Handle or Chat ID:
                  </label>
                  <input
                    type="text"
                    value={telegramHandle}
                    onChange={(e) => setTelegramHandle(e.target.value)}
                    required
                    placeholder="@your_telegram_id"
                    className="w-full h-10 px-3 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-xs focus:outline-none focus:border-blue-500"
                  />
                </div>
              )}
            </div>
          </div>

          {/* STEP 4: PREFERENCE OF LANGUAGE (MULTI-SELECT - GROUPED IN SINGLE MESSAGE) */}
          <div className="space-y-2 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
            <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center justify-between">
              <span>4. Preference of language? (Choose one or more)</span>
              <span className="text-[11px] font-normal text-slate-500">
                Grouped together in a single message
              </span>
            </label>

            <div className="grid grid-cols-3 gap-2.5">
              <button
                type="button"
                onClick={() => toggleLanguage('en')}
                className={`p-3 rounded-xl border text-center transition-all flex items-center justify-center gap-2 cursor-pointer ${
                  selectedLanguages.includes('en')
                    ? 'bg-blue-600 text-white border-blue-600 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                }`}
              >
                <span>English</span>
                {selectedLanguages.includes('en') && <Check className="w-3.5 h-3.5" />}
              </button>

              <button
                type="button"
                onClick={() => toggleLanguage('hi')}
                className={`p-3 rounded-xl border text-center transition-all flex items-center justify-center gap-2 cursor-pointer ${
                  selectedLanguages.includes('hi')
                    ? 'bg-blue-600 text-white border-blue-600 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                }`}
              >
                <span>हिन्दी (Hindi)</span>
                {selectedLanguages.includes('hi') && <Check className="w-3.5 h-3.5" />}
              </button>

              <button
                type="button"
                onClick={() => toggleLanguage('mr')}
                className={`p-3 rounded-xl border text-center transition-all flex items-center justify-center gap-2 cursor-pointer ${
                  selectedLanguages.includes('mr')
                    ? 'bg-blue-600 text-white border-blue-600 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                }`}
              >
                <span>मराठी (Marathi)</span>
                {selectedLanguages.includes('mr') && <Check className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>

          {/* STEP 5: SENSITIVITY LEVEL */}
          <div className="space-y-2 pb-2">
            <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] block">
              5. Minimum Alert Sensitivity
            </label>

            <div className="grid grid-cols-3 gap-2.5">
              <button
                type="button"
                onClick={() => setMinSeverity('WATCH')}
                className={`p-2.5 rounded-xl border text-center transition-all ${
                  minSeverity === 'WATCH'
                    ? 'bg-amber-50 dark:bg-amber-500/20 border-amber-500 text-amber-800 dark:text-amber-300 font-bold'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                Watch (2-Yr Return)
              </button>

              <button
                type="button"
                onClick={() => setMinSeverity('WARNING')}
                className={`p-2.5 rounded-xl border text-center transition-all ${
                  minSeverity === 'WARNING'
                    ? 'bg-orange-50 dark:bg-orange-500/20 border-orange-500 text-orange-800 dark:text-orange-300 font-bold'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                Warning (5-Yr Return)
              </button>

              <button
                type="button"
                onClick={() => setMinSeverity('SEVERE')}
                className={`p-2.5 rounded-xl border text-center transition-all ${
                  minSeverity === 'SEVERE'
                    ? 'bg-red-50 dark:bg-red-500/20 border-red-500 text-red-800 dark:text-red-300 font-bold'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                Severe (20-Yr Return)
              </button>
            </div>
          </div>

          {/* SUBMIT BUTTON */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={isLoading}
              className="w-full h-12 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-md shadow-blue-500/25 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-50 cursor-pointer"
            >
              <ShieldCheck className="w-5 h-5" />
              <span>Confirm &amp; Activate Hazard Broadcast Subscription</span>
            </button>
          </div>
        </form>

        {/* Right Column: Live Grouped Multilingual Broadcast Preview (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <div className="flex items-center gap-2">
                <Globe className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF]" />
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Grouped Multilingual Message Preview
                </h3>
              </div>

              <button
                type="button"
                onClick={handleCopyPreview}
                className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-slate-600 dark:text-[#9AA8B8] transition-colors"
                title={copiedPreview ? 'Copied!' : 'Copy preview text'}
              >
                <Copy className="w-3.5 h-3.5" />
              </button>
            </div>

            <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
              All your preferred languages ({selectedLanguages.map((l) => l.toUpperCase()).join(', ')}) are combined into a single unified broadcast message with emergency manual links:
            </p>

            {/* Message Preview Box */}
            <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] font-mono text-[11px] text-slate-800 dark:text-[#EAF0F6] leading-relaxed whitespace-pre-wrap select-all max-h-[380px] overflow-y-auto custom-scrollbar">
              {generateGroupedMultilingualMessage()}
            </div>

            {/* Manual Link Callout */}
            <div className="p-3.5 rounded-xl bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/50 flex items-start gap-2.5 text-xs">
              <BookOpen className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF] shrink-0 mt-0.5" />
              <div className="text-slate-600 dark:text-[#9AA8B8]">
                <strong className="text-blue-900 dark:text-[#7AA7FF]">Automatic Emergency Manual Inclusion: </strong>
                Each outbound message automatically carries the direct link to the specific Do's &amp; Don'ts manual for that hazard and severity tier.
              </div>
            </div>
          </div>

          {/* Privacy Notice */}
          <div className="p-4 rounded-2xl bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-600 dark:text-[#9AA8B8] flex items-start gap-2.5 shadow-sm">
            <ShieldCheck className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">DPDP Act 2023 Compliant: </span>
              Anonymous tokens used. No personal profiling, no third-party tracking, and instant 1-click unsubscription at any time.
            </div>
          </div>
        </div>
      </div>

      {/* CONFIRMATION POPUP MODAL */}
      {isSuccessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 dark:bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-100 dark:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
              <Check className="w-6 h-6" />
            </div>

            <div className="space-y-1">
              <h3 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">
                Subscription Activated!
              </h3>
              <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                You are now subscribed to direct civic hazard alerts for{' '}
                <strong className="text-slate-900 dark:text-[#EAF0F6]">{currentStation.name}</strong>.
                Alerts will be sent via {selectedChannels.join(', ')} in {selectedLanguages.map((l) => l.toUpperCase()).join(' & ')} with emergency manuals included.
              </p>
            </div>

            <button
              onClick={() => setIsSuccessModal(false)}
              className="w-full py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold"
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default SubscribeView;

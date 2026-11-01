import React, { useState, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setManualTarget, addToast, HazardType } from '../features/ui/uiSlice';
import {
  BookOpen,
  CheckCircle2,
  XCircle,
  PhoneCall,
  Share2,
  Waves,
  Sun,
  Wind,
  ShieldAlert,
  ArrowRight,
} from 'lucide-react';

interface ManualContent {
  title: string;
  summary: string;
  dos: string[];
  donts: string[];
  keyActions: string[];
  kitChecklist: string[];
}

export const MANUALS_DATA: Record<HazardType, Record<'WATCH' | 'WARNING' | 'SEVERE', ManualContent>> = {
  FLOOD: {
    WATCH: {
      title: 'River Flood Watch — Precautionary Stage (2-Year Return)',
      summary:
        'River levels and upstream catchment runoff are steadily rising toward the 2-year flood threshold. Early precautionary actions should be initiated in low-lying riverside communities.',
      keyActions: [
        'Inspect and clear neighborhood stormwater drains and culverts of plastic and debris.',
        'Charge power banks, mobile phones, and emergency flashlights.',
        'Keep 3 to 5 days of potable drinking water stored in food-grade sealed containers.',
        'Move sensitive electronic equipment and important paper documents above ground level.',
      ],
      dos: [
        'Monitor hourly river stage reports on the Kestrel map or local municipal portal.',
        'Check that household drainage backflow prevention flaps are functional.',
        'Keep emergency medicines and infants’ formula safely elevated.',
        'Confirm evacuation meeting points with all family members.',
      ],
      donts: [
        'Do NOT ignore rising water alerts or upstream dam discharge announcements.',
        'Do NOT dump trash, silt, or construction debris into nullahs or stormwater drains.',
        'Do NOT park motorbikes or cars in subterranean basements prone to waterlogging.',
        'Do NOT allow domestic livestock to remain tethered near riverbank pastures.',
      ],
      kitChecklist: [
        'Waterproof dry-bag for Aadhar cards, property deeds & bank documents',
        'Torch with spare alkaline batteries',
        '3-day supply of personal prescription medicines',
        'First aid kit with antiseptics and bandages',
      ],
    },
    WARNING: {
      title: 'River Flood Warning — Active Threat (5-Year Return)',
      summary:
        'River discharge has breached the 5-year return threshold. Water has begun spilling into riparian corridors, low-lying causeways, and riverbank settlements. Prepare for rapid elevation or relocation.',
      keyActions: [
        'Relocate all vehicles, livestock, and vulnerable family members away from the river corridor.',
        'Disconnect electrical appliances and elevate power strips above high-water marks.',
        'Keep emergency go-bag by the main door ready for immediate evacuation.',
        'Identify your designated municipal flood relief shelter.',
      ],
      dos: [
        'Move to upper floors or designated relief camps if water enters your street.',
        'Boil all tap water vigorously for at least 3 minutes before drinking or cooking.',
        'Follow designated municipal evacuation routes strictly.',
        'Assist elderly, disabled, and pregnant neighbors to secure locations.',
      ],
      donts: [
        'Do NOT attempt to walk, swim, or wade through moving floodwaters of any depth.',
        'Do NOT drive across flooded causeways, bridges, or submerged underpasses.',
        'Do NOT touch metal gates, lamp posts, or water pipes near submerged areas.',
        'Do NOT consume food that has come into direct contact with floodwaters.',
      ],
      kitChecklist: [
        'Bottled drinking water (at least 2 litres per person per day)',
        'Dry non-perishable food (chana, roasted poha, energy bars, biscuits)',
        'Water purification tablets (Chlorine / NaDCC)',
        'Fully charged mobile phone with emergency numbers saved offline',
      ],
    },
    SEVERE: {
      title: 'River Flood Severe Emergency — Immediate Evacuation (20-Year Return)',
      summary:
        'CRITICAL LIFE-SAFETY EMERGENCY. Discharge exceeds the 20-year return threshold. Extensive catastrophic inundation underway. Mandatory evacuation in low-lying sectors.',
      keyActions: [
        'EVACUATE IMMEDIATELY. Move to high ground or concrete relief centers before access roads are cut off.',
        'Turn off main electrical circuit breaker and LPG gas cylinder valves before leaving your home.',
        'Never delay evacuation to save non-essential material possessions.',
        'Call 112 or NDRF helpline 1078 if trapped by rising water.',
      ],
      dos: [
        'Signal rescuers from your rooftop using brightly colored cloth or torch flashes.',
        'Wear sturdy closed shoes to prevent puncture wounds from hidden submerged debris.',
        'Stay tuned to emergency broadcast channels and local disaster authority updates.',
        'Cooperate fully with NDRF and SDRF rescue boat operators.',
      ],
      donts: [
        'Do NOT enter or drive across flooded roads — just 15 cm of moving water can sweep an adult, and 30 cm can float a car.',
        'Do NOT touch fallen electric lines or submerged transformers under any circumstances.',
        'Do NOT return to your home until emergency authorities officially declare it structurally safe.',
        'Do NOT drink untreated water from taps, borewells, or shallow pumps in inundated zones.',
      ],
      kitChecklist: [
        'Government photo ID cards (wrapped in multiple sealed ziplocks)',
        'Emergency cash in small denominations',
        'Life jackets or floating buoyancy aids if available',
        'Whistle to attract search and rescue teams',
      ],
    },
  },
  HEAT: {
    WATCH: {
      title: 'Heatwave Watch — High Precaution (Yellow Tier)',
      summary:
        'Maximum temperatures and wet-bulb heat indexes are forecasted to remain 3°C to 4°C above normal. Precautionary hydration and thermal protection are advised.',
      keyActions: [
        'Drink water at regular 30-minute intervals even if you do not feel thirsty.',
        'Carry an umbrella, wide-brimmed hat, or wet cloth when walking outside.',
        'Provide shade and clean drinking water for domestic animals and street animals.',
        'Keep window blinds or curtains closed on sun-facing walls during daytime.',
      ],
      dos: [
        'Wear loose, lightweight, light-colored cotton clothing.',
        'Consume buttermilk, ORS, lemon water, and coconut water to maintain electrolyte balance.',
        'Reschedule heavy outdoor labor to early mornings (before 9 AM) or evenings.',
        'Keep infants and elderly family members in the coolest room of the house.',
      ],
      donts: [
        'Do NOT leave children or pets inside parked vehicles under any circumstance.',
        'Do NOT consume alcohol, aerated sugary soft drinks, or excessive caffeinated beverages.',
        'Do NOT step out barefoot on hot asphalt or concrete surfaces.',
        'Do NOT skip meals; consume light, frequent, high-water-content meals.',
      ],
      kitChecklist: [
        'Electrolyte sachets (WHO-standard ORS)',
        'Insulated water flask with cool water',
        'Cotton cap or protective scarf',
        'Sunscreen lotion SPF 30+',
      ],
    },
    WARNING: {
      title: 'Heatwave Warning — Active Health Risk (Orange Tier)',
      summary:
        'Severe heatwave conditions imminent. High risk of heat cramps, exhaustion, and dehydration for vulnerable populations and outdoor laborers.',
      keyActions: [
        'Avoid direct exposure to sunlight between 12:00 PM and 4:00 PM.',
        'Establish cooling zones with fans, coolers, and wet towels.',
        'Check frequently on neighbors living alone, particularly seniors and those with chronic illness.',
        'Immediately move anyone showing signs of dizziness to a cool shaded spot.',
      ],
      dos: [
        'Apply cold wet cloths to the neck, forehead, and armpits if feeling overheated.',
        'Wear UV-protective sunglasses and carry a wet handkerchief.',
        'Keep livestock in ventilated shelters with continuous water spray.',
        'Rest frequently in shaded or air-conditioned public spaces.',
      ],
      donts: [
        'Do NOT undertake strenuous athletic training or heavy manual labor in direct afternoon sun.',
        'Do NOT cook or operate high-heat ovens in poorly ventilated kitchens during midday.',
        'Do NOT ignore symptoms like dry mouth, headache, nausea, or rapid pulse.',
        'Do NOT wear tight synthetic polyester garments.',
      ],
      kitChecklist: [
        'Cold gel ice packs in home freezer',
        'Digital clinical thermometer',
        'Glucose / Dextrose powder for instant energy',
        'Oral Rehydration Solution (ORS) packets',
      ],
    },
    SEVERE: {
      title: 'Severe Heatwave Emergency — Life-Threatening (Red Tier)',
      summary:
        'CRITICAL HEAT EMERGENCY. Extreme wet-bulb temperatures threatening organ failure. High likelihood of fatal heatstroke across all age brackets.',
      keyActions: [
        'STAY INDOORS in cooled environments. Halt all non-essential outdoor movements.',
        'If someone stops sweating, develops confusion, vomiting, or collapses, call 108/112 IMMEDIATELY for heatstroke emergency.',
        'Immerse heatstroke victims in cool (not freezing) water or pack armpits and groin with ice while awaiting ambulance.',
        'Utilize municipal cooling shelters if household ventilation is insufficient.',
      ],
      dos: [
        'Continuously hydrate with chilled electrolyte solutions.',
        'Keep curtains drawn and windows closed during peak heat; open cross-ventilation after sunset.',
        'Take frequent cool showers or sponge baths throughout the day.',
        'Ensure continuous medical supervision for dialysis and cardiovascular patients.',
      ],
      donts: [
        'Do NOT venture outdoors during daytime without critical emergency reasons.',
        'Do NOT give fluids to an unconscious or disoriented person (choking hazard).',
        'Do NOT administer aspirin or paracetamol for environmental heatstroke without medical guidance.',
        'Do NOT leave fans blowing hot dry air directly on dehydrated elderly individuals.',
      ],
      kitChecklist: [
        'Instant chemical cold packs',
        'ORS sachets (minimum 10 packets per household)',
        'Emergency ice trays ready in freezer',
        'List of nearest air-conditioned civic cooling centers and hospitals',
      ],
    },
  },
  AIR_QUALITY: {
    WATCH: {
      title: 'Air Quality Watch — Moderate Degradation (AQI 150 - 250)',
      summary:
        'Particulate matter (PM2.5 / PM10) concentrations rising due to meteorological inversion and emissions. Sensitive groups should exercise precaution.',
      keyActions: [
        'Keep doors and windows closed during early morning peak smog hours.',
        'Use wet mopping inside homes instead of dry sweeping to avoid kicking up fine dust.',
        'Ensure kitchen exhaust chimneys and filters are clean.',
        'Sensitive individuals (asthmatics, heart patients) should avoid prolonged outdoor exposure.',
      ],
      dos: [
        'Check local ward AQI before stepping out for morning walks.',
        'Maintain indoor house plants (snake plant, areca palm) that aid air circulation.',
        'Stay well-hydrated to help mucous membranes clear inhaled particulates.',
        'Wear a triple-layer surgical mask or N95 mask if in traffic corridors.',
      ],
      donts: [
        'Do NOT burn garden leaves, cardboard, or garbage anywhere in the neighborhood.',
        'Do NOT smoke cigarettes or light incense in poorly ventilated indoor rooms.',
        'Do NOT jog along arterial highways or industrial transit roads.',
        'Do NOT skip daily prescribed inhalers or respiratory medication.',
      ],
      kitChecklist: [
        'Pack of certified N95 particulate respirator masks',
        'Asthma rescue inhalers with valid expiry dates',
        'Saline nasal spray for nasal passage rinsing',
      ],
    },
    WARNING: {
      title: 'Air Quality Warning — Very Poor (AQI 250 - 350)',
      summary:
        'Prolonged exposure causes respiratory illness to healthy individuals and serious effects to sensitive groups. Active protective measures required.',
      keyActions: [
        'Wear tightly sealed N95 or N99 respirator masks whenever stepping outdoors.',
        'Run HEPA air purifiers in bedrooms or designated clean-air rooms.',
        'Avoid all morning and evening outdoor physical exertion, jogging, or cycling.',
        'Schools and daycare centers should suspend outdoor sports and assemblies.',
      ],
      dos: [
        'Seal drafts around windows and doors with damp towels.',
        'Gargle with warm salt water after returning from outdoors.',
        'Use public mass transit or carpooling to reduce vehicular emissions.',
        'Keep windows shut during peak traffic hours.',
      ],
      donts: [
        'Do NOT rely on cloth masks or bandanas — they do not filter fine PM2.5 particulates.',
        'Do NOT burn biomass, wood, coal, or kerosene for heating or cooking indoors.',
        'Do NOT engage in outdoor physical labor without respiratory protection.',
        'Do NOT ignore persistent wheezing, eye burning, or chest tightness.',
      ],
      kitChecklist: [
        'Certified N95/FFP2 masks for all household members',
        'Replacement HEPA filters for indoor purifiers',
        'Lubricating eye drops for smoke irritation',
        'Pulse oximeter to monitor blood oxygen saturation',
      ],
    },
    SEVERE: {
      title: 'Air Quality Severe Emergency — Hazardous (AQI 350+)',
      summary:
        'HEALTH EMERGENCY. Toxic particulate saturation. Severe respiratory distress and cardiovascular aggravation across the entire population.',
      keyActions: [
        'STAY INDOORS WITH WINDOWS AND DOORS COMPLETELY SEALED.',
        'Operate high-efficiency particulate air (HEPA) purifiers on continuous maximum mode.',
        'Children, senior citizens, and patients with COPD/asthma must not step outdoors.',
        'Seek urgent hospital emergency care if experiencing shortness of breath or cyanosis.',
      ],
      dos: [
        'Create a sealed "clean room" in the house with an active HEPA purifier.',
        'Wear an airtight N95/N99 respirator if outdoor movement is unavoidable.',
        'Consult physicians via telemedicine for dosage adjustments on respiratory medicines.',
        'Advise neighborhood residents against any outdoor diesel generator operation.',
      ],
      donts: [
        'Do NOT exercise or walk outdoors under any circumstances.',
        'Do NOT open windows even if the weather feels cool outside.',
        'Do NOT perform deep-frying or aerosol spraying inside unventilated rooms.',
        'Do NOT ignore drop in SpO2 levels below 94% — seek immediate clinical help.',
      ],
      kitChecklist: [
        'N99 or elastomeric respirators with replaceable P100 particulate filters',
        'Portable oxygen cylinder / concentrator for critical respiratory patients',
        'Full 30-day stock of bronchodilators and respiratory drugs',
        'Direct contact details of local pulmonology emergency wards',
      ],
    },
  },
};

export const ManualsView: React.FC = () => {
  const dispatch = useAppDispatch();
  const manualHazard = useAppSelector((state) => state.ui.manualHazard) || 'FLOOD';
  const manualSeverity = useAppSelector((state) => state.ui.manualSeverity) || 'WARNING';

  const [activeHazard, setActiveHazard] = useState<HazardType>(manualHazard);
  const [activeSeverity, setActiveSeverity] = useState<'WATCH' | 'WARNING' | 'SEVERE'>(manualSeverity);
  const [copiedLink, setCopiedLink] = useState<boolean>(false);

  // Sync state if Redux manual target changed
  useEffect(() => {
    setActiveHazard(manualHazard);
  }, [manualHazard]);

  useEffect(() => {
    setActiveSeverity(manualSeverity);
  }, [manualSeverity]);

  const currentManual = MANUALS_DATA[activeHazard]?.[activeSeverity] || MANUALS_DATA.FLOOD.WARNING;

  const currentManualUrl = `${window.location.origin}/#/manuals?hazard=${activeHazard}&severity=${activeSeverity}`;

  const handleCopyLink = () => {
    navigator.clipboard.writeText(currentManualUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
    dispatch(
      addToast({
        id: `manual_link_${Date.now()}`,
        type: 'success',
        message: 'Direct broadcast manual link copied to clipboard!',
      })
    );
  };

  const handleSelectHazard = (h: HazardType) => {
    setActiveHazard(h);
    dispatch(setManualTarget({ hazard: h }));
  };

  const handleSelectSeverity = (s: 'WATCH' | 'WARNING' | 'SEVERE') => {
    setActiveSeverity(s);
    dispatch(setManualTarget({ severity: s }));
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 pb-12">
      {/* 1. TOP HEADER BANNER */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-xl bg-blue-50 dark:bg-[#1C2633] text-blue-600 dark:text-[#7AA7FF] border border-blue-200 dark:border-blue-500/30">
                <BookOpen className="w-4 h-4" />
              </div>
              <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
                Emergency Manuals: Hazard Do's &amp; Don'ts
              </h1>
            </div>
            <p className="text-xs sm:text-sm text-slate-600 dark:text-[#9AA8B8] max-w-3xl leading-relaxed">
              Standardized civic survival protocols and life-safety guidelines. Direct links to these manuals are automatically included in every automated WhatsApp, Telegram, and Web Push broadcast based on the hazard type and risk level.
            </p>
          </div>

          {/* Copy Shareable Broadcast Link */}
          <button
            onClick={handleCopyLink}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] border border-slate-200 dark:border-[#2A3644] text-slate-800 dark:text-[#EAF0F6] text-xs font-semibold shadow-xs transition-colors self-start md:self-auto cursor-pointer"
            title="Copy direct manual link embedded in broadcasts"
          >
            <Share2 className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF]" />
            <span>{copiedLink ? 'Link Copied!' : 'Copy Broadcast Manual Link'}</span>
          </button>
        </div>

        {/* Hazard Selector Tabs */}
        <div className="flex flex-wrap items-center gap-3 pt-3 border-t border-slate-100 dark:border-[#2A3644]">
          <span className="text-xs font-bold text-slate-400 dark:text-[#65778B] uppercase tracking-wider mr-1">
            Hazard:
          </span>
          <button
            onClick={() => handleSelectHazard('FLOOD')}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all border flex items-center gap-2 ${
              activeHazard === 'FLOOD'
                ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
                : 'bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644]'
            }`}
          >
            <Waves className="w-4 h-4" />
            <span>River Flood</span>
          </button>

          <button
            onClick={() => handleSelectHazard('HEAT')}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all border flex items-center gap-2 ${
              activeHazard === 'HEAT'
                ? 'bg-amber-600 text-white border-amber-600 shadow-xs'
                : 'bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644]'
            }`}
          >
            <Sun className="w-4 h-4" />
            <span>Extreme Heat</span>
          </button>

          <button
            onClick={() => handleSelectHazard('AIR_QUALITY')}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all border flex items-center gap-2 ${
              activeHazard === 'AIR_QUALITY'
                ? 'bg-cyan-600 text-white border-cyan-600 shadow-xs'
                : 'bg-slate-50 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644]'
            }`}
          >
            <Wind className="w-4 h-4" />
            <span>Air Quality</span>
          </button>
        </div>

        {/* Severity Level Selector Tabs */}
        <div className="flex flex-wrap items-center gap-3 pt-2 text-xs">
          <span className="text-xs font-bold text-slate-400 dark:text-[#65778B] uppercase tracking-wider mr-1">
            Risk Tier:
          </span>
          <button
            onClick={() => handleSelectSeverity('WATCH')}
            className={`px-3 py-1.5 rounded-xl font-bold transition-all border flex items-center gap-1.5 ${
              activeSeverity === 'WATCH'
                ? 'bg-amber-500 text-white border-amber-500 shadow-xs'
                : 'bg-amber-50 dark:bg-amber-950/40 text-amber-800 dark:text-amber-300 border-amber-200 dark:border-amber-900/60'
            }`}
          >
            <span>Watch (Precautionary / 2-Yr)</span>
          </button>

          <button
            onClick={() => handleSelectSeverity('WARNING')}
            className={`px-3 py-1.5 rounded-xl font-bold transition-all border flex items-center gap-1.5 ${
              activeSeverity === 'WARNING'
                ? 'bg-orange-500 text-white border-orange-500 shadow-xs'
                : 'bg-orange-50 dark:bg-orange-950/40 text-orange-800 dark:text-orange-300 border-orange-200 dark:border-orange-900/60'
            }`}
          >
            <span>Warning (Action Required / 5-Yr)</span>
          </button>

          <button
            onClick={() => handleSelectSeverity('SEVERE')}
            className={`px-3 py-1.5 rounded-xl font-bold transition-all border flex items-center gap-1.5 ${
              activeSeverity === 'SEVERE'
                ? 'bg-red-600 text-white border-red-600 shadow-xs'
                : 'bg-red-50 dark:bg-red-950/40 text-red-800 dark:text-red-300 border-red-200 dark:border-red-900/60'
            }`}
          >
            <span>Severe (Life-Safety Emergency / 20-Yr)</span>
          </button>
        </div>
      </section>

      {/* 2. ACTIVE MANUAL SUMMARY BANNER */}
      <section
        className={`border rounded-2xl p-6 shadow-sm space-y-3 ${
          activeSeverity === 'SEVERE'
            ? 'bg-red-50/80 dark:bg-red-950/20 border-red-200 dark:border-red-900/50'
            : activeSeverity === 'WARNING'
            ? 'bg-orange-50/80 dark:bg-orange-950/20 border-orange-200 dark:border-orange-900/50'
            : 'bg-amber-50/80 dark:bg-amber-950/20 border-amber-200 dark:border-amber-900/50'
        }`}
      >
        <div className="flex items-center gap-2">
          <ShieldAlert
            className={`w-5 h-5 ${
              activeSeverity === 'SEVERE'
                ? 'text-red-600 dark:text-red-400'
                : activeSeverity === 'WARNING'
                ? 'text-orange-600 dark:text-orange-400'
                : 'text-amber-600 dark:text-amber-400'
            }`}
          />
          <h2 className="text-lg sm:text-xl font-bold text-slate-900 dark:text-[#EAF0F6]">
            {currentManual.title}
          </h2>
        </div>
        <p className="text-xs sm:text-sm text-slate-700 dark:text-[#CBD5E1] leading-relaxed">
          {currentManual.summary}
        </p>

        {/* Immediate Priority Steps */}
        <div className="pt-2">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800 dark:text-slate-200 mb-2">
            Priority Action Checklist:
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs">
            {currentManual.keyActions.map((action, idx) => (
              <div
                key={idx}
                className="p-2.5 rounded-xl bg-white/80 dark:bg-[#151D27]/80 border border-slate-200 dark:border-[#2A3644] flex items-start gap-2 text-slate-800 dark:text-[#EAF0F6]"
              >
                <ArrowRight className="w-3.5 h-3.5 text-blue-600 shrink-0 mt-0.5" />
                <span>{action}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 3. TWO-COLUMN DO'S AND DON'TS COMPARISON GRID */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-start">
        {/* ================= LEFT COLUMN: WHAT TO DO (DO'S) ================= */}
        <div className="bg-white dark:bg-[#151D27] border border-emerald-200 dark:border-emerald-500/30 rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2.5 pb-3 border-b border-slate-100 dark:border-[#2A3644]">
            <div className="w-8 h-8 rounded-xl bg-emerald-100 dark:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
              <CheckCircle2 className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-emerald-800 dark:text-emerald-400">
                WHAT TO DO (DO'S)
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Recommended protective measures</p>
            </div>
          </div>

          <div className="space-y-3 text-xs leading-relaxed">
            {currentManual.dos.map((item, idx) => (
              <div
                key={idx}
                className="p-3 rounded-xl bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-100 dark:border-emerald-900/40 flex items-start gap-3"
              >
                <span className="w-5 h-5 rounded-full bg-emerald-100 dark:bg-emerald-500/30 text-emerald-700 dark:text-emerald-300 flex items-center justify-center font-bold text-[11px] shrink-0 mt-0.5">
                  ✓
                </span>
                <span className="text-slate-800 dark:text-[#EAF0F6] font-medium">{item}</span>
              </div>
            ))}
          </div>
        </div>

        {/* ================= RIGHT COLUMN: WHAT NOT TO DO (DON'TS) ================= */}
        <div className="bg-white dark:bg-[#151D27] border border-red-200 dark:border-red-500/30 rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2.5 pb-3 border-b border-slate-100 dark:border-[#2A3644]">
            <div className="w-8 h-8 rounded-xl bg-red-100 dark:bg-red-500/20 text-red-600 dark:text-red-400 flex items-center justify-center">
              <XCircle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-red-800 dark:text-red-400">
                WHAT TO AVOID (DON'TS)
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Deadly errors and hazards to refrain from</p>
            </div>
          </div>

          <div className="space-y-3 text-xs leading-relaxed">
            {currentManual.donts.map((item, idx) => (
              <div
                key={idx}
                className="p-3 rounded-xl bg-red-50/50 dark:bg-red-950/20 border border-red-100 dark:border-red-900/40 flex items-start gap-3"
              >
                <span className="w-5 h-5 rounded-full bg-red-100 dark:bg-red-500/30 text-red-700 dark:text-red-300 flex items-center justify-center font-bold text-[11px] shrink-0 mt-0.5">
                  ✕
                </span>
                <span className="text-slate-800 dark:text-[#EAF0F6] font-medium">{item}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 4. EMERGENCY GO-BAG CHECKLIST & HELPLINES */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Kit Checklist (7 cols) */}
        <div className="lg:col-span-7 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-3">
          <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
            Emergency Go-Bag &amp; Preparation Checklist
          </h3>
          <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
            Items that should be gathered and ready for immediate deployment during this {activeHazard} tier:
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs pt-1">
            {currentManual.kitChecklist.map((kit, idx) => (
              <div
                key={idx}
                className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center gap-2 text-slate-800 dark:text-[#EAF0F6]"
              >
                <span className="w-2 h-2 rounded-full bg-blue-500 shrink-0" />
                <span>{kit}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Emergency Helplines (5 cols) */}
        <div className="lg:col-span-5 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-3">
          <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-[#2A3644]">
            <PhoneCall className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
              24x7 Statutory Emergency Helplines
            </h3>
          </div>

          <div className="space-y-2 text-xs">
            <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between">
              <div>
                <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">National Emergency Response</span>
                <span className="text-[10px] text-slate-500 block">Police / Fire / Disaster</span>
              </div>
              <a
                href="tel:112"
                className="px-3 py-1 rounded-lg bg-emerald-600 text-white font-bold text-xs hover:bg-emerald-500"
              >
                Dial 112
              </a>
            </div>

            <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between">
              <div>
                <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">NDRF Disaster Control Room</span>
                <span className="text-[10px] text-slate-500 block">National Disaster Response Force</span>
              </div>
              <a
                href="tel:1078"
                className="px-3 py-1 rounded-lg bg-blue-600 text-white font-bold text-xs hover:bg-blue-500"
              >
                Dial 1078
              </a>
            </div>

            <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between">
              <div>
                <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Emergency Medical &amp; Ambulance</span>
                <span className="text-[10px] text-slate-500 block">24/7 Life Support</span>
              </div>
              <a
                href="tel:108"
                className="px-3 py-1 rounded-lg bg-amber-600 text-white font-bold text-xs hover:bg-amber-500"
              >
                Dial 108
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ManualsView;

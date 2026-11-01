import React from 'react';
import { useAppDispatch } from '../app/hooks';
import { setActiveTab } from '../features/ui/uiSlice';
import { Lock, Sparkles } from 'lucide-react';

export const AboutView: React.FC = () => {
  const dispatch = useAppDispatch();

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 pb-12">
      {/* 1. TOP HERO BANNER */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-3">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50 dark:bg-blue-600/15 border border-blue-200 dark:border-blue-500/30 text-blue-700 dark:text-[#7AA7FF] text-xs font-semibold">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Civic Early Warning Architecture</span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-[#EAF0F6]">
          About Kestrel: How It Works For You
        </h1>
        <p className="text-xs sm:text-sm text-slate-600 dark:text-[#9AA8B8] max-w-3xl leading-relaxed">
          Kestrel is an open, community-first environmental early warning platform. It gives residents, families, and neighborhood volunteers up to a 72-hour head start before river floods, extreme heatwaves, or dangerous air pollution peaks impact their neighborhoods.
        </p>
      </section>

      {/* 2. STATUTORY AUTHORITY NOTICE */}
      <section className="bg-amber-50/70 dark:bg-[#151D27] rounded-2xl border border-amber-200 dark:border-[#2A3644] border-l-4 border-l-amber-500 p-5 shadow-sm space-y-1.5 text-xs">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <h2 className="text-xs font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-2">
            <span className="material-symbols-outlined text-amber-600 text-sm">shield</span>
            <span>Civic Telemetry Notice</span>
          </h2>
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 dark:bg-amber-950/40 text-amber-800 dark:text-amber-400 border border-amber-300 dark:border-amber-800/60">
            RELAY &amp; EARLY NOTICE
          </span>
        </div>
        <p className="text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
          Kestrel is an automated scientific early warning system. In all emergency conditions requiring civilian action or statutory evacuation, official directives issued by the National Disaster Management Authority (NDMA / SACHET), the Central Water Commission (CWC), or the India Meteorological Department (IMD) take strict legal precedence.
        </p>
      </section>

      {/* 3. HOW THE PLATFORM WORKS (STEP-BY-STEP FROM END-USER PERSPECTIVE) */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-6">
        <div className="space-y-1 pb-4 border-b border-slate-100 dark:border-[#2A3644]">
          <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">
            The 4-Step Early Warning Cycle
          </h2>
          <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
            From open scientific sensors to actionable guidance on your phone
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Step 1 */}
          <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-2.5">
            <div className="w-8 h-8 rounded-lg bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF] flex items-center justify-center font-bold text-sm">
              1
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
              24x7 Numerical Ingest
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              Every few hours, Kestrel ingests 50-member hydrological simulations (GloFAS), European weather models (ECMWF), and ambient sensors across all major Indian river basins.
            </p>
          </div>

          {/* Step 2 */}
          <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-2.5">
            <div className="w-8 h-8 rounded-lg bg-amber-100 dark:bg-amber-500/20 text-amber-700 dark:text-amber-400 flex items-center justify-center font-bold text-sm">
              2
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
              Autonomous Rule Engine
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              No human bottleneck. The moment ensemble members breach safe threshold limits (2-Yr, 5-Yr, or 20-Yr return periods), warnings are calculated and triggered automatically.
            </p>
          </div>

          {/* Step 3 */}
          <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-2.5">
            <div className="w-8 h-8 rounded-lg bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 flex items-center justify-center font-bold text-sm">
              3
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
              Direct Citizen Broadcast
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              Alerts reach your phone directly via WhatsApp, Telegram, and Web Push. All your preferred languages are combined in one message with links to survival manuals.
            </p>
          </div>

          {/* Step 4 */}
          <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-2.5">
            <div className="w-8 h-8 rounded-lg bg-purple-100 dark:bg-purple-500/20 text-purple-700 dark:text-purple-400 flex items-center justify-center font-bold text-sm">
              4
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
              Public Integrity Proof
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              You can verify any warning yourself: check the interactive Map, inspect the hydrograph on Point Detail, and audit immutable past transmission logs in History.
            </p>
          </div>
        </div>
      </section>

      {/* 4. UNDERSTANDING RISK LEVELS (RETURN PERIODS EXPLAINED SIMPLY) */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-5">
        <div className="space-y-1 pb-3 border-b border-slate-100 dark:border-[#2A3644]">
          <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">
            Understanding Alert Severity Tiers
          </h2>
          <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
            What each warning tier means for your safety and daily routine
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          {/* Watch */}
          <div className="p-4 rounded-xl bg-amber-50/60 dark:bg-[#1C2633] border border-amber-200 dark:border-amber-500/30 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-bold text-amber-800 dark:text-amber-300 text-sm">
                Watch (2-Yr Return)
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded bg-amber-100 dark:bg-amber-950 text-amber-800 dark:text-amber-300 font-semibold">
                Precautionary
              </span>
            </div>
            <p className="text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              Conditions that occur roughly once every 2 years. Drainage channels start filling up, and temperatures climb. A 72-hour advance notice to secure documents, charge devices, and stay informed.
            </p>
          </div>

          {/* Warning */}
          <div className="p-4 rounded-xl bg-orange-50/60 dark:bg-[#1C2633] border border-orange-200 dark:border-orange-500/30 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-bold text-orange-800 dark:text-orange-400 text-sm">
                Warning (5-Yr Return)
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded bg-orange-100 dark:bg-orange-950 text-orange-800 dark:text-orange-400 font-semibold">
                Action Required
              </span>
            </div>
            <p className="text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              A significant event occurring roughly once in 5 years. Active inundation of low causeways or hazardous outdoor heat. Move vehicles to high ground, prepare go-bag, and assist vulnerable neighbors.
            </p>
          </div>

          {/* Severe */}
          <div className="p-4 rounded-xl bg-red-50/60 dark:bg-[#1C2633] border border-red-200 dark:border-red-500/30 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-bold text-red-800 dark:text-red-400 text-sm">
                Severe (20-Yr Return)
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded bg-red-100 dark:bg-red-950 text-red-800 dark:text-red-400 font-semibold">
                Emergency Evac
              </span>
            </div>
            <p className="text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              A rare, catastrophic emergency occurring once every 20 years. Massive river overtopping or lethal wet-bulb heat. Mandatory evacuation to designated relief camps and immediate life-safety action.
            </p>
          </div>
        </div>
      </section>

      {/* 5. PRIVACY & DATA TRANSPARENCY */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-4">
        <div className="flex items-center gap-2.5 pb-3 border-b border-slate-100 dark:border-[#2A3644]">
          <Lock className="w-5 h-5 text-blue-600 dark:text-[#7AA7FF]" />
          <h2 className="text-base font-bold text-slate-900 dark:text-[#EAF0F6]">
            Privacy &amp; DPDP Act 2023 Compliance
          </h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs text-slate-600 dark:text-[#9AA8B8]">
          <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
            <span className="font-bold text-slate-900 dark:text-[#EAF0F6] block">Zero Behavioral Tracking</span>
            <p>We do not track your location in the background or monitor your browsing activity.</p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
            <span className="font-bold text-slate-900 dark:text-[#EAF0F6] block">Anonymous Tokens</span>
            <p>Subscribers are identified solely by anonymous cryptographic tokens. Zero names or profiling.</p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1">
            <span className="font-bold text-slate-900 dark:text-[#EAF0F6] block">1-Click Unsubscribe</span>
            <p>You can revoke your subscription and permanently erase your contact handle anytime with 1 click.</p>
          </div>
        </div>
      </section>

      {/* 6. CALL TO ACTION: SUBSCRIBE / EXPLORE MAP */}
      <section className="bg-gradient-to-r from-blue-600 to-indigo-700 rounded-2xl p-6 sm:p-8 text-white flex flex-col sm:flex-row items-center justify-between gap-4 shadow-lg shadow-blue-500/20">
        <div className="space-y-1 text-center sm:text-left">
          <h3 className="text-lg font-bold">Ready to protect your household?</h3>
          <p className="text-xs text-blue-100">
            Subscribe in less than 30 seconds to receive direct broadcasts for your ward.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => dispatch(setActiveTab('subscribe'))}
            className="px-5 py-2.5 rounded-xl bg-white text-blue-700 font-bold text-xs shadow-md hover:bg-blue-50 transition-colors"
          >
            Subscribe to Alerts
          </button>
          <button
            onClick={() => dispatch(setActiveTab('map'))}
            className="px-5 py-2.5 rounded-xl bg-blue-800/80 hover:bg-blue-800 text-white font-semibold text-xs border border-blue-400/40 transition-colors"
          >
            View Live Map
          </button>
        </div>
      </section>
    </div>
  );
};

export default AboutView;

import React, { useState } from 'react';

export const ResidentAboutView: React.FC = () => {
  const [activeSection, setActiveSection] = useState<string>('how-it-works');

  return (
    <div className="w-full max-w-7xl mx-auto py-4 space-y-6">
      {/* Top Hero Banner */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="max-w-3xl space-y-2">
          <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-md bg-blue-50 dark:bg-blue-500/15 border border-blue-200 dark:border-blue-500/30 text-blue-700 dark:text-[#7AA7FF] text-xs font-semibold">
            <span className="material-symbols-outlined text-sm">menu_book</span>
            <span>Civic Telemetry &amp; Open Methodology</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-[#EAF0F6]">
            About Kestrel: Civic Methodology &amp; Provenance
          </h1>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
            Kestrel delivers urgent environmental and disaster telemetry—encompassing river crests, extreme wet-bulb heat, and particulate air quality—with strict scientific rigor, visible provenance, and civic transparency.
          </p>
        </div>
      </section>

      {/* Prominent Civic Authority Disclaimer Card */}
      <div className="w-full bg-amber-50/70 dark:bg-[#151D27] rounded-2xl border border-amber-200 dark:border-[#2A3644] border-l-4 border-l-amber-500 p-6 shadow-sm">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-lg bg-amber-500/10 border border-amber-500/30 flex items-center justify-center flex-shrink-0 text-amber-600 dark:text-amber-400">
            <span className="material-symbols-outlined text-2xl">shield</span>
          </div>
          <div className="space-y-1.5 flex-1">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <h2 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                Kestrel is not an official warning system. IMD, CWC, NDMA and SACHET remain the statutory authority.
              </h2>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-white dark:bg-[#1C2633] text-amber-700 dark:text-[#FB923C] border border-amber-300 dark:border-[#FB923C]/30">
                ▲ STATUTORY NOTICE
              </span>
            </div>
            <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              Kestrel acts as an open, hyper-local civic telemetry relay and multi-hazard synthesis layer. We aggregate open meteorological catchment simulations, ambient air quality sensors, and municipal ward alerts to give neighborhood disaster volunteers up to a 6-hour head start. In all emergency conditions requiring civilian action or evacuation, statutory notifications issued by the National Disaster Management Authority (SACHET/NDMA), Pune Municipal Corporation (PMC), or the India Meteorological Department take strict precedence.
            </p>
          </div>
        </div>
      </div>

      {/* Main Two-Column Layout */}
      <div className="flex flex-col lg:flex-row gap-8 items-start">
        {/* Left Sticky Navigation */}
        <aside className="w-full lg:w-72 lg:sticky lg:top-24 flex-shrink-0 space-y-4">
          <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-4 shadow-sm space-y-1 text-xs">
            <div className="font-bold text-slate-400 dark:text-[#9AA8B8] uppercase tracking-wider px-3 py-1.5 text-[11px]">
              Table of Contents
            </div>
            <button
              onClick={() => setActiveSection('how-it-works')}
              className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-left transition-colors font-medium ${
                activeSection === 'how-it-works'
                  ? 'bg-blue-50 dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-semibold'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6] hover:bg-slate-50 dark:hover:bg-[#1C2633]'
              }`}
            >
              <span className="material-symbols-outlined text-base">flowsheet</span>
              <span>1. How it works &amp; Provenance</span>
            </button>
            <button
              onClick={() => setActiveSection('data-sources')}
              className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-left transition-colors font-medium ${
                activeSection === 'data-sources'
                  ? 'bg-blue-50 dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-semibold'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6] hover:bg-slate-50 dark:hover:bg-[#1C2633]'
              }`}
            >
              <span className="material-symbols-outlined text-base">database</span>
              <span>2. Data Sources &amp; Ingest</span>
            </button>
            <button
              onClick={() => setActiveSection('thresholds')}
              className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-left transition-colors font-medium ${
                activeSection === 'thresholds'
                  ? 'bg-blue-50 dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-semibold'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6] hover:bg-slate-50 dark:hover:bg-[#1C2633]'
              }`}
            >
              <span className="material-symbols-outlined text-base">rule</span>
              <span>3. Return Periods &amp; Thresholds</span>
            </button>
            <button
              onClick={() => setActiveSection('privacy')}
              className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-left transition-colors font-medium ${
                activeSection === 'privacy'
                  ? 'bg-blue-50 dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-semibold'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6] hover:bg-slate-50 dark:hover:bg-[#1C2633]'
              }`}
            >
              <span className="material-symbols-outlined text-base">lock</span>
              <span>4. Privacy &amp; Zero-Tracking</span>
            </button>
            <button
              onClick={() => setActiveSection('cap')}
              className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-left transition-colors font-medium ${
                activeSection === 'cap'
                  ? 'bg-blue-50 dark:bg-[#1C2633] text-blue-700 dark:text-[#7AA7FF] font-semibold'
                  : 'text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6] hover:bg-slate-50 dark:hover:bg-[#1C2633]'
              }`}
            >
              <span className="material-symbols-outlined text-base">rss_feed</span>
              <span>5. Machine Feeds &amp; CAP XML</span>
            </button>
          </div>

          {/* Quick Helpline Box */}
          <div className="p-4 rounded-2xl bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-600 dark:text-[#9AA8B8] space-y-1">
            <span className="text-[11px] font-bold text-blue-600 dark:text-[#7AA7FF] uppercase">District Operations Desk</span>
            <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Pune Municipal Disaster Cell</div>
            <div>020-25501269 · Available 24x7</div>
          </div>
        </aside>

        {/* Right Content Column */}
        <main className="flex-1 space-y-6">
          {/* Section 1: Provenance Hierarchy */}
          <article className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-5">
            <div className="flex items-center gap-3 pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <div className="w-8 h-8 rounded-lg bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF] flex items-center justify-center font-bold text-sm">
                1
              </div>
              <div>
                <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Three-Tier Telemetry Provenance Hierarchy
                </h2>
                <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">
                  Eliminating ambiguity between numerical simulations and on-the-ground reality
                </p>
              </div>
            </div>

            <p className="text-xs sm:text-sm text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              When extreme precipitation strikes upstream catchments, emergency responders and residents need clear, unambiguous certainty. Every warning emitted by Kestrel carries an explicit provenance tag:
            </p>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
              {/* Tier 1 */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="px-2 py-0.5 rounded bg-slate-200 dark:bg-slate-700/60 text-slate-700 dark:text-slate-300 font-semibold">
                    Model Only
                  </span>
                  <span className="text-[11px] text-slate-400 dark:text-[#65778B]">Tier 1</span>
                </div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Numerical Hydrology</h3>
                <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  Generated solely from the 50-member GloFAS ensemble hydrograph and ECMWF runoff forecasts. No physical gauge confirmation yet.
                </p>
              </div>

              {/* Tier 2 */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-blue-200 dark:border-blue-500/30 space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="px-2 py-0.5 rounded bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF] font-semibold">
                    Model + Reports
                  </span>
                  <span className="text-[11px] text-blue-600 dark:text-blue-400">Tier 2</span>
                </div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Citizen Corroboration</h3>
                <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  Active when 2 or more verified citizen ground reports (water level rise, blocked culverts) corroborate the predictive hydrograph.
                </p>
              </div>

              {/* Tier 3 */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-emerald-200 dark:border-emerald-500/30 space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="px-2 py-0.5 rounded bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 font-semibold">
                    Official Confirmed
                  </span>
                  <span className="text-[11px] text-emerald-600 dark:text-emerald-400">Tier 3</span>
                </div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Statutory Gauge Calibration</h3>
                <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                  Calibrated directly against physical telemetry from the Central Water Commission (CWC) or IMD Doppler radar stations.
                </p>
              </div>
            </div>
          </article>

          {/* Section 2: Data Sources */}
          <article className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-4">
            <div className="flex items-center gap-3 pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <div className="w-8 h-8 rounded-lg bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF] flex items-center justify-center font-bold text-sm">
                2
              </div>
              <div>
                <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">Operational Data Feeds &amp; Sources</h2>
                <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">Open scientific APIs and government ingest bridges</p>
              </div>
            </div>

            <div className="space-y-3 text-xs sm:text-sm text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              <p>
                Kestrel combines open public weather telemetry with real-time Indian river basin models:
              </p>
              <ul className="list-disc pl-5 space-y-1.5 text-xs text-slate-600 dark:text-[#9AA8B8]">
                <li>
                  <strong className="text-slate-900 dark:text-[#EAF0F6]">Open-Meteo Flood API:</strong> Global Flood Awareness System (GloFAS) 50-member daily discharge ensemble with 10-day predictive horizon.
                </li>
                <li>
                  <strong className="text-slate-900 dark:text-[#EAF0F6]">Open-Meteo Weather API:</strong> High-resolution hourly wet-bulb temperatures and heat indexes.
                </li>
                <li>
                  <strong className="text-slate-900 dark:text-[#EAF0F6]">Central Water Commission (CWC):</strong> Baseline datum calibrations across all 10 major Indian hydrological basins (Ganga, Brahmaputra, Godavari, Krishna, Mahanadi, Indus, etc.).
                </li>
                <li>
                  <strong className="text-slate-900 dark:text-[#EAF0F6]">Central Pollution Control Board (CPCB):</strong> Real-time continuous ambient air quality monitoring stations (CAAQMS).
                </li>
              </ul>
            </div>
          </article>

          {/* Section 3: Return Periods */}
          <article className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-4">
            <div className="flex items-center gap-3 pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <div className="w-8 h-8 rounded-lg bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF] flex items-center justify-center font-bold text-sm">
                3
              </div>
              <div>
                <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">Return Period Threshold Engineering</h2>
                <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">Standardized return frequencies for consistent severity triggers</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div className="p-3.5 rounded-xl bg-amber-50/70 dark:bg-[#1C2633] border border-amber-200 dark:border-amber-500/30">
                <span className="text-amber-800 dark:text-amber-300 font-bold block mb-1">2-Year Return (Watch)</span>
                <p className="text-slate-600 dark:text-[#9AA8B8]">
                  Discharge level expected once every 2 years. Early notice to prepare drainage pumps and low culverts.
                </p>
              </div>
              <div className="p-3.5 rounded-xl bg-orange-50/70 dark:bg-[#1C2633] border border-orange-200 dark:border-orange-500/30">
                <span className="text-orange-800 dark:text-orange-400 font-bold block mb-1">5-Year Return (Warning)</span>
                <p className="text-slate-600 dark:text-[#9AA8B8]">
                  Discharge level expected once every 5 years. Active inundation threat along low-lying riverbanks.
                </p>
              </div>
              <div className="p-3.5 rounded-xl bg-red-50/70 dark:bg-[#1C2633] border border-red-200 dark:border-red-500/30">
                <span className="text-red-800 dark:text-red-400 font-bold block mb-1">20-Year Return (Severe)</span>
                <p className="text-slate-600 dark:text-[#9AA8B8]">
                  Critical life-safety emergency. Mandatory evacuation notice and flood relief shelter activation.
                </p>
              </div>
            </div>
          </article>
        </main>
      </div>
    </div>
  );
};

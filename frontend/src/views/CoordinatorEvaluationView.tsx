import React from 'react';
import { useAppDispatch } from '../app/hooks';
import { addToast } from '../features/ui/uiSlice';

export const CoordinatorEvaluationView: React.FC = () => {
  const dispatch = useAppDispatch();

  const handleExport = () => {
    dispatch(
      addToast({
        id: `eval_${Date.now()}`,
        type: 'success',
        message: 'Evaluation performance audit report exported (PDF/CSV)!',
      })
    );
  };

  return (
    <div className="w-full max-w-[1380px] mx-auto py-2 space-y-6">
      {/* Top Header & Actions */}
      <div className="bg-white dark:bg-[#151D27] p-5 rounded-2xl border border-slate-200 dark:border-[#2A3644] flex flex-wrap items-center justify-between gap-4 shadow-sm">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
            System Performance &amp; Verification Evaluation
          </h1>
          <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">
            Statistical reliability, lead-time verification, and citizen telemetry feedback audit.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] px-3.5 py-2 rounded-xl text-xs text-slate-800 dark:text-[#EAF0F6] font-semibold flex items-center gap-2 dark:hover:bg-[#253243]">
            <span className="material-symbols-outlined text-sm text-slate-500 dark:text-[#9AA8B8]">date_range</span>
            <span>Evaluation Window: Monsoon 2024 (Last 120d)</span>
          </button>

          <button
            onClick={handleExport}
            className="bg-[#1D4ED8] hover:bg-blue-600 text-white px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 shadow-md shadow-blue-500/20 active:scale-[0.98] transition-all"
          >
            <span className="material-symbols-outlined text-sm">download</span>
            <span>Export Report</span>
          </button>
        </div>
      </div>

      {/* 4 KPI Bento Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1 */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 space-y-3 shadow-sm">
          <div className="flex justify-between items-start text-xs text-slate-500 dark:text-[#9AA8B8]">
            <span className="font-semibold">Alerts Sent</span>
            <span className="bg-slate-100 dark:bg-[#1C2633] px-2 py-0.5 rounded text-[10px]">n = 142 events</span>
          </div>
          <div>
            <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6]">142</div>
            <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">Total dispatches across 3 hazards</p>
          </div>
          <div className="pt-2 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5 font-semibold">
            <span className="material-symbols-outlined text-sm">check_circle</span>
            <span>98.8% confirmed delivery</span>
          </div>
        </div>

        {/* KPI 2 */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 space-y-3 shadow-sm">
          <div className="flex justify-between items-start text-xs text-slate-500 dark:text-[#9AA8B8]">
            <span className="font-semibold">Median Time to First Delivery</span>
            <span className="bg-slate-100 dark:bg-[#1C2633] px-2 py-0.5 rounded text-[10px]">n = 142 dispatches</span>
          </div>
          <div>
            <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6]">14.2 s</div>
            <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">From automated trigger to gateway dispatch</p>
          </div>
          <div className="pt-2 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5 font-semibold">
            <span className="material-symbols-outlined text-sm">speed</span>
            <span>Target &lt; 30 s (Within SLA)</span>
          </div>
        </div>

        {/* KPI 3 */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 space-y-3 shadow-sm">
          <div className="flex justify-between items-start text-xs text-slate-500 dark:text-[#9AA8B8]">
            <span className="font-semibold">False-Alarm Ratio (FAR)</span>
            <span className="bg-slate-100 dark:bg-[#1C2633] px-2 py-0.5 rounded text-[10px]">n = 123 verified</span>
          </div>
          <div>
            <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6]">11.4%</div>
            <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">Uncorroborated warnings without observed impact</p>
          </div>
          <div className="pt-2 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-blue-600 dark:text-[#7AA7FF] flex items-center gap-1.5 font-semibold">
            <span className="material-symbols-outlined text-sm">trending_down</span>
            <span>-2.1% vs 2023 baseline</span>
          </div>
        </div>

        {/* KPI 4 */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 space-y-3 shadow-sm">
          <div className="flex justify-between items-start text-xs text-slate-500 dark:text-[#9AA8B8]">
            <span className="font-semibold">Probability of Detection (POD)</span>
            <span className="bg-slate-100 dark:bg-[#1C2633] px-2 py-0.5 rounded text-[10px]">n = 114 ground events</span>
          </div>
          <div>
            <div className="text-3xl font-bold text-slate-900 dark:text-[#EAF0F6]">88.6%</div>
            <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">Ground-truth threshold exceedances captured</p>
          </div>
          <div className="pt-2 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5 font-semibold">
            <span className="material-symbols-outlined text-sm">verified</span>
            <span>Target &gt; 85% (Optimal)</span>
          </div>
        </div>
      </div>

      {/* Confusion Matrix & Lead-Time Analysis */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Confusion Matrix */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Event Contingency &amp; Confusion Matrix</h3>
          <div className="grid grid-cols-2 gap-3 text-xs">
            <div className="p-4 rounded-xl bg-emerald-50/60 dark:bg-[#1C2633] border border-emerald-200 dark:border-emerald-500/30">
              <span className="text-[11px] text-emerald-800 dark:text-[#9AA8B8] block">True Positives (Hits)</span>
              <span className="text-2xl font-bold text-emerald-700 dark:text-emerald-400">101 events</span>
              <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block mt-0.5">Forecasted &amp; Ground Verified</span>
            </div>
            <div className="p-4 rounded-xl bg-amber-50/60 dark:bg-[#1C2633] border border-amber-200 dark:border-amber-500/30">
              <span className="text-[11px] text-amber-800 dark:text-[#9AA8B8] block">False Positives (False Alarms)</span>
              <span className="text-2xl font-bold text-amber-700 dark:text-amber-400">13 events</span>
              <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block mt-0.5">Forecasted, but water stayed in banks</span>
            </div>
            <div className="p-4 rounded-xl bg-red-50/60 dark:bg-[#1C2633] border border-red-200 dark:border-red-500/30">
              <span className="text-[11px] text-red-800 dark:text-[#9AA8B8] block">False Negatives (Misses)</span>
              <span className="text-2xl font-bold text-red-700 dark:text-red-400">13 events</span>
              <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block mt-0.5">Localized flash inundation unforecasted</span>
            </div>
            <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644]">
              <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8] block">Correct Rejections</span>
              <span className="text-2xl font-bold text-slate-900 dark:text-[#EAF0F6]">820 cycles</span>
              <span className="text-[10px] text-slate-500 dark:text-[#9AA8B8] block mt-0.5">Quiet cycles correctly withheld</span>
            </div>
          </div>
        </div>

        {/* Lead-Time Verification Breakdown */}
        <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
          <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Forecast Advance Warning Lead-Time</h3>
          <div className="space-y-3 text-xs">
            <div className="space-y-1">
              <div className="flex justify-between text-slate-600 dark:text-[#9AA8B8]">
                <span>72-Hour Horizon (Early Advisory)</span>
                <span className="font-semibold text-emerald-600 dark:text-emerald-400">92.4% Reliability</span>
              </div>
              <div className="w-full bg-slate-100 dark:bg-[#1C2633] h-2 rounded-full overflow-hidden">
                <div className="bg-emerald-500 h-2 rounded-full" style={{ width: '92.4%' }}></div>
              </div>
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-slate-600 dark:text-[#9AA8B8]">
                <span>48-Hour Horizon (Civic Warning)</span>
                <span className="font-semibold text-blue-600 dark:text-[#7AA7FF]">94.1% Reliability</span>
              </div>
              <div className="w-full bg-slate-100 dark:bg-[#1C2633] h-2 rounded-full overflow-hidden">
                <div className="bg-blue-500 h-2 rounded-full" style={{ width: '94.1%' }}></div>
              </div>
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-slate-600 dark:text-[#9AA8B8]">
                <span>24-Hour Horizon (Crest Emergency)</span>
                <span className="font-semibold text-cyan-600 dark:text-cyan-400">97.8% Reliability</span>
              </div>
              <div className="w-full bg-slate-100 dark:bg-[#1C2633] h-2 rounded-full overflow-hidden">
                <div className="bg-cyan-500 h-2 rounded-full" style={{ width: '97.8%' }}></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

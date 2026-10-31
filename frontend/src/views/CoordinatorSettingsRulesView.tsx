import React, { useState } from 'react';
import { useAppDispatch } from '../app/hooks';
import { addToast } from '../features/ui/uiSlice';

export const CoordinatorSettingsRulesView: React.FC = () => {
  const dispatch = useAppDispatch();

  // Settings states
  const [rp2Value, setRp2Value] = useState<number>(40);
  const [rp5Value, setRp5Value] = useState<number>(85);
  const [rp20Value, setRp20Value] = useState<number>(180);
  const [consensusPercent, setConsensusPercent] = useState<number>(30);
  const [autoDispatchSevere, setAutoDispatchSevere] = useState<boolean>(true);
  const [quietHoursEnabled, setQuietHoursEnabled] = useState<boolean>(false);
  const [hasChanges, setHasChanges] = useState<boolean>(false);

  const handleSave = () => {
    setHasChanges(false);
    dispatch(
      addToast({
        id: `save_${Date.now()}`,
        type: 'success',
        message: 'Threshold policies and sensitivity rules saved to gateway cluster!',
      })
    );
  };

  const handleReset = () => {
    setRp2Value(40);
    setRp5Value(85);
    setRp20Value(180);
    setConsensusPercent(30);
    setAutoDispatchSevere(true);
    setQuietHoursEnabled(false);
    setHasChanges(false);
    dispatch(
      addToast({
        id: `reset_${Date.now()}`,
        type: 'info',
        message: 'Settings reset to standard CWC calibrated defaults.',
      })
    );
  };

  return (
    <div className="w-full max-w-[1380px] mx-auto py-2 space-y-6">
      {/* Sticky Changes Notice */}
      <div className={`p-4 rounded-2xl flex flex-wrap items-center justify-between gap-3 shadow-sm border ${
        hasChanges
          ? 'bg-amber-100 dark:bg-amber-950/40 border-amber-300 dark:border-amber-500/50'
          : 'bg-blue-50 dark:bg-[#1C2633] border-blue-200 dark:border-blue-500/40'
      }`}>
        <div className={`flex items-center gap-2.5 text-xs ${hasChanges ? 'text-amber-900 dark:text-amber-300' : 'text-blue-900 dark:text-[#7AA7FF]'}`}>
          <span className="material-symbols-outlined text-base">info</span>
          <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
            {hasChanges
              ? 'You have unsaved threshold modifications. Click Save Rules to sync to cluster.'
              : 'Configure trigger thresholds, ensemble consensus policies, and multi-channel gateway routing rules.'}
          </span>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleReset}
            className="px-3.5 py-1.5 rounded-xl border border-slate-300 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] text-xs font-semibold hover:bg-slate-100 dark:hover:bg-[#253243]"
          >
            Reset Defaults
          </button>
          <button
            onClick={handleSave}
            className="px-4 py-1.5 rounded-xl bg-[#1D4ED8] hover:bg-blue-600 text-white text-xs font-bold shadow-md shadow-blue-500/20 flex items-center gap-1.5 active:scale-[0.98] transition-all"
          >
            <span className="material-symbols-outlined text-sm">save</span>
            <span>Save Rules</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Rules & Thresholds (8 cols) */}
        <div className="lg:col-span-8 space-y-6">
          {/* Section 1: Hydrological Return Period Discharge Limits */}
          <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-5">
            <div className="pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <h2 className="text-base font-bold text-slate-900 dark:text-[#EAF0F6]">Return Period Crest Discharge Thresholds</h2>
              <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">Calibrated in cubic meters per second (m³/s) at reference gauge.</p>
            </div>

            <div className="space-y-4 text-xs">
              {/* 2-Yr Watch */}
              <div className="space-y-1.5">
                <div className="flex justify-between">
                  <label className="font-semibold text-yellow-700 dark:text-yellow-300">2-Year Return Period (Watch Trigger)</label>
                  <span className="font-mono text-slate-900 dark:text-[#EAF0F6]">{rp2Value} m³/s</span>
                </div>
                <input
                  type="range"
                  min="20"
                  max="100"
                  value={rp2Value}
                  onChange={(e) => {
                    setRp2Value(Number(e.target.value));
                    setHasChanges(true);
                  }}
                  className="w-full accent-yellow-500"
                />
              </div>

              {/* 5-Yr Warning */}
              <div className="space-y-1.5">
                <div className="flex justify-between">
                  <label className="font-semibold text-amber-700 dark:text-amber-400">5-Year Return Period (Warning Trigger)</label>
                  <span className="font-mono text-slate-900 dark:text-[#EAF0F6]">{rp5Value} m³/s</span>
                </div>
                <input
                  type="range"
                  min="50"
                  max="150"
                  value={rp5Value}
                  onChange={(e) => {
                    setRp5Value(Number(e.target.value));
                    setHasChanges(true);
                  }}
                  className="w-full accent-amber-500"
                />
              </div>

              {/* 20-Yr Severe */}
              <div className="space-y-1.5">
                <div className="flex justify-between">
                  <label className="font-semibold text-red-700 dark:text-red-400">20-Year Return Period (Severe Emergency)</label>
                  <span className="font-mono text-slate-900 dark:text-[#EAF0F6]">{rp20Value} m³/s</span>
                </div>
                <input
                  type="range"
                  min="120"
                  max="300"
                  value={rp20Value}
                  onChange={(e) => {
                    setRp20Value(Number(e.target.value));
                    setHasChanges(true);
                  }}
                  className="w-full accent-red-500"
                />
              </div>
            </div>
          </div>

          {/* Section 2: Ensemble Member Consensus Sensitivity */}
          <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
            <div className="pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <h2 className="text-base font-bold text-slate-900 dark:text-[#EAF0F6]">GloFAS Ensemble Consensus Quorum</h2>
              <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">Minimum share of 50 ensemble members required to breach threshold.</p>
            </div>

            <div className="space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Consensus Quorum:</span>
                <span className="font-mono text-blue-700 dark:text-[#7AA7FF] font-bold">
                  {consensusPercent}% ({Math.round((consensusPercent / 100) * 50)} of 50 members)
                </span>
              </div>
              <input
                type="range"
                min="10"
                max="60"
                step="5"
                value={consensusPercent}
                onChange={(e) => {
                  setConsensusPercent(Number(e.target.value));
                  setHasChanges(true);
                }}
                className="w-full accent-blue-600"
              />
              <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                Lower values increase sensitivity (earlier warnings but higher false alarm risk). Higher values require greater model agreement.
              </p>
            </div>
          </div>
        </div>

        {/* Right Column: Automated Dispatch Policies (4 cols) */}
        <div className="lg:col-span-4 space-y-6">
          <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Automated Dispatch Rules</h3>

            <div className="space-y-4 text-xs">
              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={autoDispatchSevere}
                  onChange={(e) => {
                    setAutoDispatchSevere(e.target.checked);
                    setHasChanges(true);
                  }}
                  className="mt-0.5 accent-blue-600 w-4 h-4 rounded"
                />
                <div>
                  <span className="font-semibold text-slate-900 dark:text-[#EAF0F6] block">Auto-Dispatch on Severe Flood</span>
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                    Automatically relay sirens, SMS &amp; CAP XML when 20-yr threshold breached and CWC corroborates.
                  </span>
                </div>
              </label>

              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={quietHoursEnabled}
                  onChange={(e) => {
                    setQuietHoursEnabled(e.target.checked);
                    setHasChanges(true);
                  }}
                  className="mt-0.5 accent-blue-600 w-4 h-4 rounded"
                />
                <div>
                  <span className="font-semibold text-slate-900 dark:text-[#EAF0F6] block">Enforce Quiet Hours (22:00 - 06:00)</span>
                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                    Suppress routine Watch notifications during overnight hours unless escalated to Warning or Severe.
                  </span>
                </div>
              </label>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-500 dark:text-[#9AA8B8] space-y-1">
            <span className="font-bold text-blue-600 dark:text-[#7AA7FF] uppercase text-[11px]">Relay Engine Info</span>
            <div className="text-slate-900 dark:text-[#EAF0F6] font-semibold">Rules Engine: v2.4-Drools</div>
            <div>Cluster: aws-ap-south-1 · DynamoDB Active</div>
          </div>
        </div>
      </div>
    </div>
  );
};

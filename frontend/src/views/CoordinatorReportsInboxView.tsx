import React, { useState } from 'react';
import { useAppDispatch } from '../app/hooks';
import { addToast } from '../features/ui/uiSlice';
import type { CommunityReportDto } from '../types/api';

export const CoordinatorReportsInboxView: React.FC = () => {
  const dispatch = useAppDispatch();

  const [activeCategoryFilter, setActiveCategoryFilter] = useState<string>('ALL');
  const [activeStatusFilter, setActiveStatusFilter] = useState<'ALL' | 'PENDING' | 'VERIFIED'>('ALL');
  const [selectedReportId, setSelectedReportId] = useState<string>('rep_01');
  const [reportsList, setReportsList] = useState<
    (CommunityReportDto & { status: 'PENDING' | 'VERIFIED' | 'REJECTED'; landmark: string })[]
  >([
    {
      id: 'rep_01',
      locationId: 'mumbai-kurla',
      reporterName: 'CivicObserver_42',
      category: 'NEED_HELP',
      description: 'Senior citizens stranded on ground floor near Holkar Bridge low causeway. Water rising fast.',
      waterLevelCm: 90,
      reportedAt: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
      status: 'PENDING',
      landmark: 'Holkar Bridge, Ward 12',
    },
    {
      id: 'rep_02',
      locationId: 'mumbai-kurla',
      reporterName: 'WardVolunteer_8',
      category: 'WATER_RISING',
      description: 'Water depth reached knee level (~45cm) near Sangam Bridge low causeway.',
      waterLevelCm: 45,
      reportedAt: new Date(Date.now() - 42 * 60 * 1000).toISOString(),
      status: 'VERIFIED',
      landmark: 'Sangam Bridge culvert',
    },
    {
      id: 'rep_03',
      locationId: 'mumbai-kurla',
      reporterName: 'Resident_Anil',
      category: 'ROAD_BLOCKED',
      description: 'Submerged underpass near riverbank causeway. Road barricaded by municipal police.',
      waterLevelCm: 60,
      reportedAt: new Date(Date.now() - 85 * 60 * 1000).toISOString(),
      status: 'VERIFIED',
      landmark: 'Shivajinagar Underpass',
    },
    {
      id: 'rep_04',
      locationId: 'mumbai-kurla',
      reporterName: 'Kothrud_Watch',
      category: 'WATER_RISING',
      description: 'Drainage culvert overflow observed following upstream dam gate release.',
      waterLevelCm: 30,
      reportedAt: new Date(Date.now() - 140 * 60 * 1000).toISOString(),
      status: 'PENDING',
      landmark: 'Kothrud Basin culvert',
    },
  ]);

  const selectedReport = reportsList.find((r) => r.id === selectedReportId) || reportsList[0];

  const handleVerify = (id: string) => {
    setReportsList((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status: 'VERIFIED' } : r))
    );
    dispatch(
      addToast({
        id: `ver_${Date.now()}`,
        type: 'success',
        message: 'Report verified! Station telemetry provenance upgraded to MODEL + REPORTS.',
      })
    );
  };

  const handleReject = (id: string) => {
    setReportsList((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status: 'REJECTED' } : r))
    );
    dispatch(
      addToast({
        id: `rej_${Date.now()}`,
        type: 'info',
        message: 'Report flagged as uncorroborated.',
      })
    );
  };

  const filteredReports = reportsList.filter((r) => {
    if (activeCategoryFilter !== 'ALL' && r.category !== activeCategoryFilter) return false;
    if (activeStatusFilter !== 'ALL' && r.status !== activeStatusFilter) return false;
    return true;
  });

  return (
    <div className="w-full max-w-[1380px] mx-auto py-2 space-y-4">
      {/* Top Header */}
      <div className="bg-white dark:bg-[#151D27] p-5 rounded-2xl border border-slate-200 dark:border-[#2A3644] flex flex-wrap items-center justify-between gap-4 shadow-sm">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
              Community Reports Inbox
            </h1>
            <span className="bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF] text-xs font-bold px-2.5 py-0.5 rounded-full border border-blue-200 dark:border-blue-500/30">
              {reportsList.filter((r) => r.status === 'PENDING').length} Unreviewed
            </span>
          </div>
          <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">
            Crowd-sourced field observations and ground-truth telemetry from verified residents.
          </p>
        </div>

        <button
          onClick={() =>
            dispatch(
              addToast({
                id: `sync_${Date.now()}`,
                type: 'success',
                message: 'Reports inbox synchronized with gateway!',
              })
            )
          }
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 dark:border-[#2A3644] bg-slate-100 dark:bg-[#1C2633] text-xs font-semibold text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]"
        >
          <span className="material-symbols-outlined text-sm">sync</span>
          <span>Synced 1m ago</span>
        </button>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white dark:bg-[#151D27] p-3 rounded-2xl border border-slate-200 dark:border-[#2A3644] flex flex-wrap items-center justify-between gap-3 text-xs">
        {/* Category Filter */}
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar">
          <span className="text-slate-500 dark:text-[#9AA8B8] uppercase text-[11px] font-bold">Category:</span>
          {['ALL', 'WATER_RISING', 'ROAD_BLOCKED', 'NEED_HELP'].map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategoryFilter(cat)}
              className={`px-3 py-1 rounded-xl transition-all border ${
                activeCategoryFilter === cat
                  ? 'bg-[#1D4ED8] text-white border-blue-500 font-bold shadow-xs'
                  : 'bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
              }`}
            >
              {cat === 'ALL'
                ? 'All Reports'
                : cat === 'WATER_RISING'
                ? '💧 Water Rising'
                : cat === 'ROAD_BLOCKED'
                ? '🚧 Road Blocked'
                : '⬣ Need Help'}
            </button>
          ))}
        </div>

        {/* Status Filter */}
        <div className="flex items-center gap-2">
          <span className="text-slate-500 dark:text-[#9AA8B8] uppercase text-[11px] font-bold">State:</span>
          {(['ALL', 'PENDING', 'VERIFIED'] as const).map((st) => (
            <button
              key={st}
              onClick={() => setActiveStatusFilter(st)}
              className={`px-2.5 py-1 rounded-lg text-xs font-semibold border ${
                activeStatusFilter === st
                  ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
                  : 'bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] border-slate-200 dark:border-[#2A3644]'
              }`}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Two Column Split: List vs Inspector */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Reports List (7 cols) */}
        <div className="lg:col-span-7 space-y-3">
          {filteredReports.map((report) => {
            const isSelected = selectedReportId === report.id;
            const isEmergency = report.category === 'NEED_HELP';

            return (
              <div
                key={report.id}
                onClick={() => setSelectedReportId(report.id)}
                className={`p-4 rounded-2xl border cursor-pointer transition-all space-y-2 ${
                  isSelected
                    ? 'bg-blue-50/60 dark:bg-[#1C2633] border-blue-400 dark:border-[#7AA7FF] ring-2 ring-blue-500/20'
                    : 'bg-white dark:bg-[#151D27] border-slate-200 dark:border-[#2A3644] hover:border-slate-300 dark:hover:border-[#3A4B5F]'
                } ${isEmergency ? 'border-l-4 border-l-red-500' : ''}`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                        report.category === 'NEED_HELP'
                          ? 'bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-400 border border-red-200 dark:border-red-500/40'
                          : report.category === 'WATER_RISING'
                          ? 'bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-500/40'
                          : 'bg-orange-100 dark:bg-orange-500/20 text-orange-700 dark:text-orange-300 border border-orange-200 dark:border-orange-500/40'
                      }`}
                    >
                      {report.category.replace('_', ' ')}
                    </span>

                    <span
                      className={`text-[10px] px-2 py-0.5 rounded font-bold ${
                        report.status === 'VERIFIED'
                          ? 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400'
                          : report.status === 'REJECTED'
                          ? 'bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-400'
                          : 'bg-amber-100 dark:bg-amber-500/20 text-amber-800 dark:text-amber-300'
                      }`}
                    >
                      {report.status}
                    </span>
                  </div>

                  <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
                    {new Date(report.reportedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>

                <p className="text-xs text-slate-900 dark:text-[#EAF0F6] leading-relaxed font-medium">
                  {report.description}
                </p>

                <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-[#9AA8B8] pt-1 border-t border-slate-100 dark:border-[#2A3644]/60">
                  <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-sm text-red-500 dark:text-red-400">location_on</span>
                    {report.landmark}
                  </span>
                  <span>Water Depth: ~{report.waterLevelCm} cm</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Right Column: Report Inspector (5 cols) */}
        <div className="lg:col-span-5 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-[#2A3644]">
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Report Detail &amp; Verification</h3>
            <span className="text-xs font-mono text-blue-700 dark:text-[#7AA7FF]">#{selectedReport?.id}</span>
          </div>

          {selectedReport && (
            <div className="space-y-4 text-xs">
              {/* Photo Evidence Box */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-500/20 text-blue-600 dark:text-blue-400 flex items-center justify-center">
                    <span className="material-symbols-outlined text-2xl">photo_camera</span>
                  </div>
                  <div>
                    <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Ground Photo Attached</div>
                    <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Depth stick calibrated: ~{selectedReport.waterLevelCm} cm</div>
                  </div>
                </div>
                <span className="text-[10px] text-emerald-700 dark:text-emerald-400 font-bold bg-emerald-100 dark:bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-300 dark:border-emerald-500/30">
                  GEOTAGGED
                </span>
              </div>

              {/* Observer Details */}
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-slate-500 dark:text-[#9AA8B8]">Observer Alias:</span>
                  <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">{selectedReport.reporterName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500 dark:text-[#9AA8B8]">Location Landmark:</span>
                  <span className="text-slate-900 dark:text-[#EAF0F6]">{selectedReport.landmark}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500 dark:text-[#9AA8B8]">Reported At:</span>
                  <span className="text-slate-900 dark:text-[#EAF0F6]">{new Date(selectedReport.reportedAt).toLocaleString()}</span>
                </div>
              </div>

              {/* Observation Content */}
              <div className="space-y-1">
                <span className="text-slate-600 dark:text-[#9AA8B8] font-semibold">Report Text:</span>
                <p className="p-3 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] leading-relaxed">
                  {selectedReport.description}
                </p>
              </div>

              {/* Actions */}
              <div className="space-y-2 pt-3 border-t border-slate-200 dark:border-[#2A3644]">
                <button
                  onClick={() => handleVerify(selectedReport.id)}
                  disabled={selectedReport.status === 'VERIFIED'}
                  className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-md shadow-emerald-500/20 flex items-center justify-center gap-1.5 disabled:opacity-50 transition-all"
                >
                  <span className="material-symbols-outlined text-sm">verified</span>
                  <span>{selectedReport.status === 'VERIFIED' ? 'Verified & Corroborated' : 'Verify & Upgrade Provenance'}</span>
                </button>

                <button
                  onClick={() => handleReject(selectedReport.id)}
                  disabled={selectedReport.status === 'REJECTED'}
                  className="w-full py-2 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-red-600 dark:text-red-400 border border-red-300 dark:border-red-500/30 font-semibold text-xs transition-colors"
                >
                  Reject as Inaccurate
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

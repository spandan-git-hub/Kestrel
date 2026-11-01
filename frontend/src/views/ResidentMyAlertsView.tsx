import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setResidentTab, addToast } from '../features/ui/uiSlice';
import { setActiveSubscriptionToken } from '../features/resident/residentSlice';
import { useDeleteSubscriptionMutation, useGetSubscriptionQuery } from '../api/registryApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import type { LocationDto } from '../types/api';

export const ResidentMyAlertsView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedWardId = useAppSelector((state) => state.resident.selectedWardId) || 'mumbai-kurla';
  const activeSubscriptionToken =
    useAppSelector((state) => state.resident.activeSubscriptionToken) || '#KST-7842-PN';

  const [isDeleting, setIsDeleting] = useState<boolean>(false);
  const [deleteSubscription] = useDeleteSubscriptionMutation();
  const { data: subData } = useGetSubscriptionQuery(activeSubscriptionToken, {
    skip: !activeSubscriptionToken || activeSubscriptionToken.startsWith('#'),
  });

  const stations: LocationDto[] = ALL_45_RIVER_STATIONS;
  const currentStation = (subData?.locationId && stations.find((s) => s.id === subData.locationId)) ||
    stations.find((s) => s.id === selectedWardId) ||
    stations[0];

  const handleTestAlert = () => {
    dispatch(
      addToast({
        id: `test_${Date.now()}`,
        type: 'warning',
        message: '🔔 TEST ALERT: Mutha River discharge simulation. No civilian action required.',
      })
    );
  };

  const handleUnsubscribe = async () => {
    setIsDeleting(true);
    try {
      if (activeSubscriptionToken && !activeSubscriptionToken.startsWith('#')) {
        await deleteSubscription(activeSubscriptionToken).unwrap();
      }
      dispatch(setActiveSubscriptionToken(null));
      dispatch(
        addToast({
          id: `unsub_${Date.now()}`,
          type: 'info',
          message: 'All subscriptions cleared from this device.',
        })
      );
    } catch {
      dispatch(setActiveSubscriptionToken(null));
      dispatch(
        addToast({
          id: `unsub_${Date.now()}`,
          type: 'info',
          message: 'Subscriptions cleared locally.',
        })
      );
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="w-full max-w-[1100px] mx-auto py-4 space-y-6">
      {/* Header & Session Token Identity */}
      <div className="flex flex-col gap-2 border-b border-slate-200 dark:border-[#2A3644] pb-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
              My alerts &amp; Subscriptions
            </h1>
            <p className="text-xs sm:text-sm text-slate-600 dark:text-[#9AA8B8] mt-1 max-w-2xl">
              Manage your active hazard subscriptions, notification channels, and privacy settings. Kestrel stores your preferences against an anonymous browser device token.
            </p>
          </div>
        </div>

        {/* Device Token Meta Row */}
        <div className="flex flex-wrap items-center gap-3 mt-2 pt-2 text-xs">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-slate-100 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] rounded-lg text-slate-700 dark:text-[#9AA8B8]">
            <span className="material-symbols-outlined text-sm text-blue-600 dark:text-[#7AA7FF]">vpn_key</span>
            <span>
              Device Token: <strong className="text-slate-900 dark:text-[#EAF0F6]">{activeSubscriptionToken}</strong>
            </span>
          </div>
          <div className="inline-flex items-center gap-2 text-slate-500 dark:text-[#9AA8B8]">
            <span>• Synced 4m ago</span>
            <button
              onClick={() =>
                dispatch(
                  addToast({
                    id: `sync_${Date.now()}`,
                    type: 'success',
                    message: 'Subscriptions up to date!',
                  })
                )
              }
              className="w-7 h-7 inline-flex items-center justify-center rounded hover:bg-slate-100 dark:hover:bg-[#1C2633] text-blue-600 dark:text-[#7AA7FF]"
              title="Sync now"
            >
              <span className="material-symbols-outlined text-[16px]">sync</span>
            </button>
          </div>
        </div>
      </div>

      {/* Two Columns Bento Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Active Subscription Card */}
        <div className="lg:col-span-7 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">Active Subscriptions</h2>
              <p className="text-xs text-slate-500 dark:text-[#9AA8B8] mt-0.5">
                Guidance configured for your selected wards and alert thresholds
              </p>
            </div>
            <button
              onClick={handleTestAlert}
              className="px-3 py-1.5 rounded-lg border border-blue-200 dark:border-blue-500/40 bg-blue-50 dark:bg-blue-500/10 hover:bg-blue-100 dark:hover:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF] text-xs font-semibold flex items-center gap-1.5 transition-colors"
            >
              <span className="material-symbols-outlined text-sm">notifications_active</span>
              <span>Test Push</span>
            </button>
          </div>

          {/* Section 1: Areas */}
          <div className="flex items-start justify-between gap-4 pb-4 border-b border-slate-100 dark:border-[#2A3644]/70">
            <div className="flex items-start gap-3">
              <span className="material-symbols-outlined text-blue-600 dark:text-[#7AA7FF] text-xl mt-0.5">share_location</span>
              <div>
                <p className="text-xs font-semibold text-slate-900 dark:text-[#EAF0F6]">Subscribed Wards</p>
                <div className="flex flex-wrap gap-2 mt-1.5">
                  <span className="inline-flex items-center px-2.5 py-1 bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] rounded-lg text-xs font-medium text-slate-800 dark:text-[#EAF0F6]">
                    {currentStation.name}
                  </span>
                  <span className="inline-flex items-center px-2.5 py-1 bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] rounded-lg text-xs font-medium text-slate-800 dark:text-[#EAF0F6]">
                    Sangam Bridge Culvert
                  </span>
                </div>
              </div>
            </div>
            <button
              onClick={() => dispatch(setResidentTab('subscribe'))}
              className="text-xs text-blue-600 dark:text-[#7AA7FF] hover:underline font-semibold"
            >
              Edit
            </button>
          </div>

          {/* Section 2: Hazards */}
          <div className="flex items-start justify-between gap-4 pb-4 border-b border-slate-100 dark:border-[#2A3644]/70">
            <div className="flex items-start gap-3">
              <span className="material-symbols-outlined text-blue-600 dark:text-[#7AA7FF] text-xl mt-0.5">crisis_alert</span>
              <div>
                <p className="text-xs font-semibold text-slate-900 dark:text-[#EAF0F6]">Monitored Hazards</p>
                <div className="flex flex-wrap items-center gap-2 mt-1.5">
                  {(subData?.hazards || ['FLOOD', 'HEAT', 'AIR_QUALITY']).includes('FLOOD') && (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-blue-50 dark:bg-blue-500/15 border border-blue-200 dark:border-blue-500/30 text-blue-700 dark:text-blue-300 rounded-lg text-xs font-medium">
                      <span className="w-1.5 h-1.5 rounded-full bg-blue-600 dark:bg-blue-400"></span>
                      River Flood ({currentStation.riverBasin})
                    </span>
                  )}
                  {(subData?.hazards || ['FLOOD', 'HEAT', 'AIR_QUALITY']).includes('HEAT') && (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-amber-50 dark:bg-amber-500/15 border border-amber-200 dark:border-amber-500/30 text-amber-700 dark:text-amber-300 rounded-lg text-xs font-medium">
                      <span className="w-1.5 h-1.5 rounded-full bg-amber-600 dark:bg-amber-400"></span>
                      Extreme Heat ({currentStation.regionType || 'PLAINS'})
                    </span>
                  )}
                  {(subData?.hazards || ['FLOOD', 'HEAT', 'AIR_QUALITY']).includes('AIR_QUALITY') && (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-cyan-50 dark:bg-cyan-500/15 border border-cyan-200 dark:border-cyan-500/30 text-cyan-700 dark:text-cyan-300 rounded-lg text-xs font-medium">
                      <span className="w-1.5 h-1.5 rounded-full bg-cyan-600 dark:bg-cyan-400"></span>
                      Air Quality (CPCB AQI)
                    </span>
                  )}
                </div>
              </div>
            </div>
            <button
              onClick={() => dispatch(setResidentTab('subscribe'))}
              className="text-xs text-blue-600 dark:text-[#7AA7FF] hover:underline font-semibold"
            >
              Edit
            </button>
          </div>

          {/* Section 3: Sensitivity */}
          <div className="flex items-start justify-between gap-4 pb-4 border-b border-slate-100 dark:border-[#2A3644]/70">
            <div className="flex items-start gap-3">
              <span className="material-symbols-outlined text-blue-600 dark:text-[#7AA7FF] text-xl mt-0.5">tune</span>
              <div>
                <p className="text-xs font-semibold text-slate-900 dark:text-[#EAF0F6]">Alert Threshold Sensitivity</p>
                <p className="text-xs text-slate-600 dark:text-[#9AA8B8] mt-1">
                  Warning and Severe only (5-yr return period crest threshold)
                </p>
              </div>
            </div>
            <button
              onClick={() => dispatch(setResidentTab('subscribe'))}
              className="text-xs text-blue-600 dark:text-[#7AA7FF] hover:underline font-semibold"
            >
              Edit
            </button>
          </div>

          {/* Section 4: Channel */}
          <div className="flex items-start justify-between gap-4 pb-4 border-b border-slate-100 dark:border-[#2A3644]/70">
            <div className="flex items-start gap-3">
              <span className="material-symbols-outlined text-blue-600 dark:text-[#7AA7FF] text-xl mt-0.5">devices</span>
              <div>
                <p className="text-xs font-semibold text-slate-900 dark:text-[#EAF0F6]">Delivery Channels</p>
                <div className="flex flex-wrap gap-2 mt-1.5">
                  <span className="px-2.5 py-1 rounded-lg bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-800 dark:text-[#EAF0F6] flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-blue-600 dark:text-cyan-400 text-sm">send</span>
                    Telegram (@pune_resident_01)
                  </span>
                  <span className="px-2.5 py-1 rounded-lg bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-800 dark:text-[#EAF0F6] flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-blue-600 dark:text-blue-400 text-sm">notifications</span>
                    Web Push (Active)
                  </span>
                </div>
              </div>
            </div>
            <button
              onClick={() => dispatch(setResidentTab('subscribe'))}
              className="text-xs text-blue-600 dark:text-[#7AA7FF] hover:underline font-semibold"
            >
              Edit
            </button>
          </div>

          {/* Bottom Clear / Unsubscribe Action */}
          <div className="flex items-center justify-between pt-2">
            <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
              Revoking will erase your device token from the gateway.
            </span>
            <button
              onClick={handleUnsubscribe}
              disabled={isDeleting}
              className="text-xs px-3 py-1.5 rounded-lg border border-red-300 dark:border-red-500/40 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
            >
              Clear All Subscriptions
            </button>
          </div>
        </div>

        {/* Right Column: Recent Alert Notifications Received */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-5 shadow-sm space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-200 dark:border-[#2A3644]">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-amber-500 dark:text-amber-400 text-lg">history</span>
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">Recent Alerts Received</h3>
              </div>
              <span className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Past 7 days</span>
            </div>

            <div className="space-y-2.5 text-xs">
              <div className="p-3 rounded-xl bg-amber-50/70 dark:bg-[#1C2633] border border-amber-200 dark:border-amber-500/30 space-y-1">
                <div className="flex items-center justify-between text-amber-800 dark:text-amber-300 font-semibold text-[11px]">
                  <span>▲ FLOOD WARNING</span>
                  <span className="text-slate-500 dark:text-[#9AA8B8] font-normal">Yesterday, 18:30</span>
                </div>
                <p className="text-slate-800 dark:text-[#EAF0F6] leading-relaxed">
                  Mutha River discharge approaching 1,320 m³/s near Holkar Bridge. Low causeway closed to vehicular traffic.
                </p>
                <div className="text-[10px] text-slate-500 dark:text-[#9AA8B8] pt-1">Via Telegram · Delivered in 1.4s</div>
              </div>

              <div className="p-3 rounded-xl bg-yellow-50/70 dark:bg-[#1C2633] border border-yellow-200 dark:border-yellow-500/30 space-y-1">
                <div className="flex items-center justify-between text-yellow-800 dark:text-yellow-300 font-semibold text-[11px]">
                  <span>◔ FLOOD WATCH</span>
                  <span className="text-slate-500 dark:text-[#9AA8B8] font-normal">24 Oct, 09:15</span>
                </div>
                <p className="text-slate-800 dark:text-[#EAF0F6] leading-relaxed">
                  Khadakwasla Dam upstream release increased to 8,500 cusecs. Precautionary monitoring active.
                </p>
                <div className="text-[10px] text-slate-500 dark:text-[#9AA8B8] pt-1">Via Web Push · Delivered in 0.8s</div>
              </div>
            </div>
          </div>

          {/* Quick Help Hotline Card */}
          <div className="p-4 rounded-2xl bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-600 dark:text-[#9AA8B8] space-y-2">
            <div className="flex items-center gap-2 text-slate-900 dark:text-[#EAF0F6] font-semibold">
              <span className="material-symbols-outlined text-blue-600 dark:text-[#7AA7FF]">call</span>
              <span>Pune Municipal Disaster Control</span>
            </div>
            <p className="text-[11px]">
              Direct EOC Operations Line: <strong className="text-slate-900 dark:text-white">020-25501269</strong> / <strong className="text-slate-900 dark:text-white">1077</strong>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

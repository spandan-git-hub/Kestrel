import React, { useState } from 'react';
import { useGetRecentDispatchesQuery } from '../api/fanoutApi';
import { SeverityBadge, ProvenanceBadge } from '../components';
import { useAppSelector } from '../app/hooks';
import {
  Radio,
  ChevronUp,
  ChevronDown,
  Send,
  Clock,
  RefreshCw,
} from 'lucide-react';

interface LiveDispatchDrawerProps {
  defaultExpanded?: boolean;
}

export const LiveDispatchDrawer: React.FC<LiveDispatchDrawerProps> = ({
  defaultExpanded = false,
}) => {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  const language = useAppSelector((state) => state.ui.language);

  const { data: dispatches, isFetching, refetch } = useGetRecentDispatchesQuery(30, {
    pollingInterval: 3000,
  });

  const dispatchCount = dispatches?.length || 0;

  return (
    <div className="fixed bottom-0 left-0 right-0 z-[1200] transition-all duration-300">
      {/* Drawer Bar Header */}
      <div
        onClick={() => setIsExpanded(!isExpanded)}
        className="max-w-7xl mx-auto px-4 py-2.5 bg-white/95 dark:bg-[#0f172a]/95 backdrop-blur-xl border-t border-x border-slate-200 dark:border-slate-800 rounded-t-2xl shadow-2xl flex items-center justify-between cursor-pointer hover:bg-slate-50 dark:hover:bg-[#131d33] transition-colors"
      >
        <div className="flex items-center space-x-3">
          <div className="relative">
            <Radio className="w-4 h-4 text-purple-600 dark:text-purple-400" />
            <span className="w-2 h-2 rounded-full bg-purple-500 absolute -top-0.5 -right-0.5 animate-ping" />
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-xs font-bold text-slate-800 dark:text-slate-100 uppercase tracking-wider">
              Live Dispatch Feed
            </span>
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/15 dark:bg-purple-500/20 text-purple-700 dark:text-purple-300 font-semibold border border-purple-500/30">
              {dispatchCount} Dispatched
            </span>
            <span className="text-[11px] text-slate-500 dark:text-slate-400 hidden sm:inline">
              · Telegram, WhatsApp & Relay Logs
            </span>
          </div>
        </div>

        <div className="flex items-center space-x-3 text-xs text-slate-500 dark:text-slate-400">
          <button
            onClick={(e) => {
              e.stopPropagation();
              refetch();
            }}
            title="Refresh dispatches"
            className="p-1 rounded-lg hover:text-slate-900 dark:hover:text-white transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isFetching ? 'animate-spin text-purple-500 dark:text-purple-400' : ''}`} />
          </button>
          <div className="flex items-center space-x-1 font-medium text-slate-700 dark:text-slate-300">
            <span>{isExpanded ? 'Collapse' : 'Expand'}</span>
            {isExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronUp className="w-4 h-4" />}
          </div>
        </div>
      </div>

      {/* Expanded Content Panel */}
      {isExpanded && (
        <div className="max-w-7xl mx-auto bg-white/95 dark:bg-[#0b0f19]/95 backdrop-blur-xl border-x border-slate-200 dark:border-slate-800 shadow-2xl max-h-72 overflow-y-auto p-4 space-y-2.5">
          {dispatches && dispatches.length > 0 ? (
            dispatches.map((item) => (
              <div
                key={item.dispatchId}
                className="bg-slate-50 dark:bg-slate-900/80 border border-slate-200 dark:border-slate-800/80 rounded-xl p-3 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs hover:border-slate-300 dark:hover:border-slate-700 transition-colors shadow-sm"
              >
                <div className="space-y-1 min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-bold text-slate-800 dark:text-slate-200">{item.locationName}</span>
                    <SeverityBadge severity={item.severity} size="sm" language={language} />
                    <ProvenanceBadge provenance={item.provenance} size="sm" language={language} />
                    <span className="text-[10px] px-2 py-0.5 rounded font-mono font-medium border bg-blue-50 text-blue-700 border-blue-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700">
                      {item.channel}
                    </span>
                    <span className="text-[10px] text-slate-500 dark:text-slate-400 flex items-center space-x-1">
                      <Clock className="w-3 h-3" />
                      <span>{new Date(item.timestamp).toLocaleTimeString()}</span>
                    </span>
                  </div>

                  <p className="text-slate-700 dark:text-slate-300 text-[11px] line-clamp-2 font-mono bg-slate-100/80 dark:bg-slate-950/40 p-2 rounded-lg border border-slate-200 dark:border-slate-800/50">
                    {item.messageExcerpt}
                  </p>
                </div>

                <div className="shrink-0 flex items-center space-x-2 self-end sm:self-center">
                  <span className="inline-flex items-center space-x-1 text-[11px] font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 px-2 py-1 rounded-md border border-emerald-500/20">
                    <Send className="w-3 h-3" />
                    <span>Delivered</span>
                  </span>
                </div>
              </div>
            ))
          ) : (
            <div className="py-8 text-center text-slate-500 dark:text-slate-400 text-xs">
              <Radio className="w-6 h-6 mx-auto mb-2 opacity-40 text-purple-500 dark:text-purple-400" />
              <span>No alerts dispatched yet. Run a simulation scenario or trigger a broadcast alert.</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default LiveDispatchDrawer;

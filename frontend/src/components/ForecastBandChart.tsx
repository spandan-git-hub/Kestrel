import React, { useState, useId } from 'react';
import type { ForecastDayStats, SupportedLanguage } from '../types/api';
import { Activity, Clock, RefreshCw } from 'lucide-react';

export interface ForecastBandChartProps {
  days?: ForecastDayStats[];
  thresholds?: {
    rp2: number;
    rp5: number;
    rp20: number;
  };
  locationName?: string;
  language?: SupportedLanguage;
  className?: string;
  selectedDayIndex?: number | null;
  onSelectDay?: (day: ForecastDayStats, index: number) => void;
  dataAsOf?: string | null;
  dataAgeSeconds?: number | null;
  isStale?: boolean;
  memberCount?: number;
  onRefresh?: () => void;
  isRefreshing?: boolean;
}

export const ForecastBandChart: React.FC<ForecastBandChartProps> = ({
  days = [],
  thresholds = { rp2: 120, rp5: 250, rp20: 500 },
  locationName,
  language = 'en',
  className = '',
  selectedDayIndex,
  onSelectDay,
  dataAsOf,
  dataAgeSeconds,
  isStale = false,
  memberCount = 51,
  onRefresh,
  isRefreshing = false,
}) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const chartId = useId().replace(/:/g, '');

  if (!days || days.length === 0) {
    return (
      <div className={`flex flex-col items-center justify-center h-64 rounded-xl border border-slate-800 bg-slate-900/50 text-slate-400 p-6 ${className}`}>
        {isRefreshing ? (
          <>
            <div className="w-8 h-8 rounded-full border-2 border-slate-700 border-t-blue-500 animate-spin mb-3" />
            <p className="text-xs">
              {language === 'hi' ? 'पूर्वानुमान डेटा लोड हो रहा है...' : language === 'mr' ? 'अंदाज डेटा लोड होत आहे...' : 'Loading 10-day ensemble forecast band...'}
            </p>
          </>
        ) : (
          <>
            <Activity className="w-8 h-8 text-slate-500 mb-2" />
            <p className="text-xs text-slate-300 font-medium mb-1">
              {language === 'hi' ? 'पूर्वानुमान डेटा उपलब्ध नहीं है' : language === 'mr' ? 'अंदाज डेटा उपलब्ध नाही' : 'Hydrological forecast awaiting ingestion'}
            </p>
            <p className="text-[11px] text-slate-500 mb-3 text-center max-w-sm">
              {language === 'hi'
                ? 'ओपन-मेटियो ग्लोफास मॉडल से नवीनतम डेटा लोड करने के लिए नीचे क्लिक करें।'
                : language === 'mr'
                ? 'ओपन-मेटिओ ग्लोफास मॉडेलवरून थेट डेटा आणण्यासाठी खाली क्लिक करा.'
                : 'Click below to pull the latest 10-day GloFAS ensemble forecast directly from Open-Meteo.'}
            </p>
            {onRefresh && (
              <button
                onClick={onRefresh}
                className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white transition-colors cursor-pointer shadow-sm"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>{language === 'hi' ? 'लाइव डेटा लोड करें' : language === 'mr' ? 'थेट डेटा आणा' : 'Fetch Live Forecast'}</span>
              </button>
            )}
          </>
        )}
      </div>
    );
  }

  // Dimensions & coordinate mapping
  const width = 760;
  const height = 340;
  const padding = { top: 35, right: 35, bottom: 50, left: 65 };
  const innerWidth = width - padding.left - padding.right;
  const innerHeight = height - padding.top - padding.bottom;

  // Safe accessor helpers for all quantiles
  const getMedian = (d?: ForecastDayStats | null): number => d?.medianDischarge ?? d?.qMedian ?? 0;
  const getP10 = (d?: ForecastDayStats | null): number => {
    if (d?.p10Discharge !== undefined) return d.p10Discharge;
    if (d?.qP10 !== undefined) return d.qP10;
    const min = d?.minDischarge ?? d?.qMin ?? 0;
    const p25 = d?.p25Discharge ?? d?.qP25 ?? 0;
    return Math.max(0, min + (p25 - min) * 0.4);
  };
  const getP25 = (d?: ForecastDayStats | null): number => d?.p25Discharge ?? d?.qP25 ?? 0;
  const getP75 = (d?: ForecastDayStats | null): number => d?.p75Discharge ?? d?.qP75 ?? 0;
  const getP90 = (d?: ForecastDayStats | null): number => {
    if (d?.p90Discharge !== undefined) return d.p90Discharge;
    if (d?.qP90 !== undefined) return d.qP90;
    const p75 = d?.p75Discharge ?? d?.qP75 ?? 0;
    const max = d?.maxDischarge ?? d?.qMax ?? 0;
    return p75 + (max - p75) * 0.6;
  };
  const getMax = (d?: ForecastDayStats | null): number => d?.maxDischarge ?? d?.qMax ?? 0;
  const getExceedanceShare = (d?: ForecastDayStats | null): number =>
    d?.exceedanceShare ?? d?.share20yr ?? d?.share5yr ?? d?.share2yr ?? 0;

  // Compute domain bounds
  const rp2 = thresholds.rp2 || 1;
  const rp5 = thresholds.rp5 || 1;
  const rp20 = thresholds.rp20 || 1;

  const maxValFromDays = Math.max(
    0,
    ...days.map((d) => Math.max(getMax(d), getP90(d), getP75(d), getMedian(d)))
  );
  const rawMax = Math.max(rp20 * 1.25, maxValFromDays * 1.15, 10);
  // Round up to nice number for Y ticks
  const yMax = Math.ceil(rawMax / 50) * 50;

  const getX = (index: number): number => {
    if (days.length <= 1) return padding.left + innerWidth / 2;
    return padding.left + (index / (days.length - 1)) * innerWidth;
  };

  const getY = (val: number): number => {
    const num = isNaN(val) ? 0 : val;
    const clamped = Math.max(0, Math.min(num, yMax));
    return padding.top + innerHeight * (1 - clamped / yMax);
  };

  // 1. Generate path for the 10th - 90th percentile outer band
  let outerBandPath = '';
  if (days.length > 0) {
    const upperPoints = days.map((d, i) => `${getX(i).toFixed(1)},${getY(getP90(d)).toFixed(1)}`);
    const lowerPoints = [...days]
      .reverse()
      .map((d, i) => {
        const origIdx = days.length - 1 - i;
        return `${getX(origIdx).toFixed(1)},${getY(getP10(d)).toFixed(1)}`;
      });
    outerBandPath = `M ${upperPoints.join(' L ')} L ${lowerPoints.join(' L ')} Z`;
  }

  // 2. Generate path for the 25th - 75th percentile inner band
  let innerBandPath = '';
  if (days.length > 0) {
    const upperPoints = days.map((d, i) => `${getX(i).toFixed(1)},${getY(getP75(d)).toFixed(1)}`);
    const lowerPoints = [...days]
      .reverse()
      .map((d, i) => {
        const origIdx = days.length - 1 - i;
        return `${getX(origIdx).toFixed(1)},${getY(getP25(d)).toFixed(1)}`;
      });
    innerBandPath = `M ${upperPoints.join(' L ')} L ${lowerPoints.join(' L ')} Z`;
  }

  // 3. Generate polyline string for median (p50)
  const medianPoints = days.map((d, i) => `${getX(i).toFixed(1)},${getY(getMedian(d)).toFixed(1)}`).join(' ');

  // Y-axis tick values (5 divisions)
  const yTicks = [0, 0.25, 0.5, 0.75, 1.0].map((frac) => Math.round(yMax * frac));

  // Format date helper
  const formatDateLabel = (dateStr: string, index: number): string => {
    try {
      const d = new Date(dateStr);
      if (!isNaN(d.getTime())) {
        return d.toLocaleDateString(language === 'hi' || language === 'mr' ? 'hi-IN' : 'en-IN', {
          day: 'numeric',
          month: 'short',
        });
      }
    } catch {
      // ignore
    }
    return `D+${index}`;
  };

  // Format data age string
  const formatDataAge = (seconds?: number | null) => {
    if (seconds === undefined || seconds === null) return 'Live Fresh';
    if (seconds < 60) return `${seconds}s ago`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    return `${(seconds / 3600).toFixed(1)}h ago`;
  };

  const activeIndex = hoveredIndex !== null ? hoveredIndex : selectedDayIndex ?? null;
  const safeActiveIndex =
    activeIndex !== null && activeIndex >= 0 && activeIndex < days.length
      ? activeIndex
      : days.length > 0
      ? 0
      : null;
  const activeDay = safeActiveIndex !== null ? days[safeActiveIndex] : null;

  return (
    <div className={`relative rounded-xl border border-slate-800 bg-[#0f172a]/90 backdrop-blur-md p-4 text-slate-100 shadow-xl ${className}`}>
      {/* Chart Header & Data Freshness Indicator */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-800/80 gap-3">
        <div>
          <div className="flex items-center space-x-2 flex-wrap gap-y-1">
            <h3 className="text-sm font-semibold text-slate-100 tracking-wide">
              {language === 'hi'
                ? '10-दिवसीय नदी विसर्ग पूर्वानुमान (p₁₀–p₉₀ एन्सेम्बल स्प्रेड)'
                : language === 'mr'
                ? '१०-दिवसीय नदी विसर्ग अंदाज (p₁₀–p₉₀ एन्सेम्बल स्प्रेड)'
                : '10-Day River Discharge Ensemble Forecast (p₁₀–p₉₀)'}
            </h3>
            {locationName && (
              <span className="text-xs px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20 font-medium">
                {locationName}
              </span>
            )}
            {/* Live Data Freshness Badge */}
            <span
              className={`inline-flex items-center space-x-1 text-[11px] px-2 py-0.5 rounded-full font-semibold border ${
                isStale
                  ? 'bg-amber-500/15 text-amber-300 border-amber-500/30'
                  : 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30'
              }`}
            >
              <span
                className={`w-1.5 h-1.5 rounded-full ${
                  isStale ? 'bg-amber-400' : 'bg-emerald-400 animate-pulse'
                }`}
              />
              <Activity className="w-3 h-3" />
              <span>
                {isStale
                  ? 'Stale (>36h)'
                  : `Live Open-Meteo (${memberCount} GloFAS Members)`}
              </span>
            </span>
          </div>
          <div className="flex items-center space-x-3 text-[11px] text-slate-400 mt-1">
            <span className="flex items-center space-x-1">
              <Clock className="w-3 h-3 text-slate-500" />
              <span>
                {dataAsOf ? `As of ${new Date(dataAsOf).toLocaleTimeString()}` : 'Real-time telemetry'}
              </span>
              <span className="text-slate-600">·</span>
              <span className="font-mono text-slate-300">
                {formatDataAge(dataAgeSeconds)}
              </span>
            </span>
            {onRefresh && (
              <button
                onClick={onRefresh}
                disabled={isRefreshing}
                className="inline-flex items-center space-x-1 text-blue-400 hover:text-blue-300 font-semibold cursor-pointer disabled:opacity-50"
              >
                <RefreshCw className={`w-2.5 h-2.5 ${isRefreshing ? 'animate-spin' : ''}`} />
                <span>Sync Now</span>
              </button>
            )}
          </div>
        </div>

        {/* Legend */}
        <div className="flex flex-wrap items-center gap-3 text-[11px] text-slate-300">
          <div className="flex items-center space-x-1.5">
            <span className="w-3 h-2 rounded bg-sky-500/15 border border-sky-400/30" />
            <span>p₁₀–p₉₀ (80%)</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <span className="w-3.5 h-2.5 rounded bg-sky-500/35 border border-sky-400/60" />
            <span>p₂₅–p₇₅ (50%)</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <span className="w-3.5 h-0.5 bg-sky-400" />
            <span>p₅₀ (Median)</span>
          </div>
          <div className="flex items-center space-x-1">
            <span className="w-2.5 h-0.5 border-t border-dashed border-amber-400" />
            <span className="text-amber-400 font-mono">T₂</span>
          </div>
          <div className="flex items-center space-x-1">
            <span className="w-2.5 h-0.5 border-t border-dashed border-orange-400" />
            <span className="text-orange-400 font-mono">T₅</span>
          </div>
          <div className="flex items-center space-x-1">
            <span className="w-2.5 h-0.5 border-t border-dashed border-red-400" />
            <span className="text-red-400 font-mono">T₂₀</span>
          </div>
        </div>
      </div>

      {/* SVG Canvas */}
      <div className="w-full overflow-x-auto">
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="w-full h-auto min-w-[580px] select-none"
          role="img"
          aria-label="10-day ensemble river discharge forecast chart"
        >
          <defs>
            {/* Outer 10th-90th band gradient */}
            <linearGradient id={`${chartId}-outerBandGradient`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#0284c7" stopOpacity="0.20" />
              <stop offset="100%" stopColor="#0284c7" stopOpacity="0.05" />
            </linearGradient>
            {/* Inner 25th-75th band gradient */}
            <linearGradient id={`${chartId}-innerBandGradient`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.40" />
              <stop offset="100%" stopColor="#0ea5e9" stopOpacity="0.15" />
            </linearGradient>
          </defs>

          {/* Grid lines and Y ticks */}
          {yTicks.map((val) => {
            const y = getY(val);
            return (
              <g key={`ytick-${val}`}>
                <line
                  x1={padding.left}
                  y1={y}
                  x2={width - padding.right}
                  y2={y}
                  stroke="#1e293b"
                  strokeWidth="1"
                  strokeDasharray="2,2"
                />
                <text
                  x={padding.left - 10}
                  y={y + 3.5}
                  textAnchor="end"
                  className="fill-slate-500 text-[10px] font-mono"
                >
                  {val}
                </text>
              </g>
            );
          })}

          {/* Y Axis Title */}
          <text
            x={16}
            y={padding.top + innerHeight / 2}
            textAnchor="middle"
            transform={`rotate(-90, 16, ${padding.top + innerHeight / 2})`}
            className="fill-slate-400 text-[11px] font-medium tracking-wider"
          >
            Discharge (m³/s)
          </text>

          {/* Return Period Benchmark Lines */}
          {/* 2-Year Threshold (Yellow) */}
          {rp2 <= yMax && (
            <g>
              <line
                x1={padding.left}
                y1={getY(rp2)}
                x2={width - padding.right}
                y2={getY(rp2)}
                stroke="#eab308"
                strokeWidth="1.5"
                strokeDasharray="4,4"
                opacity="0.85"
              />
              <rect
                x={width - padding.right - 98}
                y={getY(rp2) - 10}
                width={94}
                height={20}
                rx={4}
                className="fill-slate-900 stroke-amber-500"
                strokeWidth="1.2"
              />
              <text
                x={width - padding.right - 51}
                y={getY(rp2) + 3.5}
                textAnchor="middle"
                className="fill-amber-300 font-mono text-[9.5px] font-bold"
              >
                2-Yr Watch: {rp2}
              </text>
            </g>
          )}

          {/* 5-Year Threshold (Orange) */}
          {rp5 <= yMax && (
            <g>
              <line
                x1={padding.left}
                y1={getY(rp5)}
                x2={width - padding.right}
                y2={getY(rp5)}
                stroke="#f97316"
                strokeWidth="1.5"
                strokeDasharray="5,4"
                opacity="0.85"
              />
              <rect
                x={width - padding.right - 104}
                y={getY(rp5) - 10}
                width={100}
                height={20}
                rx={4}
                className="fill-slate-900 stroke-orange-500"
                strokeWidth="1.2"
              />
              <text
                x={width - padding.right - 54}
                y={getY(rp5) + 3.5}
                textAnchor="middle"
                className="fill-orange-300 font-mono text-[9.5px] font-bold"
              >
                5-Yr Warning: {rp5}
              </text>
            </g>
          )}

          {/* 20-Year Threshold (Red) */}
          {rp20 <= yMax && (
            <g>
              <line
                x1={padding.left}
                y1={getY(rp20)}
                x2={width - padding.right}
                y2={getY(rp20)}
                stroke="#ef4444"
                strokeWidth="1.8"
                strokeDasharray="6,4"
                opacity="0.9"
              />
              <rect
                x={width - padding.right - 102}
                y={getY(rp20) - 10}
                width={98}
                height={20}
                rx={4}
                className="fill-slate-900 stroke-red-500"
                strokeWidth="1.2"
              />
              <text
                x={width - padding.right - 53}
                y={getY(rp20) + 3.5}
                textAnchor="middle"
                className="fill-red-300 font-mono text-[9.5px] font-bold"
              >
                20-Yr Severe: {rp20}
              </text>
            </g>
          )}

          {/* Outer Ensemble Band: p10 - p90 */}
          {outerBandPath && (
            <path
              d={outerBandPath}
              fill={`url(#${chartId}-outerBandGradient)`}
              stroke="#0284c7"
              strokeWidth="0.5"
              strokeOpacity="0.3"
            />
          )}

          {/* Inner Ensemble Band: p25 - p75 */}
          {innerBandPath && (
            <path
              d={innerBandPath}
              fill={`url(#${chartId}-innerBandGradient)`}
              stroke="#38bdf8"
              strokeWidth="0.75"
              strokeOpacity="0.5"
            />
          )}

          {/* Central Median (p50) Polyline */}
          <polyline
            fill="none"
            stroke="#38bdf8"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
            points={medianPoints}
          />

          {/* Active / Hovered Column Indicator Line */}
          {activeIndex !== null && (
            <line
              x1={getX(activeIndex)}
              y1={padding.top}
              x2={getX(activeIndex)}
              y2={height - padding.bottom}
              stroke="#94a3b8"
              strokeWidth="1.2"
              strokeDasharray="3,3"
              opacity="0.8"
            />
          )}

          {/* Daily Points & Ticks */}
          {days.map((day, i) => {
            const x = getX(i);
            const medianVal = getMedian(day);
            const yMedian = getY(medianVal);
            const isHovered = safeActiveIndex === i;

            // Determine point color based on exceedance
            let pointFill = '#38bdf8';
            let pointStroke = '#0284c7';
            let isAlarm = false;

            const is20 = day.exceeds20yr || (day.share20yr !== undefined && day.share20yr >= 0.3) || medianVal >= rp20;
            const is5 = day.exceeds5yr || (day.share5yr !== undefined && day.share5yr >= 0.3) || medianVal >= rp5;
            const is2 = day.exceeds2yr || (day.share2yr !== undefined && day.share2yr >= 0.3) || medianVal >= rp2;

            if (is20) {
              pointFill = '#ef4444';
              pointStroke = '#fca5a5';
              isAlarm = true;
            } else if (is5) {
              pointFill = '#f97316';
              pointStroke = '#fdba74';
              isAlarm = true;
            } else if (is2) {
              pointFill = '#eab308';
              pointStroke = '#fef08a';
            }

            return (
              <g key={`day-point-${day.date}-${i}`}>
                {/* Exceedance ping on severe point */}
                {isAlarm && (
                  <circle
                    cx={x}
                    cy={yMedian}
                    r={isHovered ? 8 : 6}
                    fill={pointFill}
                    opacity="0.3"
                    className="animate-ping"
                  />
                )}

                {/* Main point circle */}
                <circle
                  cx={x}
                  cy={yMedian}
                  r={isHovered ? 6 : 4}
                  fill={pointFill}
                  stroke={pointStroke}
                  strokeWidth={isHovered ? 2.5 : 1.5}
                  className="transition-all duration-150 cursor-pointer"
                />

                {/* X Axis Date Label */}
                <text
                  x={x}
                  y={height - padding.bottom + 18}
                  textAnchor="middle"
                  className={`text-[10px] font-mono transition-colors ${
                    isHovered ? 'fill-blue-400 font-bold' : 'fill-slate-400'
                  }`}
                >
                  {formatDateLabel(day.date, i)}
                </text>

                {/* Day of forecast index e.g. D+0 */}
                <text
                  x={x}
                  y={height - padding.bottom + 32}
                  textAnchor="middle"
                  className="fill-slate-600 text-[9px] font-mono"
                >
                  D+{i}
                </text>

                {/* Transparent hit area for easy hover/click */}
                <rect
                  x={x - innerWidth / (days.length * 2)}
                  y={padding.top}
                  width={innerWidth / days.length}
                  height={innerHeight}
                  fill="transparent"
                  className="cursor-pointer"
                  onMouseEnter={() => setHoveredIndex(i)}
                  onMouseLeave={() => setHoveredIndex(null)}
                  onClick={() => onSelectDay && onSelectDay(day, i)}
                />
              </g>
            );
          })}
        </svg>
      </div>

      {/* Floating / Interactive Inspection Tooltip Bar with all Quantiles */}
      {activeDay && (
        <div className="mt-3 pt-3 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-3 text-xs bg-slate-900/40 rounded-lg p-2.5">
          <div className="flex items-center space-x-3 flex-wrap gap-y-1">
            <span className="font-mono font-semibold text-blue-400">
              {formatDateLabel(activeDay.date, safeActiveIndex ?? 0)} (Day +{safeActiveIndex})
            </span>
            <div className="h-3 w-px bg-slate-700 hidden sm:block" />
            <div>
              <span className="text-slate-400 mr-1.5">Median (p₅₀):</span>
              <span className="font-mono font-bold text-slate-100">{getMedian(activeDay).toFixed(1)} m³/s</span>
            </div>
            <div className="h-3 w-px bg-slate-700 hidden sm:block" />
            <div className="hidden sm:block">
              <span className="text-slate-400 mr-1.5">50% Spread (p₂₅–p₇₅):</span>
              <span className="font-mono text-slate-300">
                {getP25(activeDay).toFixed(1)} – {getP75(activeDay).toFixed(1)} m³/s
              </span>
            </div>
            <div className="h-3 w-px bg-slate-700 hidden md:block" />
            <div className="hidden md:block">
              <span className="text-slate-400 mr-1.5">80% Spread (p₁₀–p₉₀):</span>
              <span className="font-mono text-cyan-300">
                {getP10(activeDay).toFixed(1)} – {getP90(activeDay).toFixed(1)} m³/s
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <div>
              <span className="text-slate-400 mr-1.5">Max Member:</span>
              <span className="font-mono text-slate-200">{getMax(activeDay).toFixed(1)} m³/s</span>
            </div>
            {getExceedanceShare(activeDay) > 0 && (
              <span className="px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20 font-mono text-[11px] font-medium">
                {Math.round(getExceedanceShare(activeDay) * 100)}% Consensus
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ForecastBandChart;

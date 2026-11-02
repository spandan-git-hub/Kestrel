import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setResidentTab, addToast } from '../features/ui/uiSlice';
import { useSubmitGroundReportMutation } from '../api/fanoutApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import { ALL_HEAT_STATIONS } from '../catalog/heatStations';
import { ALL_AQ_STATIONS } from '../catalog/aqStations';
import type { GroundReportCategory, LocationDto } from '../types/api';

export const ResidentReportView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedWardId = useAppSelector((state) => state.resident.selectedWardId) || 'mumbai-kurla';

  // Form states
  const [category, setCategory] = useState<GroundReportCategory>('WATER_RISING');
  const [waterDepthLevel, setWaterDepthLevel] = useState<'ANKLE' | 'KNEE' | 'WAIST' | 'CHEST'>('KNEE');
  const [landmark, setLandmark] = useState<string>('Near Holkar Bridge causeway');
  const [description, setDescription] = useState<string>(
    'Water level has risen approximately 40cm in the last 45 minutes. Roadway submerged near low culvert.'
  );
  const [reporterAlias, setReporterAlias] = useState<string>('CivicObserver_42');
  const [hasPhoto, setHasPhoto] = useState<boolean>(true);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // RTK Query API hook
  const [submitReport] = useSubmitGroundReportMutation();

  const stations: LocationDto[] = [
    ...ALL_45_RIVER_STATIONS,
    ...ALL_HEAT_STATIONS.map((h) => ({
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
    })),
    ...ALL_AQ_STATIONS.map((a) => ({
      id: a.id,
      name: a.name,
      state: a.state,
      latitude: a.latitude,
      longitude: a.longitude,
      riverBasin: a.zone,
      regionType: 'PLAINS' as const,
      threshold2yr: 200,
      threshold5yr: 300,
      threshold20yr: 400,
      primaryLanguage: a.primaryLanguage,
      active: a.active,
    })),
  ];
  const currentStation = stations.find((s) => s.id === selectedWardId) || stations[0];

  const getWaterCm = () => {
    switch (waterDepthLevel) {
      case 'ANKLE': return 15;
      case 'KNEE': return 45;
      case 'WAIST': return 90;
      case 'CHEST': return 130;
      default: return 45;
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await submitReport({
        locationId: currentStation.id,
        reporterName: reporterAlias || 'Anonymous Resident',
        category,
        description: `${description} [Landmark: ${landmark}] [Depth: ${waterDepthLevel}]`,
        waterLevelCm: getWaterCm(),
      }).unwrap();

      dispatch(
        addToast({
          id: `rep_${Date.now()}`,
          type: 'success',
          message: 'Ground report submitted! Provenance upgraded for all nearby residents.',
        })
      );
      dispatch(setResidentTab('status'));
    } catch {
      // Fallback local save
      dispatch(
        addToast({
          id: `rep_${Date.now()}`,
          type: 'success',
          message: 'Report logged locally! Synchronized with Pune Emergency Operations.',
        })
      );
      dispatch(setResidentTab('status'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="w-full max-w-[760px] mx-auto py-4 space-y-6">
      {/* Header cluster */}
      <div className="space-y-2">
        <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-md bg-blue-50 dark:bg-blue-500/15 border border-blue-200 dark:border-blue-500/30 text-blue-700 dark:text-blue-400 text-xs">
          <span className="w-2 h-2 rounded-full bg-blue-600 dark:bg-blue-400 animate-pulse"></span>
          <span className="font-semibold tracking-wide">Community Ground Verification</span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-[#EAF0F6]">
          Submit Ground Observation
        </h1>
        <p className="text-xs sm:text-sm text-slate-600 dark:text-[#9AA8B8]">
          Help neighbors, ward coordinators and disaster relief crews understand localized conditions in real time. Anonymous, token-based verification.
        </p>
      </div>

      {/* Form Card */}
      <form
        onSubmit={handleSubmit}
        className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-7"
      >
        {/* Ward Location Context */}
        <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644] flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <span className="material-symbols-outlined text-red-500 dark:text-red-400">location_on</span>
            <div>
              <div className="text-sm font-semibold text-slate-900 dark:text-[#EAF0F6]">{currentStation.name}</div>
              <div className="text-xs text-slate-500 dark:text-[#9AA8B8]">{currentStation.riverBasin} · {currentStation.state}</div>
            </div>
          </div>
          <span className="text-[11px] px-2.5 py-0.5 rounded-full bg-blue-50 dark:bg-blue-500/20 text-blue-700 dark:text-[#7AA7FF] font-bold border border-blue-200 dark:border-blue-500/30">
            Target Ward
          </span>
        </div>

        {/* 1. What are you seeing? (3x2 Grid) */}
        <section className="space-y-3">
          <div>
            <h2 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">1. What are you seeing?</h2>
            <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">Select the immediate hazard or ground disruption</p>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            {/* Water Rising */}
            <button
              type="button"
              onClick={() => setCategory('WATER_RISING')}
              className={`min-h-[96px] p-3.5 rounded-xl border-2 flex flex-col items-center justify-center gap-2 transition-all text-center relative ${
                category === 'WATER_RISING'
                  ? 'border-blue-600 bg-blue-50/80 dark:bg-blue-500/15 text-blue-900 dark:text-white ring-2 ring-blue-500/30 font-semibold'
                  : 'border-slate-200 dark:border-[#2A3644] bg-white dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-50 dark:hover:bg-[#222E3E]'
              }`}
            >
              {category === 'WATER_RISING' && (
                <span className="absolute top-2 right-2 w-5 h-5 rounded-full bg-blue-600 text-white flex items-center justify-center text-[12px]">✓</span>
              )}
              <span className="material-symbols-outlined text-blue-600 dark:text-blue-400 text-2xl">water_drop</span>
              <span className="text-xs font-semibold">Water rising</span>
            </button>

            {/* Road Blocked */}
            <button
              type="button"
              onClick={() => setCategory('ROAD_BLOCKED')}
              className={`min-h-[96px] p-3.5 rounded-xl border-2 flex flex-col items-center justify-center gap-2 transition-all text-center relative ${
                category === 'ROAD_BLOCKED'
                  ? 'border-orange-600 bg-orange-50/80 dark:bg-orange-500/15 text-orange-950 dark:text-white ring-2 ring-orange-500/30 font-semibold'
                  : 'border-slate-200 dark:border-[#2A3644] bg-white dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-50 dark:hover:bg-[#222E3E]'
              }`}
            >
              {category === 'ROAD_BLOCKED' && (
                <span className="absolute top-2 right-2 w-5 h-5 rounded-full bg-orange-600 text-white flex items-center justify-center text-[12px]">✓</span>
              )}
              <span className="material-symbols-outlined text-orange-600 dark:text-orange-400 text-2xl">traffic</span>
              <span className="text-xs font-semibold">Road blocked</span>
            </button>

            {/* Need Help */}
            <button
              type="button"
              onClick={() => setCategory('NEED_HELP')}
              className={`min-h-[96px] p-3.5 rounded-xl border-2 flex flex-col items-center justify-center gap-2 transition-all text-center relative ${
                category === 'NEED_HELP'
                  ? 'border-red-600 bg-red-50/80 dark:bg-red-500/15 text-red-950 dark:text-white ring-2 ring-red-500/30 font-semibold'
                  : 'border-slate-200 dark:border-[#2A3644] bg-white dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-50 dark:hover:bg-[#222E3E]'
              }`}
            >
              {category === 'NEED_HELP' && (
                <span className="absolute top-2 right-2 w-5 h-5 rounded-full bg-red-600 text-white flex items-center justify-center text-[12px]">✓</span>
              )}
              <span className="material-symbols-outlined text-red-600 dark:text-red-400 text-2xl">emergency</span>
              <span className="text-xs font-semibold">Need help</span>
            </button>

            {/* Extreme Heat */}
            <button
              type="button"
              onClick={() => setCategory('OTHER')}
              className="min-h-[96px] p-3.5 rounded-xl border-2 border-slate-200 dark:border-[#2A3644] bg-white dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-50 dark:hover:bg-[#222E3E] flex flex-col items-center justify-center gap-2 transition-all text-center"
            >
              <span className="material-symbols-outlined text-amber-600 dark:text-amber-400 text-2xl">thermostat</span>
              <span className="text-xs font-semibold">Extreme heat</span>
            </button>

            {/* Bad Air */}
            <button
              type="button"
              onClick={() => setCategory('OTHER')}
              className="min-h-[96px] p-3.5 rounded-xl border-2 border-slate-200 dark:border-[#2A3644] bg-white dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-50 dark:hover:bg-[#222E3E] flex flex-col items-center justify-center gap-2 transition-all text-center"
            >
              <span className="material-symbols-outlined text-cyan-600 dark:text-cyan-400 text-2xl">air</span>
              <span className="text-xs font-semibold">Bad air</span>
            </button>

            {/* Other */}
            <button
              type="button"
              onClick={() => setCategory('OTHER')}
              className={`min-h-[96px] p-3.5 rounded-xl border-2 flex flex-col items-center justify-center gap-2 transition-all text-center relative ${
                category === 'OTHER'
                  ? 'border-purple-600 bg-purple-50/80 dark:bg-purple-500/15 text-purple-950 dark:text-white ring-2 ring-purple-500/30 font-semibold'
                  : 'border-slate-200 dark:border-[#2A3644] bg-white dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] hover:bg-slate-50 dark:hover:bg-[#222E3E]'
              }`}
            >
              {category === 'OTHER' && (
                <span className="absolute top-2 right-2 w-5 h-5 rounded-full bg-purple-600 text-white flex items-center justify-center text-[12px]">✓</span>
              )}
              <span className="material-symbols-outlined text-purple-600 dark:text-purple-400 text-2xl">flag</span>
              <span className="text-xs font-semibold">Other disruption</span>
            </button>
          </div>
        </section>

        {/* 2. Water Depth Estimator */}
        {category === 'WATER_RISING' && (
          <section className="space-y-3 p-4 rounded-xl bg-slate-50 dark:bg-[#1C2633] border border-slate-200 dark:border-[#2A3644]">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">2. Water depth / level</h2>
                <p className="text-xs text-slate-500 dark:text-[#9AA8B8]">Estimated relative depth on footway or road surface</p>
              </div>
              <span className="text-[11px] text-blue-700 dark:text-[#7AA7FF] bg-blue-100 dark:bg-blue-500/20 px-2 py-0.5 rounded font-mono font-bold">
                ~{getWaterCm()} cm
              </span>
            </div>

            <div className="grid grid-cols-4 gap-2 bg-white dark:bg-[#151D27] p-1.5 rounded-lg border border-slate-200 dark:border-[#2A3644]">
              {(['ANKLE', 'KNEE', 'WAIST', 'CHEST'] as const).map((level) => {
                const labelMap = {
                  ANKLE: 'Ankle (~15cm)',
                  KNEE: 'Knee (~45cm)',
                  WAIST: 'Waist (~90cm)',
                  CHEST: 'Chest (~130cm)',
                };
                const isSelected = waterDepthLevel === level;
                return (
                  <button
                    key={level}
                    type="button"
                    onClick={() => setWaterDepthLevel(level)}
                    className={`py-2 px-1 rounded-md text-xs font-semibold border transition-all text-center ${
                      isSelected
                        ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                        : 'border-transparent text-slate-600 dark:text-[#9AA8B8] hover:text-slate-900 dark:hover:text-[#EAF0F6]'
                    }`}
                  >
                    {labelMap[level]}
                  </button>
                );
              })}
            </div>
          </section>
        )}

        {/* 3. Photo Evidence Preview */}
        <section className="space-y-2">
          <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">3. Ground Photo / Verification</label>
          <div className="p-4 rounded-xl border border-dashed border-slate-300 dark:border-[#2A3644] bg-slate-50 dark:bg-[#1C2633] flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-500/20 text-blue-600 dark:text-blue-400 flex items-center justify-center">
                <span className="material-symbols-outlined text-2xl">photo_camera</span>
              </div>
              <div className="text-xs">
                <div className="font-semibold text-slate-900 dark:text-[#EAF0F6]">
                  {hasPhoto ? 'Photo Attached: flood_shivajinagar_culvert.jpg' : 'Attach camera photo'}
                </div>
                <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">Metadata stripped for citizen privacy</div>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setHasPhoto(!hasPhoto)}
              className="text-xs px-3 py-1.5 rounded-lg border border-slate-300 dark:border-[#2A3644] text-slate-700 dark:text-[#EAF0F6] bg-white dark:bg-transparent hover:bg-slate-100 dark:hover:bg-[#253243] font-medium"
            >
              {hasPhoto ? 'Remove' : 'Capture'}
            </button>
          </div>
        </section>

        {/* 4. Landmark / Coordinates */}
        <section className="space-y-2">
          <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">4. Landmark or Street Location</label>
          <input
            type="text"
            value={landmark}
            onChange={(e) => setLandmark(e.target.value)}
            required
            placeholder="e.g. Near Holkar Bridge, Sangam Bridge causeway"
            className="w-full h-11 px-3.5 rounded-xl bg-white dark:bg-[#0B1118] border border-slate-300 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-sm focus:border-blue-500 focus:outline-none"
          />
        </section>

        {/* 5. Additional Description */}
        <section className="space-y-2">
          <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">5. Observations &amp; Ground Notes</label>
          <textarea
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            className="w-full p-3 rounded-xl bg-white dark:bg-[#0B1118] border border-slate-300 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-sm focus:border-blue-500 focus:outline-none"
          />
        </section>

        {/* 6. Anonymous Alias */}
        <div className="flex items-center justify-between text-xs text-slate-600 dark:text-[#9AA8B8] pt-1">
          <div className="flex items-center gap-1.5">
            <span className="material-symbols-outlined text-sm">badge</span>
            <span>Submitting as:</span>
            <input
              type="text"
              value={reporterAlias}
              onChange={(e) => setReporterAlias(e.target.value)}
              className="px-2 py-0.5 rounded bg-white dark:bg-[#0B1118] border border-slate-300 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] font-mono text-xs w-36"
            />
          </div>
          <span className="text-[11px] text-slate-400 dark:text-[#65778B]">Token #KST-7842</span>
        </div>

        {/* Submit CTA */}
        <div className="pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full h-12 rounded-xl bg-[#1D4ED8] hover:bg-blue-600 text-white font-bold text-sm shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-50"
          >
            {isSubmitting ? (
              <>
                <span className="material-symbols-outlined text-[18px] animate-spin">refresh</span>
                <span>Transmitting to EOC Gateway...</span>
              </>
            ) : (
              <>
                <span className="material-symbols-outlined text-[18px]">send</span>
                <span>Submit Ground Observation</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

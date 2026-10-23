import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import {
  setSelectedWardId,
  setSubscribeOpen,
  setReportOpen,
  setReportCategory,
  setReportDescription,
} from '../features/resident/residentSlice';
import { addToast } from '../features/ui/uiSlice';
import { SeverityBadge, ProvenanceBadge, DataAge } from '../components';
import { useCreateSubscriptionMutation } from '../api/registryApi';
import { useGetAlertStatusesQuery } from '../api/rulesApi';
import { useSubmitGroundReportMutation, useGetReportsForLocationQuery } from '../api/fanoutApi';
import type {
  SupportedLanguage,
  Severity,
  Provenance,
  GroundReportCategory,
  DeliveryChannel,
} from '../types/api';
import {
  Bell,
  Camera,
  Share2,
  MapPin,
  X,
  CheckCircle2,
  Users,
} from 'lucide-react';

const PILOT_WARDS = [
  {
    id: 'mumbai-kurla',
    name: 'Kurla Ward L, Mumbai',
    basin: 'Mithi River',
    helpline: '1916 (BMC Disaster Cell)',
  },
  {
    id: 'patna-ward12',
    name: 'Ward 12, Patna',
    basin: 'Ganges River',
    helpline: '1070 (Bihar SDMA)',
  },
  {
    id: 'guwahati-river',
    name: 'Riverside, Guwahati',
    basin: 'Brahmaputra Basin',
    helpline: '1079 (ASDMA Emergency)',
  },
  {
    id: 'cuttack-delta',
    name: 'Cuttack Municipal Corp',
    basin: 'Mahanadi Delta',
    helpline: '1077 (Odisha OSDMA)',
  },
];

export const ResidentView: React.FC = () => {
  const dispatch = useAppDispatch();
  const selectedWardId = useAppSelector((state) => state.resident.selectedWardId) || 'mumbai-kurla';
  const language = useAppSelector((state) => state.ui.language);
  const isSubscribeOpen = useAppSelector((state) => state.resident.isSubscribeOpen);
  const isReportOpen = useAppSelector((state) => state.resident.isReportOpen);
  const reportCategory = useAppSelector((state) => state.resident.reportCategory);
  const reportDescription = useAppSelector((state) => state.resident.reportDescription);

  // Local form states
  const [reporterName, setReporterName] = useState('');
  const [waterLevelCm, setWaterLevelCm] = useState(25);
  const [subChannel, setSubChannel] = useState<DeliveryChannel>('WHATSAPP');
  const [subRecipient, setSubRecipient] = useState('');
  const [subLanguage, setSubLanguage] = useState<SupportedLanguage>(language);
  const [subSeverity, setSubSeverity] = useState<Severity>('WARNING');

  // RTK Query API hooks
  const { data: alertStatuses } = useGetAlertStatusesQuery(undefined, { pollingInterval: 5000 });
  const { data: reports } = useGetReportsForLocationQuery(selectedWardId);

  const [submitReport, { isLoading: isReporting }] = useSubmitGroundReportMutation();
  const [createSubscription, { isLoading: isSubscribing }] = useCreateSubscriptionMutation();

  const currentWard = PILOT_WARDS.find((w) => w.id === selectedWardId) || PILOT_WARDS[0];
  const activeStatus = alertStatuses?.find((s) => s.locationId === selectedWardId);
  const severity: Severity = activeStatus?.severity || 'NONE';
  const provenance: Provenance = activeStatus?.provenance || 'MODEL_ONLY';
  const reportCount = reports?.length || 0;

  // Localized Headline and Advice Content
  const getAlertContent = () => {
    switch (severity) {
      case 'SEVERE':
        if (language === 'hi') {
          return {
            title: `🚨 गंभीर पूर चेतावनी — ${currentWard.name}`,
            subtitle: `${currentWard.basin} का जलस्तर चरम सीमा पार कर चुका है। तत्काल सुरक्षित स्थानों पर जाएं।`,
            instructions: [
              'निचली मंजिलों को तुरंत खाली करें और ऊंचे स्थानों पर जाएं।',
              'बिजली के मुख्य स्विच और गैस कनेक्शन बंद करें।',
              `आपातकालीन हेल्पलाइन पर कॉल करें: ${currentWard.helpline}`,
            ],
            color: 'rose',
          };
        }
        if (language === 'mr') {
          return {
            title: `🚨 गंभीर पूर चेतावणी — ${currentWard.name}`,
            subtitle: `${currentWard.basin} नदीची पातळी अत्यंत धोकादायक पातळीवर आहे. तात्काळ सुरक्षित स्थळी जावे.`,
            instructions: [
              'सखल भागातील घरांतून तातडीने सुरक्षित ठिकाणी स्थलांतर करा.',
              'विद्युत पुरवठा व गॅस सिलिंडर बंद करा.',
              `आपत्कालीन मदत कक्ष: ${currentWard.helpline}`,
            ],
            color: 'rose',
          };
        }
        return {
          title: `🚨 Severe Flood Warning — ${currentWard.name}`,
          subtitle: `River discharge in ${currentWard.basin} exceeds 20-Year extreme breach threshold. Urgent evacuation required.`,
          instructions: [
            'Evacuate ground-level residences immediately to designated municipal relief shelters.',
            'Turn off main electricity switch and LPG cylinder valves.',
            `Contact Emergency Helpline: ${currentWard.helpline}`,
          ],
          color: 'rose',
        };

      case 'WARNING':
        if (language === 'hi') {
          return {
            title: `⚠️ केस्ट्रल पूर चेतावनी — ${currentWard.name}`,
            subtitle: `अगले २४-४८ घंटों में ${currentWard.basin} का जलस्तर बढ़ने और निचले इलाकों में जलभराव की उच्च संभावना।`,
            instructions: [
              'महत्वपूर्ण दस्तावेज, दवाएं और पीने का पानी सुरक्षित स्थान पर रखें।',
              'नदी तटों और जलभराव वाले रास्तों पर जाने से बचें।',
              'स्थानीय प्रशासन के अलर्ट पर नजर रखें।',
            ],
            color: 'orange',
          };
        }
        if (language === 'mr') {
          return {
            title: `⚠️ केस्ट्रल पूर इशारा — ${currentWard.name}`,
            subtitle: `पुढील ४८ तासांत ${currentWard.basin} नदीची पातळी वाढण्याची आणि सखल भागात पूरस्थिती निर्माण होण्याची शक्यता.`,
            instructions: [
              'महत्त्वाची कागदपत्रे, औषधे व पिण्याचे पाणी सुरक्षित ठिकाणी ठेवा.',
              'नदीकाठच्या रस्त्यांवर व पुलांवर जाणे टाळा.',
              'स्थानिक महापालिका / आपत्ती व्यवस्थापन कक्षाच्या सूचनांचे पालन करा.',
            ],
            color: 'orange',
          };
        }
        return {
          title: `⚠️ Flood Warning (5-Yr Crest) — ${currentWard.name}`,
          subtitle: `High probability of river inundation and urban waterlogging in ${currentWard.basin} over the next 48 hours.`,
          instructions: [
            'Secure emergency essentials: drinking water, dry rations, torch, medicines, and power banks.',
            'Avoid walking or driving through waterlogged underpasses and riverfront roads.',
            'Follow municipal disaster management bulletins.',
          ],
          color: 'orange',
        };

      case 'WATCH':
        if (language === 'hi') {
          return {
            title: `🟡 पूर निगरानी सलाह — ${currentWard.name}`,
            subtitle: `${currentWard.basin} में जलस्तर सामान्य से अधिक हो रहा है। सतर्क रहें।`,
            instructions: [
              'मौसम और नदी के स्तर पर नियमित नजर बनाए रखें।',
              'स्थानीय नाले या जल निकासी अवरुद्ध होने पर प्रशासन को सूचित करें।',
            ],
            color: 'amber',
          };
        }
        if (language === 'mr') {
          return {
            title: `🟡 पूर दक्षता सल्ला — ${currentWard.name}`,
            subtitle: `${currentWard.basin} मध्ये पाण्याची पातळी वाढताना दिसत आहे. नागरिकांनी सतर्क राहावे.`,
            instructions: [
              'हवामान अंदाज व नदी पातळीविषयी नियमित माहिती घेत राहा.',
              'पाणी साचल्यास किंवा गटारे तुंबल्यास त्वरित तक्रार नोंदवा.',
            ],
            color: 'amber',
          };
        }
        return {
          title: `🟡 Flood Watch Advisory — ${currentWard.name}`,
          subtitle: `River discharge in ${currentWard.basin} is approaching elevated levels. Maintain situational awareness.`,
          instructions: [
            'Monitor weather updates and river basin advisories.',
            'Report any sudden water level rise in your neighborhood.',
          ],
          color: 'amber',
        };

      case 'NONE':
      default:
        if (language === 'hi') {
          return {
            title: `🟢 स्थिति सामान्य — ${currentWard.name}`,
            subtitle: `${currentWard.basin} में जलस्तर सामान्य सीमा में है। कोई सक्रिय बाढ़ जोखिम नहीं है।`,
            instructions: [
              'सभी नदी प्रवाह और मौसम संकेतक सुरक्षित सीमा के भीतर हैं।',
              'आपदा संबंधी पूर्व सूचना पाने के लिए नि:शुल्क अलर्ट सदस्यता लें।',
            ],
            color: 'emerald',
          };
        }
        if (language === 'mr') {
          return {
            title: `🟢 परिस्थिती सुरक्षित — ${currentWard.name}`,
            subtitle: `${currentWard.basin} नदीची पातळी सुरक्षित मर्यादेत आहे. कोणताही धोका नाही.`,
            instructions: [
              'सर्व जलप्रवाह निर्देशांक स्थिर व सुरक्षित आहेत.',
              'भविष्यातील आपत्कालीन पूर्वसूचनेसाठी मोफत सदस्यत्व घ्या.',
            ],
            color: 'emerald',
          };
        }
        return {
          title: `🟢 Normal / Safe Conditions — ${currentWard.name}`,
          subtitle: `River discharge in ${currentWard.basin} is within baseline normal limits. No flood risk detected.`,
          instructions: [
            'All hydrological ensemble forecast models indicate calm water levels.',
            'Subscribe for free WhatsApp/Telegram alerts to stay prepared ahead of monsoon spikes.',
          ],
          color: 'emerald',
        };
    }
  };

  const alertContent = getAlertContent();

  const handleGroundReportSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await submitReport({
        locationId: selectedWardId,
        category: (reportCategory as GroundReportCategory) || 'WATER_RISING',
        description:
          reportDescription.trim() ||
          `Water rising rapidly observed near main ward road (${waterLevelCm} cm estimate).`,
        reporterName: reporterName.trim() || 'Resident Citizen',
        waterLevelCm: Number(waterLevelCm),
      }).unwrap();

      dispatch(
        addToast({
          id: `toast-${Date.now()}`,
          type: 'success',
          message: `Ground report accepted! Location provenance immediately upgraded to ${res.provenanceUpgradedTo}.`,
        })
      );
      dispatch(setReportOpen(false));
      dispatch(setReportDescription(''));
    } catch (err: unknown) {
      dispatch(
        addToast({
          id: `toast-err-${Date.now()}`,
          type: 'error',
          message: 'Failed to record ground report. Check backend connectivity.',
        })
      );
    }
  };

  const handleSubscribeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subRecipient.trim()) {
      dispatch(
        addToast({
          id: `toast-err-${Date.now()}`,
          type: 'warning',
          message: 'Please provide a valid phone number or handle.',
        })
      );
      return;
    }

    try {
      const res = await createSubscription({
        locationId: selectedWardId,
        channel: subChannel,
        recipientHandle: subRecipient.trim(),
        preferredLanguage: subLanguage,
        minSeverity: subSeverity,
      }).unwrap();

      dispatch(
        addToast({
          id: `toast-${Date.now()}`,
          type: 'success',
          message: `Subscribed successfully! Token: ${res.token.slice(0, 8)}... (${subChannel} alerts enabled)`,
        })
      );
      dispatch(setSubscribeOpen(false));
      setSubRecipient('');
    } catch (err: unknown) {
      dispatch(
        addToast({
          id: `toast-err-${Date.now()}`,
          type: 'error',
          message: 'Failed to register subscription. Please try again.',
        })
      );
    }
  };

  const handleShareToWhatsApp = () => {
    const text = `${alertContent.title}\n${alertContent.subtitle}\n\nEmergency Helpline: ${currentWard.helpline}\n[Relayed by Kestrel Citizen Alert Relay - Provenance: ${provenance}]`;
    const url = `https://wa.me/?text=${encodeURIComponent(text)}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Ward Selector Bar */}
      <div className="bg-[#101726]/90 border border-slate-800 rounded-2xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-lg">
        <div className="flex items-center space-x-2">
          <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20">
            <MapPin className="w-4 h-4" />
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Your Ward / Location
            </div>
            <div className="text-sm font-bold text-slate-100">{currentWard.name}</div>
          </div>
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-auto">
          <select
            value={selectedWardId}
            onChange={(e) => dispatch(setSelectedWardId(e.target.value))}
            className="w-full sm:w-auto bg-slate-900 border border-slate-700/80 rounded-xl px-3.5 py-2 text-xs font-medium text-slate-200 focus:outline-none focus:border-blue-500 cursor-pointer"
          >
            {PILOT_WARDS.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name} ({w.basin})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Main Resident Emergency Card */}
      <div
        className={`rounded-3xl border p-6 sm:p-8 shadow-2xl relative overflow-hidden transition-all duration-300 ${
          severity === 'SEVERE'
            ? 'bg-gradient-to-b from-rose-950/60 via-slate-900 to-[#0b0f19] border-rose-500/50 shadow-rose-950/30'
            : severity === 'WARNING'
            ? 'bg-gradient-to-b from-orange-950/60 via-slate-900 to-[#0b0f19] border-orange-500/50 shadow-orange-950/30'
            : severity === 'WATCH'
            ? 'bg-gradient-to-b from-amber-950/40 via-slate-900 to-[#0b0f19] border-amber-500/40'
            : 'bg-gradient-to-b from-emerald-950/30 via-slate-900 to-[#0b0f19] border-emerald-500/30'
        }`}
      >
        {/* Pulsing hazard glow if warning or severe */}
        {(severity === 'WARNING' || severity === 'SEVERE') && (
          <div className="absolute top-0 right-0 w-64 h-64 bg-orange-500/10 rounded-full blur-3xl pointer-events-none" />
        )}

        {/* Badges Row */}
        <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
          <div className="flex items-center space-x-2">
            <SeverityBadge severity={severity} size="lg" language={language} />
            <ProvenanceBadge provenance={provenance} size="md" language={language} />
          </div>

          <div className="flex items-center space-x-3 text-xs text-slate-400">
            <DataAge seconds={activeStatus?.dataAgeSeconds ?? 180} language={language} />
            {reportCount > 0 && (
              <span className="inline-flex items-center space-x-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-purple-500/20 text-purple-300 border border-purple-500/30">
                <Users className="w-3.5 h-3.5" />
                <span>{reportCount} Ground Corroboration{reportCount > 1 ? 's' : ''}</span>
              </span>
            )}
          </div>
        </div>

        {/* Alert Title & Subtitle */}
        <div className="space-y-3 mb-6">
          <h2 className="text-xl sm:text-2xl font-black text-slate-100 tracking-tight leading-snug">
            {alertContent.title}
          </h2>
          <p className="text-sm sm:text-base text-slate-300 leading-relaxed max-w-3xl">
            {alertContent.subtitle}
          </p>
        </div>

        {/* Actionable Instructions */}
        <div className="space-y-2 mb-8 bg-slate-950/60 border border-slate-800/80 rounded-2xl p-4 sm:p-5">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
            {language === 'hi' ? 'नागरिक सुरक्षा निर्देश:' : language === 'mr' ? 'नागरिक सुरक्षा मार्गदर्शक:' : 'Resident Safety Instructions:'}
          </div>
          <ul className="space-y-2 text-xs sm:text-sm text-slate-200">
            {alertContent.instructions.map((inst, idx) => (
              <li key={idx} className="flex items-start space-x-2.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <span>{inst}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Action Buttons Row */}
        <div className="flex flex-col sm:flex-row items-center gap-3">
          {/* Ground Report Button */}
          <button
            id="btn-report-ground"
            onClick={() => dispatch(setReportOpen(true))}
            className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 px-5 py-3 rounded-xl font-bold text-xs bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white shadow-lg shadow-purple-600/25 transition-all cursor-pointer"
          >
            <Camera className="w-4 h-4" />
            <span>
              {language === 'hi' ? 'जमीनी स्थिति की रिपोर्ट करें' : language === 'mr' ? 'जमिनी परिस्थिती नोंदवा' : 'Report Ground Situation (2-Click)'}
            </span>
          </button>

          {/* Subscribe for Alerts Button */}
          <button
            id="btn-subscribe-alerts"
            onClick={() => dispatch(setSubscribeOpen(true))}
            className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 px-5 py-3 rounded-xl font-bold text-xs bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 shadow-md transition-all cursor-pointer"
          >
            <Bell className="w-4 h-4 text-blue-400" />
            <span>
              {language === 'hi' ? 'मुफ्त अलर्ट सदस्यता लें' : language === 'mr' ? 'मोफत अलर्टसाठी सबस्क्राइब करा' : 'Subscribe for Alerts'}
            </span>
          </button>

          {/* Share to WhatsApp */}
          <button
            onClick={handleShareToWhatsApp}
            className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 px-5 py-3 rounded-xl font-bold text-xs bg-[#25D366]/20 text-[#25D366] border border-[#25D366]/40 hover:bg-[#25D366]/30 shadow-md transition-all cursor-pointer"
          >
            <Share2 className="w-4 h-4" />
            <span>
              {language === 'hi' ? 'व्हाट्सएप पर शेयर करें' : language === 'mr' ? 'व्हॉट्सअॅपवर शेअर करा' : 'Share to WhatsApp Group'}
            </span>
          </button>
        </div>
      </div>

      {/* Community Corroborations Feed */}
      <div className="bg-[#101726]/90 border border-slate-800 rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Users className="w-4 h-4 text-purple-400" />
            <h3 className="text-sm font-bold text-slate-100">
              {language === 'hi' ? 'नागरिक ग्राउंड रिपोर्ट' : language === 'mr' ? 'नागरिक जमिनी अहवाल' : 'Community Ground Corroborations'}
            </h3>
          </div>
          <span className="text-xs text-slate-400">
            {reportCount} {reportCount === 1 ? 'Report' : 'Reports'}
          </span>
        </div>

        {reports && reports.length > 0 ? (
          <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
            {reports.map((rep) => (
              <div
                key={rep.id}
                className="p-3 rounded-xl bg-slate-900/80 border border-slate-800/80 text-xs flex items-start justify-between gap-3"
              >
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="font-semibold text-slate-200">{rep.reporterName}</span>
                    <span className="px-2 py-0.5 rounded text-[10px] bg-purple-500/20 text-purple-300 font-mono">
                      {rep.category}
                    </span>
                    {rep.waterLevelCm && (
                      <span className="text-[10px] text-blue-400 font-semibold">
                        ~{rep.waterLevelCm} cm water
                      </span>
                    )}
                  </div>
                  <p className="text-slate-300 text-[11px]">{rep.description}</p>
                </div>
                <span className="text-[10px] text-slate-500 shrink-0">
                  {new Date(rep.reportedAt).toLocaleTimeString()}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-3">
            No community reports submitted for this ward yet. Submit a report above if you observe rising water.
          </p>
        )}
      </div>

      {/* 2-Click Ground Report Modal */}
      {isReportOpen && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200"
        >
          <div className="relative w-full max-w-lg bg-[#0f172a] border border-slate-700 rounded-2xl shadow-2xl p-6 space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-2">
                <Camera className="w-5 h-5 text-purple-400" />
                <h3 className="text-base font-bold text-slate-100">Report Ground Situation</h3>
              </div>
              <button
                onClick={() => dispatch(setReportOpen(false))}
                className="p-1 rounded-lg text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleGroundReportSubmit} className="space-y-4">
              {/* Category Selection */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-slate-300">
                  What is happening on the ground? (Click 1)
                </label>
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { id: 'WATER_RISING', label: '🌊 Water Rising Rapidly' },
                    { id: 'ROAD_BLOCKED', label: '🚧 Road / Underpass Blocked' },
                    { id: 'NEED_HELP', label: '🆘 Stranded / Need Help' },
                    { id: 'OTHER', label: 'ℹ️ Other Observation' },
                  ].map((cat) => (
                    <button
                      key={cat.id}
                      type="button"
                      onClick={() => dispatch(setReportCategory(cat.id))}
                      className={`p-2.5 rounded-xl text-xs font-semibold border text-left transition-colors ${
                        reportCategory === cat.id
                          ? 'bg-purple-600/30 text-purple-200 border-purple-500 shadow-sm'
                          : 'bg-slate-900 border-slate-800 text-slate-300 hover:bg-slate-800'
                      }`}
                    >
                      {cat.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Water Level Slider */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs text-slate-300">
                  <span>Estimated Water Depth:</span>
                  <span className="font-bold text-blue-400">{waterLevelCm} cm</span>
                </div>
                <input
                  type="range"
                  min="5"
                  max="150"
                  step="5"
                  value={waterLevelCm}
                  onChange={(e) => setWaterLevelCm(Number(e.target.value))}
                  className="w-full accent-blue-500 cursor-pointer"
                />
              </div>

              {/* Reporter Name & Description */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300">Your Name (Optional)</label>
                <input
                  type="text"
                  value={reporterName}
                  onChange={(e) => setReporterName(e.target.value)}
                  placeholder="e.g. Rahul Sharma"
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-purple-500"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300">Details / Location Note</label>
                <input
                  type="text"
                  value={reportDescription}
                  onChange={(e) => dispatch(setReportDescription(e.target.value))}
                  placeholder="e.g. Near Bail Bazar bridge, water waist high"
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-purple-500"
                />
              </div>

              <p className="text-[11px] text-purple-300/80 bg-purple-950/30 border border-purple-900/40 p-2.5 rounded-xl">
                ⚡ Submitting this report immediately upgrades this ward's alert provenance from{' '}
                <code>MODEL_ONLY</code> to <code>MODEL_PLUS_REPORTS</code> on coordinator dashboards.
              </p>

              <div className="flex items-center justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => dispatch(setReportOpen(false))}
                  className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isReporting}
                  className="px-5 py-2.5 rounded-xl text-xs font-bold bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white shadow-lg transition-all disabled:opacity-60"
                >
                  {isReporting ? 'Submitting...' : 'Submit Ground Report (Click 2)'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Subscribe Modal */}
      {isSubscribeOpen && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200"
        >
          <div className="relative w-full max-w-md bg-[#0f172a] border border-slate-700 rounded-2xl shadow-2xl p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-2">
                <Bell className="w-5 h-5 text-blue-400" />
                <h3 className="text-base font-bold text-slate-100">Subscribe for Local Alerts</h3>
              </div>
              <button
                onClick={() => dispatch(setSubscribeOpen(false))}
                className="p-1 rounded-lg text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubscribeSubmit} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300">Delivery Channel</label>
                <div className="grid grid-cols-3 gap-2">
                  {(['WHATSAPP', 'TELEGRAM', 'SMS'] as DeliveryChannel[]).map((ch) => (
                    <button
                      key={ch}
                      type="button"
                      onClick={() => setSubChannel(ch)}
                      className={`p-2 rounded-xl text-xs font-semibold border text-center transition-colors ${
                        subChannel === ch
                          ? 'bg-blue-600/30 text-blue-200 border-blue-500'
                          : 'bg-slate-900 border-slate-800 text-slate-400 hover:bg-slate-800'
                      }`}
                    >
                      {ch}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300">
                  {subChannel === 'TELEGRAM' ? 'Telegram Username (@handle)' : 'Mobile Phone Number'}
                </label>
                <input
                  type="text"
                  value={subRecipient}
                  onChange={(e) => setSubRecipient(e.target.value)}
                  placeholder={subChannel === 'TELEGRAM' ? '@kurla_resident' : '+91 98765 43210'}
                  required
                  className="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-300">Language</label>
                  <select
                    value={subLanguage}
                    onChange={(e) => setSubLanguage(e.target.value as SupportedLanguage)}
                    className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    <option value="en">English</option>
                    <option value="hi">हिंदी (Hindi)</option>
                    <option value="mr">मराठी (Marathi)</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-300">Minimum Level</label>
                  <select
                    value={subSeverity}
                    onChange={(e) => setSubSeverity(e.target.value as Severity)}
                    className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    <option value="WATCH">Watch (2-Yr)</option>
                    <option value="WARNING">Warning (5-Yr)</option>
                    <option value="SEVERE">Severe Only (20-Yr)</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => dispatch(setSubscribeOpen(false))}
                  className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubscribing}
                  className="px-5 py-2.5 rounded-xl text-xs font-bold bg-blue-600 hover:bg-blue-500 text-white shadow-lg transition-all disabled:opacity-60"
                >
                  {isSubscribing ? 'Registering...' : 'Confirm Subscription'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ResidentView;

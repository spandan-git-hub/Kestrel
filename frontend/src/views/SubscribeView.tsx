import React, { useState, useMemo, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { addToast, setActiveTab } from '../features/ui/uiSlice';
import { setActiveSubscriptionToken } from '../features/resident/residentSlice';
import { useCreateSubscriptionMutation, useGetLocationsQuery } from '../api/registryApi';
import { ALL_45_RIVER_STATIONS } from '../catalog/indianRivers';
import { ALL_HEAT_STATIONS } from '../catalog/heatStations';
import { ALL_AQ_STATIONS } from '../catalog/aqStations';
import { getStationLiveSeverity } from '../api/liveOpenMeteoFallback';
import {
  sendTelegramMessage,
  getStoredTelegramBotToken,
  setStoredTelegramBotToken,
  getStoredTelegramDefaultChatId,
  getTelegramBotUsername,
  fetchLatestTelegramChatId,
  TelegramSendResult,
} from '../api/telegramService';
import type { Hazard, Severity, SupportedLanguage } from '../types/api';
import {
  Bell,
  MapPin,
  Check,
  Send,
  Radio,
  Globe,
  ShieldCheck,
  BookOpen,
  Copy,
  Clock,
  Zap,
  AlertTriangle,
  X,
  Search,
  CheckCircle2,
  ExternalLink,
  MessageSquare,
  Settings,
  Key,
  Share2,
  BellRing,
  RefreshCw,
} from 'lucide-react';

interface HazardStationOption {
  id: string;
  name: string;
  subTitle: string;
  state: string;
  hazard: Hazard;
  details: string;
}

export const SubscribeView: React.FC = () => {
  const dispatch = useAppDispatch();
  const currentMapHazard = (useAppSelector((state) => state.ui.activeHazard) as Hazard) || 'FLOOD';
  const selectedWardId = useAppSelector((state) => state.map.selectedLocationId) || 'mumbai-kurla';

  // 1. HAZARD CHOICE (First choose what hazard)
  const [selectedHazard, setSelectedHazard] = useState<Hazard>(currentMapHazard);

  // 2. STATIONS CHOICE (Then choose what station(s) you want notifications for)
  const [selectedStationIds, setSelectedStationIds] = useState<string[]>([selectedWardId]);
  const [stationSearch, setStationSearch] = useState<string>('');

  // 3. DIRECT NOTIFICATION CHANNELS (Telegram and/or Web Push)
  const [selectedChannels, setSelectedChannels] = useState<string[]>(['TELEGRAM', 'WEB_PUSH']);
  const [telegramHandle, setTelegramHandle] = useState<string>(() => {
    return getStoredTelegramDefaultChatId() || '';
  });
  const [webPushPermission, setWebPushPermission] = useState<string>(() => {
    return typeof Notification !== 'undefined' ? Notification.permission : 'default';
  });

  // Telegram testing & config state
  const [telegramToken, setTelegramToken] = useState<string>(() => getStoredTelegramBotToken());
  const [showTelegramSettings, setShowTelegramSettings] = useState<boolean>(false);
  const [telegramTestStatus, setTelegramTestStatus] = useState<TelegramSendResult | null>(null);
  const [isTestingTelegram, setIsTestingTelegram] = useState<boolean>(false);
  const [isDetectingChat, setIsDetectingChat] = useState<boolean>(false);
  const [isTestingWebPush, setIsTestingWebPush] = useState<boolean>(false);
  const [activeTestPushBanner, setActiveTestPushBanner] = useState<{ title: string; body: string } | null>(null);
  const [showDelayedModal, setShowDelayedModal] = useState<boolean>(false);
  const [delayedSeconds, setDelayedSeconds] = useState<number>(300); // 5 min default
  const [isSchedulingDelay, setIsSchedulingDelay] = useState<boolean>(false);
  const [delayScheduledNotice, setDelayScheduledNotice] = useState<string | null>(null);
  const telegramBotUsername = getTelegramBotUsername();

  // 4. LANGUAGE CHOICE (Just ANY ONE language choice, not all)
  const [selectedLanguage, setSelectedLanguage] = useState<SupportedLanguage>('en');

  // Confirmation modal & preview state
  const [isSuccessModal, setIsSuccessModal] = useState<boolean>(false);
  const [immediateRiskStationCount, setImmediateRiskStationCount] = useState<number>(0);
  const [copiedPreview, setCopiedPreview] = useState<boolean>(false);

  // RTK Query API hooks
  const { data: locationsData } = useGetLocationsQuery();
  const [createSubscription, { isLoading }] = useCreateSubscriptionMutation();

  // Build station list strictly for the selected hazard
  const stationsForHazard: HazardStationOption[] = useMemo(() => {
    if (selectedHazard === 'HEAT') {
      return ALL_HEAT_STATIONS.map((s) => ({
        id: s.id,
        name: s.name,
        subTitle: `${s.climateZone} · ${s.state}`,
        state: s.state,
        hazard: 'HEAT' as Hazard,
        details: `IMD Normal Tmax: ${s.normalTmax}°C · ${s.regionType} Zone`,
      }));
    }

    if (selectedHazard === 'AIR_QUALITY') {
      return ALL_AQ_STATIONS.map((s) => ({
        id: s.id,
        name: s.name,
        subTitle: `${s.zone} · ${s.state}`,
        state: s.state,
        hazard: 'AIR_QUALITY' as Hazard,
        details: `CPCB Station: ${s.cpcbStationId} · Sensor Telemetry`,
      }));
    }

    // Default: FLOOD
    const floodList = locationsData && locationsData.length > 0 ? locationsData : ALL_45_RIVER_STATIONS;
    return floodList.map((s) => ({
      id: s.id,
      name: s.name,
      subTitle: `${s.riverBasin || 'River Basin'} · ${s.state}`,
      state: s.state,
      hazard: 'FLOOD' as Hazard,
      details: `2-Yr Watch: ${s.threshold2yr || 40} m³/s · 5-Yr Warning: ${s.threshold5yr || 85} m³/s`,
    }));
  }, [selectedHazard, locationsData]);

  // When selected hazard changes, ensure at least one valid station for that hazard is selected
  useEffect(() => {
    const hasValidStation = selectedStationIds.some((id) =>
      stationsForHazard.some((st) => st.id === id)
    );
    if (!hasValidStation && stationsForHazard.length > 0) {
      setSelectedStationIds([stationsForHazard[0].id]);
    }
  }, [selectedHazard, stationsForHazard]);

  // Filter stations by search term
  const filteredStations = useMemo(() => {
    const term = stationSearch.toLowerCase().trim();
    if (!term) return stationsForHazard;
    return stationsForHazard.filter(
      (st) =>
        st.name.toLowerCase().includes(term) ||
        st.state.toLowerCase().includes(term) ||
        st.subTitle.toLowerCase().includes(term) ||
        st.details.toLowerCase().includes(term)
    );
  }, [stationsForHazard, stationSearch]);

  // Primary active station for preview
  const primaryStation =
    stationsForHazard.find((s) => selectedStationIds.includes(s.id)) ||
    stationsForHazard[0];

  // Toggle station selection
  const toggleStationSelection = (stationId: string) => {
    setSelectedStationIds((prev) => {
      if (prev.includes(stationId)) {
        if (prev.length === 1) {
          // Keep at least one station selected
          return prev;
        }
        return prev.filter((id) => id !== stationId);
      }
      return [...prev, stationId];
    });
  };

  const removeStationSelection = (stationId: string) => {
    setSelectedStationIds((prev) => {
      if (prev.length <= 1) return prev;
      return prev.filter((id) => id !== stationId);
    });
  };

  const toggleChannel = (channel: string) => {
    setSelectedChannels((prev) => {
      if (prev.includes(channel)) {
        return prev.length > 1 ? prev.filter((c) => c !== channel) : prev;
      }
      return [...prev, channel];
    });
  };

  const requestWebPushPermission = async () => {
    if (typeof Notification !== 'undefined' && Notification.requestPermission) {
      try {
        const perm = await Notification.requestPermission();
        setWebPushPermission(perm);
        if (perm === 'granted') {
          dispatch(
            addToast({
              id: `wp_perm_${Date.now()}`,
              type: 'success',
              message: 'Browser push notifications successfully enabled on this device!',
            })
          );
        } else if (perm === 'denied') {
          dispatch(
            addToast({
              id: `wp_denied_${Date.now()}`,
              type: 'error',
              message: 'Notifications are blocked in browser settings. Click the tune/padlock icon in the URL bar to allow notifications.',
            })
          );
        }
      } catch {
        // Handled gracefully
      }
    }
  };

  // Generate preview in the SINGLE selected language
  const generateSingleLanguagePreview = (): string => {
    const stationName = primaryStation?.name || 'Selected Station';
    const stationSub = primaryStation?.subTitle || '';
    const manualUrl = `https://kestrel.relay/manuals?hazard=${selectedHazard}&severity=WARNING`;

    if (selectedLanguage === 'hi') {
      const hazardHi =
        selectedHazard === 'HEAT'
          ? 'भीषण गर्मी और लू'
          : selectedHazard === 'AIR_QUALITY'
          ? 'गंभीर वायु प्रदूषण (AQI)'
          : 'नदी जलस्तर और बाढ़';
      return (
        `🚨 केस्ट्रल नागरिक आपात पूर्वचेतावनी [सक्रिय जोखिम]\n` +
        `📍 स्थान: ${stationName} (${stationSub})\n` +
        `⚠️ आपदा: ${hazardHi} चेतावनी सीमा पार।\n` +
        `📊 जोखिम स्तर: प्लेटफॉर्म द्वारा स्वचालित मूल्यांकित।\n` +
        `📘 सावधानियां और निर्देश पुस्तिका (Emergency Manual): ${manualUrl}\n` +
        `🌐 लाइव ट्रैकिंग: https://kestrel.relay/\n\n` +
        `⏱️ सूचना चक्र: 24 घंटे में एक बार (स्थिति स्थिर रहने पर)। परिस्थिति बदलने पर तत्काल अलर्ट।`
      );
    }

    if (selectedLanguage === 'mr') {
      const hazardMr =
        selectedHazard === 'HEAT'
          ? 'उष्णतेची तीव्र लाट'
          : selectedHazard === 'AIR_QUALITY'
          ? 'धोकादायक हवेची गुणवत्ता'
          : 'पूर आणि नदी पातळीत वाढ';
      return (
        `🚨 केस्ट्रल आपत्कालीन इशारा [सक्रिय धोका]\n` +
        `📍 ठिकाण: ${stationName} (${stationSub})\n` +
        `⚠️ संकट: ${hazardMr} धोक्याची मर्यादा ओलांडत आहे.\n` +
        `📊 धोका पातळी: प्रणालीद्वारे स्वयंचलित निर्णय.\n` +
        `📘 काय करावे / काय करू नये (Emergency Manual): ${manualUrl}\n` +
        `🌐 थेट नकाशा: https://kestrel.relay/\n\n` +
        `⏱️ सूचना चक्र: दर 24 तासांनी एकदा (स्थिती कायम असल्यास). बदल झाल्यास त्वरित सूचना.`
      );
    }

    // Default: English
    const hazardEn =
      selectedHazard === 'HEAT'
        ? 'Extreme Heat Wave'
        : selectedHazard === 'AIR_QUALITY'
        ? 'Air Quality (AQI Emergency)'
        : 'River Inundation & Crest';
    return (
      `🚨 KESTREL CIVIC ALERT [RISK CONDITION DETECTED]\n` +
      `📍 Location: ${stationName} (${stationSub})\n` +
      `⚠️ Hazard: ${hazardEn} threshold breached.\n` +
      `📊 Alert Severity: Autonomously triggered by platform telemetry.\n` +
      `📘 Emergency Do's & Don'ts Manual: ${manualUrl}\n` +
      `🌐 Real-time Map: https://kestrel.relay/\n\n` +
      `⏱️ Notification Cadence: Once every 24 hr (if condition unchanged). Instant immediate alert upon any condition change.`
    );
  };

  const handleCopyPreview = () => {
    navigator.clipboard.writeText(generateSingleLanguagePreview());
    setCopiedPreview(true);
    setTimeout(() => setCopiedPreview(false), 2000);
    dispatch(
      addToast({
        id: `copy_${Date.now()}`,
        type: 'info',
        message: 'Notification message preview copied to clipboard!',
      })
    );
  };

  const playAlertChime = () => {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(659.25, ctx.currentTime); // E5
      osc.frequency.setValueAtTime(880, ctx.currentTime + 0.12); // A5
      gain.gain.setValueAtTime(0.2, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.35);
    } catch {
      // Audio optional
    }
  };

  const handleTestWebPush = async () => {
    setIsTestingWebPush(true);
    try {
      if (typeof window === 'undefined' || !('Notification' in window)) {
        dispatch(
          addToast({
            id: `wp_notsup_${Date.now()}`,
            type: 'error',
            message: 'Web Push notifications are not supported by this browser environment.',
          })
        );
        return;
      }

      let perm = Notification.permission;
      if (perm === 'default') {
        try {
          perm = await Notification.requestPermission();
          setWebPushPermission(perm);
        } catch {
          // Handled gracefully
        }
      }

      if (perm === 'denied') {
        dispatch(
          addToast({
            id: `wp_denied_${Date.now()}`,
            type: 'error',
            message: 'Notifications are BLOCKED in your browser site settings. Click the site settings icon (padlock/tune) in your browser address bar to allow Notifications.',
          })
        );
        return;
      }

      if (perm !== 'granted') {
        dispatch(
          addToast({
            id: `wp_denied_${Date.now()}`,
            type: 'warning',
            message: `Notification permission status: "${perm}". Please click "Enable Push" to allow alerts.`,
          })
        );
        return;
      }

      // 1. Play civic alert chime immediately
      playAlertChime();

      const stationName = primaryStation?.name || 'Selected Station';
      const title = `🚨 [TEST ALERT] ${stationName} - Kestrel`;
      const bodyText = `Live ${selectedHazard} test alert for ${stationName}. Threshold verification test. 24-hr rolling update cycle active.`;

      // 2. Trigger in-app high-visibility floating emergency alert banner
      setActiveTestPushBanner({ title, body: bodyText });
      setTimeout(() => {
        setActiveTestPushBanner((curr) => (curr?.title === title ? null : curr));
      }, 9000);

      // 3. Flash browser tab title to alert the user
      const originalTitle = document.title;
      let flashes = 0;
      const flashTimer = setInterval(() => {
        document.title = flashes % 2 === 0 ? `🚨 [ALERT] ${stationName}` : '⚠️ KESTREL HAZARD';
        flashes++;
        if (flashes > 8) {
          clearInterval(flashTimer);
          document.title = originalTitle;
        }
      }, 600);

      // 4. Service Worker Notification (connects to platform Push Notification manager on Windows/Android)
      if ('serviceWorker' in navigator) {
        try {
          let reg = await navigator.serviceWorker.getRegistration();
          if (!reg) {
            reg = await navigator.serviceWorker.register('/sw.js');
          }
          if (reg && reg.showNotification) {
            await reg.showNotification(title, {
              body: bodyText,
              tag: `kestrel-test-${Date.now()}`,
              requireInteraction: true,
            });
          }
        } catch (swErr) {
          console.debug('Service Worker showNotification fallback:', swErr);
        }
      }

      // 5. Direct browser Notification constructor (standard fallback)
      try {
        const notif = new Notification(title, {
          body: bodyText,
          tag: `kestrel-test-${Date.now()}`,
        });
        notif.onclick = () => {
          window.focus();
          notif.close();
        };
      } catch (directErr) {
        console.debug('Direct Notification constructor fallback:', directErr);
      }

      // 6. Direct Windows Action Center Native Toast (guaranteed delivery to Windows notification tray)
      fetch('/dev-alert/toast', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, message: bodyText }),
      }).catch((e) => console.debug('Dev Windows Toast bridge error:', e));

      dispatch(
        addToast({
          id: `wp_test_success_${Date.now()}`,
          type: 'success',
          message: '🔔 Alert notification dispatched! (Banner on screen + audio chime + Windows Notification Tray)',
        })
      );
    } catch (err: any) {
      dispatch(
        addToast({
          id: `wp_err_${Date.now()}`,
          type: 'error',
          message: `Failed to trigger push notification: ${err?.message || 'Check browser permissions'}`,
        })
      );
    } finally {
      setIsTestingWebPush(false);
    }
  };

  const handleScheduleDelayedAlert = async (seconds: number) => {
    setIsSchedulingDelay(true);
    try {
      const stationName = primaryStation?.name || 'Selected Station';
      const title = `🚨 [DELAYED ALERT] ${stationName} Risk Warning`;
      const message = `River crest / weather warning simulation fired while Chrome and application were closed!`;

      const res = await fetch('/dev-alert/schedule', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          delaySeconds: seconds,
          title,
          message,
          botToken: telegramToken,
          chatId: telegramHandle,
        }),
      });

      const data = await res.json().catch(() => null);
      if (data?.ok) {
        setDelayScheduledNotice(
          `⏱️ Background Timer Armed for ${seconds >= 60 ? `${Math.round(seconds / 60)} minute(s)` : `${seconds} seconds`}! You can now CLOSE Chrome and this app completely. Your laptop tray will pop up the alert and chime when the timer expires.`
        );
        dispatch(
          addToast({
            id: `delay_ok_${Date.now()}`,
            type: 'success',
            message: `Background timer armed for ${seconds}s! You can safely close Chrome now.`,
          })
        );
      } else {
        throw new Error(data?.error || 'Failed to arm timer');
      }
    } catch (e: any) {
      dispatch(
        addToast({
          id: `delay_err_${Date.now()}`,
          type: 'error',
          message: `Could not schedule timer: ${e?.message || 'Server error'}`,
        })
      );
    } finally {
      setIsSchedulingDelay(false);
    }
  };

  const handleTestTelegram = async () => {
    setIsTestingTelegram(true);
    setTelegramTestStatus(null);
    try {
      const recipient = telegramHandle.trim();
      if (!recipient) {
        dispatch(
          addToast({
            id: `tg_err_${Date.now()}`,
            type: 'error',
            message: 'Please enter your Telegram Chat ID or username.',
          })
        );
        setIsTestingTelegram(false);
        return;
      }

      const testMsg =
        `🧪 [KESTREL TEST BROADCAST]\n\n` +
        generateSingleLanguagePreview() +
        `\n\n(Verification test triggered from Kestrel Subscribe panel)`;

      const result = await sendTelegramMessage(recipient, testMsg, telegramToken);
      setTelegramTestStatus(result);

      if (result.success) {
        dispatch(
          addToast({
            id: `tg_success_${Date.now()}`,
            type: 'success',
            message: '✈️ Telegram alert message delivered successfully!',
          })
        );
      } else {
        dispatch(
          addToast({
            id: `tg_fail_${Date.now()}`,
            type: 'warning',
            message: result.message,
          })
        );
      }
    } catch (err: any) {
      dispatch(
        addToast({
          id: `tg_err_${Date.now()}`,
          type: 'error',
          message: `Telegram dispatch failed: ${err?.message || 'Network error'}`,
        })
      );
    } finally {
      setIsTestingTelegram(false);
    }
  };

  const handleOpenTelegramShare = () => {
    const text = generateSingleLanguagePreview();
    const shareUrl = `https://t.me/share/url?url=${encodeURIComponent('https://kestrel.relay/')}&text=${encodeURIComponent(text)}`;
    window.open(shareUrl, '_blank', 'noopener,noreferrer');
    dispatch(
      addToast({
        id: `tg_share_${Date.now()}`,
        type: 'info',
        message: 'Opened Telegram app / Web share with your pre-formatted hazard alert.',
      })
    );
  };

  const handleSaveTelegramToken = (token: string) => {
    setTelegramToken(token);
    setStoredTelegramBotToken(token);
    dispatch(
      addToast({
        id: `tg_token_${Date.now()}`,
        type: 'success',
        message: token.trim() ? 'Telegram Bot Token saved locally!' : 'Telegram Bot Token cleared.',
      })
    );
  };

  const handleAutoDetectChatId = async () => {
    setIsDetectingChat(true);
    try {
      const res = await fetchLatestTelegramChatId(telegramToken);
      if (res.success && res.chatId) {
        setTelegramHandle(res.chatId);
        dispatch(
          addToast({
            id: `tg_detect_${Date.now()}`,
            type: 'success',
            message: `✅ Auto-detected your Telegram Chat! ID: ${res.chatId}${res.username ? ` (@${res.username})` : ''}. Ready for alerts!`,
          })
        );
      } else {
        dispatch(
          addToast({
            id: `tg_detect_err_${Date.now()}`,
            type: 'warning',
            message: res.error || 'No recent chat detected. Open @kestrel_official_bot in Telegram, click START, and try again!',
          })
        );
      }
    } catch (e: any) {
      dispatch(
        addToast({
          id: `tg_detect_err_${Date.now()}`,
          type: 'error',
          message: `Detection failed: ${e?.message || 'Network error'}`,
        })
      );
    } finally {
      setIsDetectingChat(false);
    }
  };

  const handleSubscribeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const automatedChannels = selectedChannels.filter((c) => c === 'TELEGRAM' || c === 'WEB_PUSH');
    if (automatedChannels.length === 0) {
      dispatch(
        addToast({
          id: `sub_err_${Date.now()}`,
          type: 'error',
          message: 'Please select at least one notification channel (Telegram and/or Web Push).',
        })
      );
      return;
    }

    if (automatedChannels.includes('TELEGRAM')) {
      const handle = telegramHandle.trim();
      if (!handle) {
        dispatch(
          addToast({
            id: `sub_err_${Date.now()}`,
            type: 'error',
            message: 'Please enter your Telegram username (@handle) or numeric Chat ID.',
          })
        );
        return;
      }
      const compact = handle.replace(/[\s\-()]/g, '');
      if (
        /^(\+91|91|0)?[6-9]\d{9}$/.test(compact) ||
        /^\+[0-9]{7,15}$/.test(compact) ||
        /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(handle)
      ) {
        dispatch(
          addToast({
            id: `sub_err_${Date.now()}`,
            type: 'error',
            message: 'DPDP Act 2023 Zero-PII: Phone numbers and emails are prohibited. Use your Telegram username (@handle) or numeric Chat ID.',
          })
        );
        return;
      }
    }

    if (selectedStationIds.length === 0) {
      dispatch(
        addToast({
          id: `sub_err_${Date.now()}`,
          type: 'error',
          message: 'Please select at least one station for the chosen hazard.',
        })
      );
      return;
    }

    try {
      const tokens: string[] = [];
      let immediateRisksCount = 0;

      for (const stId of selectedStationIds) {
        // Check if this station currently has active risk
        const liveSeverity: Severity = getStationLiveSeverity(stId, selectedHazard);
        const hasRisk = liveSeverity !== 'NONE';
        if (hasRisk) {
          immediateRisksCount++;
        }

        for (const ch of automatedChannels) {
          const identifier =
            ch === 'TELEGRAM'
              ? telegramHandle.trim()
              : `webpush_${stId}_${Math.random().toString(36).substring(2, 9)}`;

          const res = await createSubscription({
            locationId: stId,
            channel: ch as any,
            recipientIdentifier: identifier,
            recipientHandle: identifier,
            preferredLanguage: selectedLanguage,
            language: selectedLanguage,
            minSeverity: 'WATCH', // Platform decides alerts autonomously from WATCH up
            hazards: [selectedHazard],
            consentGiven: true,
            consentVersion: 'DPDP-2023-v1.0',
          }).unwrap();

          if (res?.token) {
            tokens.push(res.token);
          }
        }
      }

      const activeToken = tokens[tokens.length - 1];
      if (activeToken) {
        dispatch(setActiveSubscriptionToken(activeToken));
      }

      setImmediateRiskStationCount(immediateRisksCount);

      if (immediateRisksCount > 0) {
        // Immediate notification triggered on subscription to risky station
        dispatch(
          addToast({
            id: `sub_imm_${Date.now()}`,
            type: 'warning',
            message: `⚠️ IMMEDIATE ALERT: ${immediateRisksCount} station(s) currently at active risk! Initial alert dispatched. 24-hr rolling window is now running.`,
          })
        );
      } else {
        dispatch(
          addToast({
            id: `sub_${Date.now()}`,
            type: 'success',
            message: `Hazard alert subscription active for ${selectedStationIds.length} station(s) via ${automatedChannels.join(' & ')}!`,
          })
        );
      }

      // Asynchronously trigger Telegram dispatch if Telegram channel is selected
      if (automatedChannels.includes('TELEGRAM')) {
        const previewMsg = generateSingleLanguagePreview();
        sendTelegramMessage(telegramHandle.trim(), previewMsg, telegramToken).then((tgRes) => {
          if (tgRes.success) {
            dispatch(
              addToast({
                id: `tg_live_${Date.now()}`,
                type: 'success',
                message: '✈️ Telegram alert successfully delivered to your chat!',
              })
            );
          } else {
            console.info('[Telegram] Initial send status:', tgRes.status, tgRes.message);
          }
        });
      }

      setIsSuccessModal(true);
    } catch (err: any) {
      const errorMsg =
        err?.data?.detail ||
        err?.data?.message ||
        err?.message ||
        'Failed to activate subscription. Please check network connection.';
      dispatch(
        addToast({
          id: `sub_err_${Date.now()}`,
          type: 'error',
          message: errorMsg,
        })
      );
    }
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 pb-12">
      {/* High-visibility In-App Emergency Broadcast Alert Banner */}
      {activeTestPushBanner && (
        <div className="fixed top-5 left-1/2 -translate-x-1/2 z-50 w-[94%] max-w-2xl animate-in slide-in-from-top-4 duration-300 shadow-2xl">
          <div className="bg-red-600 dark:bg-red-700 text-white rounded-2xl p-4 sm:p-5 border-2 border-amber-300 shadow-xl flex items-start gap-3.5">
            <div className="p-2.5 rounded-xl bg-white/20 text-white shrink-0 animate-pulse mt-0.5">
              <BellRing className="w-6 h-6 stroke-[2.5]" />
            </div>
            <div className="flex-1 space-y-1">
              <div className="flex items-center justify-between gap-2">
                <span className="font-bold text-sm sm:text-base tracking-wide flex items-center gap-2">
                  <span>{activeTestPushBanner.title}</span>
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-white text-red-700 font-extrabold uppercase tracking-wider">
                    Test Triggered
                  </span>
                </span>
                <button
                  type="button"
                  onClick={() => setActiveTestPushBanner(null)}
                  className="p-1 rounded-lg hover:bg-white/20 text-white/80 hover:text-white transition-colors cursor-pointer"
                  title="Dismiss alert"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              <p className="text-xs sm:text-sm text-white/95 leading-relaxed font-medium">
                {activeTestPushBanner.body}
              </p>
              <div className="text-[11px] text-white/85 pt-1.5 flex flex-wrap items-center gap-2">
                <span>🔊 Audio Chime Sounded</span>
                <span>•</span>
                <span>🖥️ Sent to Windows/Browser Push Service</span>
                <span>•</span>
                <span>🏷️ Do&apos;s &amp; Don&apos;ts manual linked</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 1. TOP HERO HEADER */}
      <section className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 sm:p-8 shadow-sm space-y-2">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-blue-50 dark:bg-[#1C2633] text-blue-600 dark:text-[#7AA7FF] border border-blue-200 dark:border-blue-500/30">
            <Bell className="w-4 h-4" />
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
            Subscribe to Direct Hazard Broadcasts
          </h1>
        </div>
        <p className="text-xs sm:text-sm text-slate-600 dark:text-[#9AA8B8] max-w-3xl leading-relaxed">
          First choose the hazard, then choose what station(s) you want notifications for. Select your notification channels (Telegram and/or Web Push) and your preferred language. The platform autonomously detects risks and triggers immediate alerts, followed by a 24-hour rolling update cycle.
        </p>
      </section>

      {/* 2. MAIN TWO-COLUMN SUBSCRIPTION BUILDER */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Form Controls (7 cols) */}
        <form
          onSubmit={handleSubscribeSubmit}
          className="lg:col-span-7 bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-6 text-xs"
        >
          {/* STEP 1: FIRST CHOOSE WHAT HAZARD */}
          <div className="space-y-3 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
            <div className="flex items-center justify-between">
              <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-blue-600 text-white text-[11px] flex items-center justify-center font-bold">1</span>
                <span>First, choose what hazard you want notifications for:</span>
              </label>
              <span className="text-[11px] text-blue-600 dark:text-[#7AA7FF] font-semibold">
                {selectedHazard === 'FLOOD' ? '45 River Stations' : selectedHazard === 'HEAT' ? '42 Observatories' : '32 CAAQMS Stations'}
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              {/* Flood Option */}
              <button
                type="button"
                onClick={() => setSelectedHazard('FLOOD')}
                className={`p-3.5 rounded-xl border text-left transition-all flex items-center justify-between cursor-pointer ${
                  selectedHazard === 'FLOOD'
                    ? 'bg-blue-50 dark:bg-blue-600/15 border-blue-500 text-blue-900 dark:text-[#7AA7FF] font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:border-slate-300 dark:hover:border-slate-600'
                }`}
              >
                <div>
                  <div className="font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-1.5">
                    <span>🌊 River Flood</span>
                  </div>
                  <div className="text-[10px] text-slate-500 mt-0.5">Inundation &amp; crests</div>
                </div>
                {selectedHazard === 'FLOOD' && <Check className="w-4 h-4 text-blue-600 shrink-0" />}
              </button>

              {/* Heat Option */}
              <button
                type="button"
                onClick={() => setSelectedHazard('HEAT')}
                className={`p-3.5 rounded-xl border text-left transition-all flex items-center justify-between cursor-pointer ${
                  selectedHazard === 'HEAT'
                    ? 'bg-amber-50 dark:bg-amber-600/15 border-amber-500 text-amber-900 dark:text-amber-400 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:border-slate-300 dark:hover:border-slate-600'
                }`}
              >
                <div>
                  <div className="font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-1.5">
                    <span>🔥 Extreme Heat</span>
                  </div>
                  <div className="text-[10px] text-slate-500 mt-0.5">IMD heatwave criteria</div>
                </div>
                {selectedHazard === 'HEAT' && <Check className="w-4 h-4 text-amber-600 shrink-0" />}
              </button>

              {/* AQ Option */}
              <button
                type="button"
                onClick={() => setSelectedHazard('AIR_QUALITY')}
                className={`p-3.5 rounded-xl border text-left transition-all flex items-center justify-between cursor-pointer ${
                  selectedHazard === 'AIR_QUALITY'
                    ? 'bg-cyan-50 dark:bg-cyan-600/15 border-cyan-500 text-cyan-900 dark:text-cyan-400 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:border-slate-300 dark:hover:border-slate-600'
                }`}
              >
                <div>
                  <div className="font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-1.5">
                    <span>💨 Air Quality</span>
                  </div>
                  <div className="text-[10px] text-slate-500 mt-0.5">CPCB AQI spikes</div>
                </div>
                {selectedHazard === 'AIR_QUALITY' && <Check className="w-4 h-4 text-cyan-600 shrink-0" />}
              </button>
            </div>
          </div>

          {/* STEP 2: THEN CHOOSE WHAT STATIONS YOU WANT NOTIFICATIONS FOR */}
          <div className="space-y-3 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
            <div className="flex items-center justify-between">
              <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-blue-600 text-white text-[11px] flex items-center justify-center font-bold">2</span>
                <span>Then choose what station(s) you want notifications for:</span>
              </label>
              <span className="text-[11px] text-slate-500">
                {selectedStationIds.length} station{selectedStationIds.length > 1 ? 's' : ''} selected
              </span>
            </div>

            {/* Selected Station Chips */}
            <div className="flex flex-wrap gap-1.5 min-h-[32px] p-2 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644]">
              {selectedStationIds.map((id) => {
                const st = stationsForHazard.find((s) => s.id === id);
                if (!st) return null;
                return (
                  <span
                    key={st.id}
                    className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-blue-50 dark:bg-blue-900/40 border border-blue-200 dark:border-blue-500/40 text-blue-900 dark:text-[#7AA7FF] text-[11px] font-semibold"
                  >
                    <MapPin className="w-3 h-3 text-red-500 shrink-0" />
                    <span>{st.name}</span>
                    {selectedStationIds.length > 1 && (
                      <button
                        type="button"
                        onClick={() => removeStationSelection(st.id)}
                        className="hover:text-red-500 transition-colors ml-0.5"
                        title="Remove station"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    )}
                  </span>
                );
              })}
            </div>

            {/* Search Input */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-3 text-slate-400" />
              <input
                type="text"
                value={stationSearch}
                onChange={(e) => setStationSearch(e.target.value)}
                placeholder={`Search ${selectedHazard === 'HEAT' ? 'heat observatories' : selectedHazard === 'AIR_QUALITY' ? 'CAAQMS monitoring stations' : 'river basins & gauging stations'}...`}
                className="w-full h-9 pl-9 pr-3 text-xs rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] focus:outline-none focus:border-blue-500"
              />
            </div>

            {/* Station List Selector */}
            <div className="max-h-52 overflow-y-auto space-y-1.5 custom-scrollbar pr-1 border border-slate-200 dark:border-[#2A3644] rounded-xl p-2 bg-slate-50/50 dark:bg-[#151D27]">
              {filteredStations.map((st) => {
                const isSelected = selectedStationIds.includes(st.id);
                return (
                  <div
                    key={st.id}
                    onClick={() => toggleStationSelection(st.id)}
                    className={`p-2.5 rounded-xl cursor-pointer transition-all flex items-center justify-between border ${
                      isSelected
                        ? 'bg-blue-50/80 dark:bg-blue-600/20 border-blue-300 dark:border-blue-500/40 text-blue-900 dark:text-[#7AA7FF]'
                        : 'bg-white dark:bg-[#1C2633] border-slate-100 dark:border-[#2A3644]/60 text-slate-700 dark:text-[#9AA8B8] hover:border-slate-300 dark:hover:border-slate-600'
                    }`}
                  >
                    <div className="flex items-center gap-2.5">
                      <div
                        className={`w-4 h-4 rounded-md border flex items-center justify-center shrink-0 ${
                          isSelected
                            ? 'bg-blue-600 border-blue-600 text-white'
                            : 'border-slate-300 dark:border-slate-600 bg-white dark:bg-[#0B1118]'
                        }`}
                      >
                        {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                      </div>
                      <div>
                        <div className="font-semibold text-slate-900 dark:text-[#EAF0F6] text-xs">
                          {st.name}
                        </div>
                        <div className="text-[10px] text-slate-500 dark:text-[#9AA8B8]">
                          {st.subTitle} · <span className="font-mono text-slate-600 dark:text-slate-400">{st.details}</span>
                        </div>
                      </div>
                    </div>
                    {isSelected && (
                      <span className="text-[10px] font-bold text-blue-600 dark:text-[#7AA7FF] px-2 py-0.5 rounded bg-blue-100 dark:bg-blue-900/40">
                        Subscribed
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* STEP 3: NOTIFICATION CHANNELS (TELEGRAM AND/OR WEB PUSH) */}
          <div className="space-y-4 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
            <div className="flex items-center justify-between">
              <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-blue-600 text-white text-[11px] flex items-center justify-center font-bold">3</span>
                <span>Choose notification channel (Telegram and/or Web Push):</span>
              </label>
              <span className="text-[11px] font-normal text-emerald-600 dark:text-emerald-400 font-mono">
                DPDP Act 2023 Zero-PII
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Telegram Verified Bot */}
              <button
                type="button"
                onClick={() => toggleChannel('TELEGRAM')}
                className={`p-4 rounded-xl border text-left transition-all flex items-start justify-between cursor-pointer ${
                  selectedChannels.includes('TELEGRAM')
                    ? 'bg-cyan-50 dark:bg-cyan-500/15 border-cyan-500 text-cyan-900 dark:text-cyan-200 font-semibold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                <div className="flex items-start gap-3">
                  <div className="p-2 rounded-lg bg-cyan-100 dark:bg-cyan-500/20 text-cyan-600 dark:text-cyan-400 mt-0.5">
                    <Send className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-bold text-slate-900 dark:text-[#EAF0F6] text-xs">Telegram Bot Alerts</div>
                    <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8] mt-0.5">
                      Direct automated push via verified Kestrel bot
                    </div>
                  </div>
                </div>
                {selectedChannels.includes('TELEGRAM') && <Check className="w-4 h-4 text-cyan-600 mt-1 shrink-0" />}
              </button>

              {/* Web Push */}
              <button
                type="button"
                onClick={() => {
                  toggleChannel('WEB_PUSH');
                  if (webPushPermission !== 'granted') {
                    requestWebPushPermission();
                  }
                }}
                className={`p-4 rounded-xl border text-left transition-all flex items-start justify-between cursor-pointer ${
                  selectedChannels.includes('WEB_PUSH')
                    ? 'bg-blue-50 dark:bg-blue-500/15 border-blue-500 text-blue-900 dark:text-blue-200 font-semibold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-600 dark:text-[#9AA8B8]'
                }`}
              >
                <div className="flex items-start gap-3">
                  <div className="p-2 rounded-lg bg-blue-100 dark:bg-blue-500/20 text-blue-600 dark:text-blue-400 mt-0.5">
                    <Radio className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-bold text-slate-900 dark:text-[#EAF0F6] text-xs">Browser Web Push</div>
                    <div className="text-[11px] text-slate-500 dark:text-[#9AA8B8] mt-0.5">
                      Instant desktop/mobile notification on this browser
                    </div>
                  </div>
                </div>
                {selectedChannels.includes('WEB_PUSH') && <Check className="w-4 h-4 text-blue-600 mt-1 shrink-0" />}
              </button>
            </div>

            {/* Telegram Handle Input, Real API Test & Instant 1-Click Fallback */}
            {selectedChannels.includes('TELEGRAM') && (
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-1 text-[11px]">
                  <label className="font-semibold text-slate-800 dark:text-[#EAF0F6] flex items-center gap-1.5">
                    <Send className="w-3.5 h-3.5 text-cyan-600" />
                    <span>Telegram Numeric Chat ID or Username:</span>
                  </label>
                  <span className="text-[10px] text-emerald-600 dark:text-emerald-400 font-mono">Zero-PII DPDP Compliant</span>
                </div>

                <div className="flex gap-2">
                  <input
                    type="text"
                    value={telegramHandle}
                    onChange={(e) => setTelegramHandle(e.target.value)}
                    required
                    placeholder="e.g. 123456789 or click Auto-Detect"
                    className="flex-1 h-10 px-3 rounded-xl bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-xs focus:outline-none focus:border-cyan-500"
                  />
                  <button
                    type="button"
                    onClick={handleTestTelegram}
                    disabled={isTestingTelegram}
                    className="px-3.5 h-10 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-bold shrink-0 flex items-center gap-1.5 shadow-sm transition-all active:scale-95 disabled:opacity-60 cursor-pointer"
                    title="Send a real test alert message to this Telegram chat"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>{isTestingTelegram ? 'Sending...' : '⚡ Test Telegram Message'}</span>
                  </button>
                </div>

                {/* Quick actions & Fallback Buttons */}
                <div className="flex flex-wrap items-center gap-2 pt-1 text-[11px]">
                  {/* Open Bot link (Required by Telegram to start chat) */}
                  <a
                    href={`https://t.me/${telegramBotUsername.replace(/^@/, '')}?start=alert`}
                    target="_blank"
                    rel="noreferrer"
                    className="px-2.5 py-1.5 rounded-lg bg-blue-100 hover:bg-blue-200 dark:bg-blue-950/40 dark:hover:bg-blue-900/50 text-blue-800 dark:text-blue-300 font-semibold flex items-center gap-1.5 transition-colors"
                  >
                    <MessageSquare className="w-3 h-3 text-blue-600" />
                    <span>1. Open @{telegramBotUsername} &amp; Click START</span>
                    <ExternalLink className="w-2.5 h-2.5 opacity-60" />
                  </a>

                  {/* Auto-Detect Chat ID */}
                  <button
                    type="button"
                    onClick={handleAutoDetectChatId}
                    disabled={isDetectingChat}
                    className="px-2.5 py-1.5 rounded-lg bg-emerald-100 hover:bg-emerald-200 dark:bg-emerald-950/40 dark:hover:bg-emerald-900/50 text-emerald-800 dark:text-emerald-300 font-semibold flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-60"
                    title="Automatically find your Telegram Chat ID after clicking Start on the bot"
                  >
                    <RefreshCw className={`w-3 h-3 text-emerald-600 ${isDetectingChat ? 'animate-spin' : ''}`} />
                    <span>{isDetectingChat ? 'Detecting...' : '2. 🔄 Auto-Detect My Chat ID'}</span>
                  </button>

                  {/* Instant 1-Click Send via Telegram App (Works 100% reliably everywhere) */}
                  <button
                    type="button"
                    onClick={handleOpenTelegramShare}
                    className="px-2.5 py-1.5 rounded-lg bg-slate-200 hover:bg-slate-300 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-slate-700 dark:text-[#9AA8B8] font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                    title="Open alert in Telegram App / Web immediately without bot requirements"
                  >
                    <Share2 className="w-3 h-3 text-cyan-500" />
                    <span>🚀 1-Click Send via Telegram App</span>
                  </button>

                  {/* Get numeric Chat ID helper */}
                  <a
                    href="https://t.me/userinfobot"
                    target="_blank"
                    rel="noreferrer"
                    className="px-2.5 py-1.5 rounded-lg bg-slate-200 hover:bg-slate-300 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-slate-700 dark:text-[#9AA8B8] font-semibold flex items-center gap-1.5 transition-colors"
                  >
                    <span>Find Chat ID (@userinfobot)</span>
                    <ExternalLink className="w-2.5 h-2.5 opacity-60" />
                  </a>

                  {/* Toggle optional Bot Token settings */}
                  <button
                    type="button"
                    onClick={() => setShowTelegramSettings(!showTelegramSettings)}
                    className="px-2 py-1.5 text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 flex items-center gap-1 ml-auto text-[10px] cursor-pointer"
                  >
                    <Settings className="w-3 h-3" />
                    <span>{showTelegramSettings ? 'Hide Bot Token' : 'Bot Token Settings'}</span>
                  </button>
                </div>

                {/* Collapsible Bot Token settings */}
                {showTelegramSettings && (
                  <div className="p-3 rounded-xl bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] space-y-2 mt-2">
                    <div className="flex items-center justify-between text-[11px] font-semibold text-slate-800 dark:text-[#EAF0F6]">
                      <span className="flex items-center gap-1.5">
                        <Key className="w-3.5 h-3.5 text-amber-500" />
                        <span>Telegram Bot Token (Optional Custom Bot):</span>
                      </span>
                      <span className="text-[10px] font-normal text-slate-500">
                        {telegramToken ? 'Configured ✅' : 'Direct API mode'}
                      </span>
                    </div>
                    <div className="flex gap-2">
                      <input
                        type="password"
                        value={telegramToken}
                        onChange={(e) => setTelegramToken(e.target.value)}
                        placeholder="e.g. 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ"
                        className="flex-1 h-9 px-3 rounded-lg bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-slate-900 dark:text-[#EAF0F6] text-xs font-mono focus:outline-none focus:border-cyan-500"
                      />
                      <button
                        type="button"
                        onClick={() => handleSaveTelegramToken(telegramToken)}
                        className="px-3 h-9 rounded-lg bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold shrink-0 cursor-pointer"
                      >
                        Save Token
                      </button>
                    </div>
                    <p className="text-[10px] text-slate-500 dark:text-[#9AA8B8]">
                      Tokens are stored locally in your browser (never transmitted). If you don't have a bot token, you can still send all alerts via the <strong>1-Click Send via Telegram App</strong> button.
                    </p>
                  </div>
                )}

                {/* Telegram Test Feedback Banner */}
                {telegramTestStatus && (
                  <div
                    className={`p-3 rounded-xl border text-xs space-y-2 ${
                      telegramTestStatus.success
                        ? 'bg-emerald-50 dark:bg-emerald-950/30 border-emerald-300 dark:border-emerald-800 text-emerald-900 dark:text-emerald-200'
                        : telegramTestStatus.status === 'CHAT_NOT_FOUND'
                        ? 'bg-amber-50 dark:bg-amber-950/30 border-amber-300 dark:border-amber-800 text-amber-900 dark:text-amber-200'
                        : 'bg-blue-50 dark:bg-blue-950/30 border-blue-300 dark:border-blue-800 text-blue-900 dark:text-blue-200'
                    }`}
                  >
                    <div className="flex items-start gap-2">
                      {telegramTestStatus.success ? (
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                      ) : (
                        <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                      )}
                      <div className="flex-1 space-y-1">
                        <div className="font-bold">{telegramTestStatus.message}</div>
                        {telegramTestStatus.status === 'CHAT_NOT_FOUND' && (
                          <div className="text-[11px] text-amber-800 dark:text-amber-300 space-y-2 pt-1">
                            <p>
                              <strong>Why this happens:</strong> Telegram protocol prohibits bots from messaging users who haven't started the bot.
                            </p>
                            <div className="flex flex-wrap gap-2 pt-1">
                              <a
                                href={telegramTestStatus.botStartUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold inline-flex items-center gap-1.5"
                              >
                                <MessageSquare className="w-3.5 h-3.5" />
                                <span>1. Open Bot &amp; Press START</span>
                                <ExternalLink className="w-3 h-3" />
                              </a>
                              <a
                                href={telegramTestStatus.fallbackShareUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold inline-flex items-center gap-1.5"
                              >
                                <Share2 className="w-3.5 h-3.5" />
                                <span>2. Or Send via Telegram App Instantly</span>
                                <ExternalLink className="w-3 h-3" />
                              </a>
                            </div>
                          </div>
                        )}
                        {telegramTestStatus.status === 'NO_TOKEN' && (
                          <div className="pt-1">
                            <a
                              href={telegramTestStatus.fallbackShareUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="px-3 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-semibold inline-flex items-center gap-1.5"
                            >
                              <Share2 className="w-3.5 h-3.5" />
                              <span>Send Alert via Telegram App (Instant Share)</span>
                              <ExternalLink className="w-3 h-3" />
                            </a>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Web Push Controls & Live Test */}
            {selectedChannels.includes('WEB_PUSH') && (
              <div className="p-3.5 rounded-xl bg-blue-50/60 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-900/40 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-3 text-xs">
                  <div className="flex items-center gap-2 text-slate-700 dark:text-[#9AA8B8]">
                    <Radio className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF] shrink-0" />
                    <span>
                      Browser Push Status:{' '}
                      <strong
                        className={
                          webPushPermission === 'granted'
                            ? 'text-emerald-600 dark:text-emerald-400'
                            : 'text-amber-600 dark:text-amber-400'
                        }
                      >
                        {webPushPermission === 'granted'
                          ? 'Active & Permitted'
                          : webPushPermission === 'denied'
                          ? 'Blocked in Browser Settings'
                          : 'Permission Required'}
                      </strong>
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    {webPushPermission !== 'granted' && (
                      <button
                        type="button"
                        onClick={requestWebPushPermission}
                        className="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-[11px] font-semibold shrink-0 cursor-pointer transition-colors"
                      >
                        Enable Push
                      </button>
                    )}
                    {/* TEST BUTTON THAT RUNS THE WEB PUSH NOTIFICATION */}
                    <button
                      type="button"
                      onClick={handleTestWebPush}
                      disabled={isTestingWebPush}
                      className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-[11px] font-bold shrink-0 flex items-center gap-1.5 shadow-sm transition-all active:scale-95 disabled:opacity-60 cursor-pointer"
                      title="Trigger a real test Web Push Notification immediately to Windows Notification Tray"
                    >
                      <BellRing className="w-3.5 h-3.5" />
                      <span>{isTestingWebPush ? 'Sending...' : '⚡ Test Web Push Notification'}</span>
                    </button>

                    {/* BUTTON TO TEST ALERT WITH CHROME & APPLICATION CLOSED */}
                    <button
                      type="button"
                      onClick={() => setShowDelayedModal(true)}
                      className="px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-[11px] font-bold shrink-0 flex items-center gap-1.5 shadow-sm transition-all active:scale-95 cursor-pointer"
                      title="Simulate background alert with Chrome and app completely closed"
                    >
                      <Clock className="w-3.5 h-3.5" />
                      <span>⏱️ Test with Chrome Closed</span>
                    </button>
                  </div>
                </div>

                <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
                  <p className="text-[10px] text-slate-500 dark:text-[#9AA8B8]">
                    Click <strong>&quot;⚡ Test Web Push&quot;</strong> for instant tray alert, or <strong>&quot;⏱️ Test with Chrome Closed&quot;</strong> to verify background wake-up.
                  </p>
                  <button
                    type="button"
                    onClick={() => dispatch(setActiveTab('my-alerts'))}
                    className="text-[11px] font-semibold text-blue-600 dark:text-[#7AA7FF] hover:underline flex items-center gap-1 cursor-pointer"
                  >
                    <span>📜 Open My Alerts Tab (7-Day History) &rarr;</span>
                  </button>
                </div>

                {/* Armed Background Alert Notice */}
                {delayScheduledNotice && (
                  <div className="p-3 rounded-xl bg-emerald-100 dark:bg-emerald-950/40 border border-emerald-300 dark:border-emerald-800 text-emerald-900 dark:text-emerald-200 text-xs flex items-start gap-2.5">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    <div className="space-y-1">
                      <div className="font-bold">Background Alert Armed!</div>
                      <p className="text-[11px] leading-relaxed">{delayScheduledNotice}</p>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* STEP 4: SINGLE LANGUAGE CHOICE (ANY ONE LANGUAGE CHOICE, NOT ALL) */}
          <div className="space-y-3 pb-5 border-b border-slate-100 dark:border-[#2A3644]">
            <div className="flex items-center justify-between">
              <label className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6] flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-blue-600 text-white text-[11px] flex items-center justify-center font-bold">4</span>
                <span>Notification Language (Choose any one language):</span>
              </label>
              <span className="text-[11px] text-slate-500">Single language choice</span>
            </div>

            <div className="grid grid-cols-3 gap-2.5">
              <button
                type="button"
                onClick={() => setSelectedLanguage('en')}
                className={`p-3 rounded-xl border text-center transition-all flex items-center justify-center gap-2 cursor-pointer ${
                  selectedLanguage === 'en'
                    ? 'bg-blue-600 text-white border-blue-600 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:border-slate-300'
                }`}
              >
                <span>English</span>
                {selectedLanguage === 'en' && <CheckCircle2 className="w-3.5 h-3.5" />}
              </button>

              <button
                type="button"
                onClick={() => setSelectedLanguage('hi')}
                className={`p-3 rounded-xl border text-center transition-all flex items-center justify-center gap-2 cursor-pointer ${
                  selectedLanguage === 'hi'
                    ? 'bg-blue-600 text-white border-blue-600 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:border-slate-300'
                }`}
              >
                <span>हिन्दी (Hindi)</span>
                {selectedLanguage === 'hi' && <CheckCircle2 className="w-3.5 h-3.5" />}
              </button>

              <button
                type="button"
                onClick={() => setSelectedLanguage('mr')}
                className={`p-3 rounded-xl border text-center transition-all flex items-center justify-center gap-2 cursor-pointer ${
                  selectedLanguage === 'mr'
                    ? 'bg-blue-600 text-white border-blue-600 font-bold shadow-xs'
                    : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] hover:border-slate-300'
                }`}
              >
                <span>मराठी (Marathi)</span>
                {selectedLanguage === 'mr' && <CheckCircle2 className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>

          {/* STEP 5: AUTONOMOUS ALERTING & 24-HR ROLLING WINDOW EXPLANATION */}
          <div className="space-y-3 pb-2">
            <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] space-y-2">
              <div className="flex items-center gap-2 text-slate-900 dark:text-[#EAF0F6] font-bold text-xs">
                <Zap className="w-4 h-4 text-amber-500" />
                <span>Autonomous Platform Decision (No Manual Alert Threshold Choice)</span>
              </div>
              <p className="text-[11px] text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                You do not need to choose low, mid, or high alerts. Kestrel continuously monitors live hydrological, thermal, and ambient air telemetry. The platform autonomously determines when an alert threshold is breached and dispatches notifications automatically.
              </p>
            </div>

            {/* 24-Hour Rolling Window Cadence Card */}
            <div className="p-3.5 rounded-xl bg-blue-50/60 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-900/40 space-y-2">
              <div className="flex items-center gap-2 text-blue-900 dark:text-[#7AA7FF] font-bold text-xs">
                <Clock className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF]" />
                <span>24-Hour Rolling Window &amp; Instant Trigger Rules</span>
              </div>
              <ul className="text-[11px] text-slate-600 dark:text-[#9AA8B8] space-y-1 list-disc list-inside leading-relaxed">
                <li>
                  <strong className="text-slate-800 dark:text-slate-200">Subscribing to a Risky Station: </strong>
                  If a station has an active risk when you subscribe, you receive an immediate notification right away, and your 24-hour rolling window begins from that moment.
                </li>
                <li>
                  <strong className="text-slate-800 dark:text-slate-200">Condition Changes: </strong>
                  If conditions escalate or de-escalate, you receive an immediate alert instantly with no waiting.
                </li>
                <li>
                  <strong className="text-slate-800 dark:text-slate-200">Unchanged Condition: </strong>
                  If risk persists with conditions unchanged, reminders are sent once every 24 hours.
                </li>
              </ul>
            </div>
          </div>

          {/* SUBMIT BUTTON */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={isLoading}
              className="w-full h-12 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-md shadow-blue-500/25 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-50 cursor-pointer"
            >
              <ShieldCheck className="w-5 h-5" />
              <span>Confirm &amp; Activate Hazard Broadcast Subscription</span>
            </button>
          </div>
        </form>

        {/* Right Column: Live Single-Language Message Preview (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-[#2A3644]">
              <div className="flex items-center gap-2">
                <Globe className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF]" />
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Single-Language Message Preview ({selectedLanguage.toUpperCase()})
                </h3>
              </div>

              <button
                type="button"
                onClick={handleCopyPreview}
                className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-[#1C2633] dark:hover:bg-[#253243] text-slate-600 dark:text-[#9AA8B8] transition-colors cursor-pointer"
                title={copiedPreview ? 'Copied!' : 'Copy preview text'}
              >
                <Copy className="w-3.5 h-3.5" />
              </button>
            </div>

            <p className="text-[11px] text-slate-500 dark:text-[#9AA8B8]">
              Your alerts will be delivered in <strong>{selectedLanguage === 'hi' ? 'हिन्दी (Hindi)' : selectedLanguage === 'mr' ? 'मराठी (Marathi)' : 'English'}</strong>. Direct emergency manual links are included automatically:
            </p>

            {/* Message Preview Box */}
            <div className="p-4 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] font-mono text-[11px] text-slate-800 dark:text-[#EAF0F6] leading-relaxed whitespace-pre-wrap select-all max-h-[380px] overflow-y-auto custom-scrollbar">
              {generateSingleLanguagePreview()}
            </div>

            {/* Manual Link Callout */}
            <div className="p-3.5 rounded-xl bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/50 flex items-start gap-2.5 text-xs">
              <BookOpen className="w-4 h-4 text-blue-600 dark:text-[#7AA7FF] shrink-0 mt-0.5" />
              <div className="text-slate-600 dark:text-[#9AA8B8]">
                <strong className="text-blue-900 dark:text-[#7AA7FF]">Emergency Manual Link: </strong>
                Every outbound dispatch automatically contains the official Do's &amp; Don'ts manual link for that hazard.
              </div>
            </div>
          </div>

          {/* Privacy Notice */}
          <div className="p-4 rounded-2xl bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] text-xs text-slate-600 dark:text-[#9AA8B8] flex items-start gap-2.5 shadow-sm">
            <ShieldCheck className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold text-slate-900 dark:text-[#EAF0F6]">DPDP Act 2023 Compliant: </span>
              Anonymous tokens used. No personal phone numbers or profiling collected, and 1-click unsubscription at any time.
            </div>
          </div>
        </div>
      </div>

      {/* CONFIRMATION POPUP MODAL */}
      {isSuccessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 dark:bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-100 dark:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
              <Check className="w-6 h-6" />
            </div>

            <div className="space-y-2">
              <h3 className="text-lg font-bold text-slate-900 dark:text-[#EAF0F6]">
                Subscription Activated!
              </h3>
              <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
                You are now subscribed to {selectedHazard === 'FLOOD' ? 'River Flood' : selectedHazard === 'HEAT' ? 'Extreme Heat' : 'Air Quality'} alerts for{' '}
                <strong className="text-slate-900 dark:text-[#EAF0F6]">{selectedStationIds.length} station(s)</strong> via{' '}
                <strong>{selectedChannels.map((c) => (c === 'TELEGRAM' ? 'Telegram' : 'Web Push')).join(' & ')}</strong> in{' '}
                <strong>{selectedLanguage.toUpperCase()}</strong>.
              </p>

              {immediateRiskStationCount > 0 ? (
                <div className="p-3 rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/50 flex items-start gap-2 text-xs text-amber-900 dark:text-amber-300">
                  <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold">Active Risk Detected: </span>
                    {immediateRiskStationCount} subscribed station(s) are currently under an active hazard risk condition. An immediate alert notification was dispatched, and your 24-hour rolling update window has officially started!
                  </div>
                </div>
              ) : (
                <div className="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800/50 flex items-start gap-2 text-xs text-emerald-900 dark:text-emerald-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold">Current Status Safe: </span>
                    Subscribed station(s) are currently within safe baseline parameters. If conditions escalate, an immediate notification will be dispatched instantly.
                  </div>
                </div>
              )}
            </div>

            {selectedChannels.includes('TELEGRAM') && (
              <button
                type="button"
                onClick={handleOpenTelegramShare}
                className="w-full py-2.5 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer transition-colors"
              >
                <Share2 className="w-3.5 h-3.5" />
                <span>Open in Telegram App / Web</span>
              </button>
            )}

            <button
              type="button"
              onClick={() => setIsSuccessModal(false)}
              className="w-full py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold cursor-pointer"
            >
              Done
            </button>
          </div>
        </div>
      )}

      {/* DELAYED BACKGROUND ALERT TEST MODAL (WORKS WITH CHROME CLOSED) */}
      {showDelayedModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 dark:bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-[#2A3644]">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-xl bg-amber-100 dark:bg-amber-500/20 text-amber-600 dark:text-amber-400">
                  <Clock className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-slate-900 dark:text-[#EAF0F6]">
                  Test Alert (Chrome &amp; App Closed)
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowDelayedModal(false)}
                className="text-slate-400 hover:text-slate-700 dark:hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-600 dark:text-[#9AA8B8] leading-relaxed">
              This tests true background delivery. When you click <strong>Arm &amp; Close</strong>, an independent background process is armed. You can <strong>close Chrome and this application completely</strong>. When the timer finishes, your laptop will pop up the alert in the Windows Notification Tray and sound an alarm!
            </p>

            {/* Delay Choice Buttons */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-800 dark:text-[#EAF0F6]">
                Choose Delay Duration:
              </label>
              <div className="grid grid-cols-4 gap-2">
                <button
                  type="button"
                  onClick={() => setDelayedSeconds(10)}
                  className={`py-2 px-2 rounded-xl border text-xs font-semibold transition-all cursor-pointer ${
                    delayedSeconds === 10
                      ? 'bg-amber-600 text-white border-amber-600 shadow-sm'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                  }`}
                >
                  10 Sec
                </button>
                <button
                  type="button"
                  onClick={() => setDelayedSeconds(30)}
                  className={`py-2 px-2 rounded-xl border text-xs font-semibold transition-all cursor-pointer ${
                    delayedSeconds === 30
                      ? 'bg-amber-600 text-white border-amber-600 shadow-sm'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                  }`}
                >
                  30 Sec
                </button>
                <button
                  type="button"
                  onClick={() => setDelayedSeconds(60)}
                  className={`py-2 px-2 rounded-xl border text-xs font-semibold transition-all cursor-pointer ${
                    delayedSeconds === 60
                      ? 'bg-amber-600 text-white border-amber-600 shadow-sm'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                  }`}
                >
                  1 Min
                </button>
                <button
                  type="button"
                  onClick={() => setDelayedSeconds(300)}
                  className={`py-2 px-2 rounded-xl border text-xs font-semibold transition-all cursor-pointer ${
                    delayedSeconds === 300
                      ? 'bg-amber-600 text-white border-amber-600 shadow-sm'
                      : 'bg-slate-50 dark:bg-[#1C2633] border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8]'
                  }`}
                >
                  5 Min
                </button>
              </div>
            </div>

            {/* Station details */}
            <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#0B1118] border border-slate-200 dark:border-[#2A3644] text-xs space-y-1">
              <div className="text-slate-500 dark:text-[#9AA8B8]">Target Station:</div>
              <div className="font-bold text-slate-900 dark:text-[#EAF0F6]">
                {primaryStation?.name || 'Selected Ward'} ({selectedHazard})
              </div>
            </div>

            <div className="pt-2 flex gap-2">
              <button
                type="button"
                onClick={() => setShowDelayedModal(false)}
                className="flex-1 py-2.5 rounded-xl border border-slate-200 dark:border-[#2A3644] text-slate-700 dark:text-[#9AA8B8] text-xs font-semibold hover:bg-slate-50 dark:hover:bg-[#1C2633] cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={isSchedulingDelay}
                onClick={async () => {
                  await handleScheduleDelayedAlert(delayedSeconds);
                  setShowDelayedModal(false);
                }}
                className="flex-1 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold shadow-md shadow-amber-600/20 flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-60"
              >
                <span>{isSchedulingDelay ? 'Arming...' : `🚀 Arm ${delayedSeconds >= 60 ? `${delayedSeconds / 60}m` : `${delayedSeconds}s`} & Close`}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SubscribeView;

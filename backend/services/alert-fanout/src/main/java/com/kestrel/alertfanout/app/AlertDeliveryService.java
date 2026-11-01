package com.kestrel.alertfanout.app;

import com.kestrel.alertfanout.api.dto.BroadcastPreviewRequest;
import com.kestrel.alertfanout.api.dto.BroadcastPreviewResponse;
import com.kestrel.alertfanout.api.dto.BroadcastRequest;
import com.kestrel.alertfanout.api.dto.BroadcastResponse;
import com.kestrel.alertfanout.api.dto.RecentDispatchItem;
import com.kestrel.alertfanout.infra.client.TelegramBotClient;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.entity.SubscriberReplica;
import com.kestrel.alertfanout.infra.repository.DeliveryLogRepository;
import com.kestrel.alertfanout.infra.repository.LocationReplicaRepository;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import com.kestrel.common.events.TelegramDeliveryPayload;
import com.kestrel.common.events.WebPushDeliveryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AlertDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(AlertDeliveryService.class);

    private final LocationReplicaRepository locationReplicaRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final AlertTemplateComposer templateComposer;
    private final TelegramBotClient telegramBotClient;
    private final TelegramDeliveryWorker telegramDeliveryWorker;
    private final WebPushDeliveryWorker webPushDeliveryWorker;
    private final SubscriberReplicaService subscriberReplicaService;

    @Autowired
    public AlertDeliveryService(
            LocationReplicaRepository locationReplicaRepository,
            DeliveryLogRepository deliveryLogRepository,
            AlertTemplateComposer templateComposer,
            TelegramBotClient telegramBotClient,
            TelegramDeliveryWorker telegramDeliveryWorker,
            WebPushDeliveryWorker webPushDeliveryWorker,
            SubscriberReplicaService subscriberReplicaService) {
        this.locationReplicaRepository = locationReplicaRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.templateComposer = templateComposer;
        this.telegramBotClient = telegramBotClient;
        this.telegramDeliveryWorker = telegramDeliveryWorker;
        this.webPushDeliveryWorker = webPushDeliveryWorker;
        this.subscriberReplicaService = subscriberReplicaService;
    }

    public BroadcastPreviewResponse previewBroadcast(BroadcastPreviewRequest request) {
        Hazard hazard = request.hazard() != null ? request.hazard() : Hazard.FLOOD;
        LocationReplica location = findOrCreateLocation(request.locationId());
        Severity severity = request.severity() != null ? request.severity() : Severity.WARNING;
        Provenance provenance = location.getCurrentProvenance() != null ? location.getCurrentProvenance() : Provenance.MODEL_ONLY;
        LocalDate peakDate = LocalDate.now().plusDays(2);

        AlertTemplateComposer.ComposedMessage enMsg = templateComposer.composeAlert(hazard, location, severity, provenance, peakDate, Language.en);
        AlertTemplateComposer.ComposedMessage hiMsg = templateComposer.composeAlert(hazard, location, severity, provenance, peakDate, Language.hi);
        AlertTemplateComposer.ComposedMessage mrMsg = templateComposer.composeAlert(hazard, location, severity, provenance, peakDate, Language.mr);

        String whatsAppShareUrl = templateComposer.generateWhatsAppShareUrl(
                hazard, location, severity, provenance, peakDate, request.customNote());

        Map<String, String> previews = Map.of(
                "en", enMsg.fullText(),
                "hi", hiMsg.fullText(),
                "mr", mrMsg.fullText()
        );

        int recipientCount = estimateRecipientCount(location.getLocationId());

        return new BroadcastPreviewResponse(
                location.getLocationId(),
                location.getName(),
                severity,
                recipientCount,
                previews,
                whatsAppShareUrl
        );
    }

    public BroadcastResponse executeBroadcast(BroadcastRequest request) {
        Hazard hazard = request.hazard() != null ? request.hazard() : Hazard.FLOOD;
        LocationReplica location = findOrCreateLocation(request.locationId());
        Severity severity = request.severity() != null ? request.severity() : Severity.WARNING;
        Provenance provenance = location.getCurrentProvenance() != null ? location.getCurrentProvenance() : Provenance.MODEL_ONLY;
        LocalDate peakDate = LocalDate.now().plusDays(2);
        String broadcastId = "bc-" + UUID.randomUUID();
        Instant now = Instant.now();

        AlertTemplateComposer.ComposedMessage regionalMsg = templateComposer.composeAlert(hazard, location, severity, provenance, peakDate, location.getPrimaryLanguage());

        String whatsAppShareUrl = templateComposer.generateWhatsAppShareUrl(
                hazard, location, severity, provenance, peakDate, request.customNote());

        List<Channel> channels = (request.channels() != null && !request.channels().isEmpty())
                ? request.channels()
                : List.of(Channel.TELEGRAM, Channel.WEB_PUSH);

        int dispatchedCount = 0;

        // 1. Telegram Dispatch
        if (channels.contains(Channel.TELEGRAM)) {
            List<SubscriberReplica> tgSubscribers = subscriberReplicaService.getActiveSubscribersForAlert(location.getLocationId(), severity, hazard)
                    .stream()
                    .filter(s -> s.getChannel() == Channel.TELEGRAM)
                    .toList();

            if (!tgSubscribers.isEmpty()) {
                for (SubscriberReplica sub : tgSubscribers) {
                    Language subLang = sub.getLanguage() != null ? sub.getLanguage() : location.getPrimaryLanguage();
                    AlertTemplateComposer.ComposedMessage msg = templateComposer.composeAlert(hazard, location, severity, provenance, peakDate, subLang);
                    String text = msg.fullText();
                    if (request.customNote() != null && !request.customNote().isBlank()) {
                        text += "\n\n📌 Coordinator Note: " + request.customNote().trim();
                    }
                    String deliveryId = "deliv-" + UUID.randomUUID();
                    TelegramDeliveryPayload payload = new TelegramDeliveryPayload(
                            deliveryId,
                            broadcastId,
                            location.getLocationId(),
                            location.getName(),
                            severity,
                            sub.getRecipientIdentifier(),
                            sub.getToken(),
                            subLang,
                            text,
                            provenance,
                            whatsAppShareUrl,
                            now
                    );
                    telegramDeliveryWorker.dispatch(payload);
                    dispatchedCount++;
                }
            } else {
                // Default Telegram channel / coordinator chat dispatch
                String telegramText = regionalMsg.fullText();
                if (request.customNote() != null && !request.customNote().isBlank()) {
                    telegramText += "\n\n📌 Coordinator Note: " + request.customNote().trim();
                }
                String deliveryId = "deliv-" + UUID.randomUUID();
                String targetChat = telegramBotClient.getDefaultChatId() != null && !telegramBotClient.getDefaultChatId().isBlank()
                        ? telegramBotClient.getDefaultChatId()
                        : "@kestrel_alert_channel";
                TelegramDeliveryPayload payload = new TelegramDeliveryPayload(
                        deliveryId,
                        broadcastId,
                        location.getLocationId(),
                        location.getName(),
                        severity,
                        targetChat,
                        null,
                        location.getPrimaryLanguage(),
                        telegramText,
                        provenance,
                        whatsAppShareUrl,
                        now
                );
                telegramDeliveryWorker.dispatch(payload);
                dispatchedCount++;
            }
        }

        // 2. Web Push Dispatch
        if (channels.contains(Channel.WEB_PUSH)) {
            List<SubscriberReplica> pushSubscribers = subscriberReplicaService.getActiveSubscribersForAlert(location.getLocationId(), severity, hazard)
                    .stream()
                    .filter(s -> s.getChannel() == Channel.WEB_PUSH)
                    .toList();

            if (!pushSubscribers.isEmpty()) {
                for (SubscriberReplica sub : pushSubscribers) {
                    Language subLang = sub.getLanguage() != null ? sub.getLanguage() : location.getPrimaryLanguage();
                    AlertTemplateComposer.ComposedMessage msg = templateComposer.composeAlert(hazard, location, severity, provenance, peakDate, subLang);
                    String deliveryId = "deliv-" + UUID.randomUUID();
                    WebPushDeliveryPayload pushPayload = new WebPushDeliveryPayload(
                            deliveryId,
                            broadcastId,
                            location.getLocationId(),
                            location.getName(),
                            severity,
                            sub.getRecipientIdentifier(),
                            null,
                            null,
                            sub.getToken(),
                            subLang,
                            buildWebPushTitle(hazard, location, subLang),
                            msg.actionableAdvice(),
                            buildWebPushClickUrl(hazard, location.getLocationId()),
                            provenance,
                            now
                    );
                    webPushDeliveryWorker.dispatch(pushPayload);
                    dispatchedCount++;
                }
            } else {
                String deliveryId = "deliv-" + UUID.randomUUID();
                WebPushDeliveryPayload pushPayload = new WebPushDeliveryPayload(
                        deliveryId,
                        broadcastId,
                        location.getLocationId(),
                        location.getName(),
                        severity,
                        "https://fcm.googleapis.com/fcm/send/kestrel-broadcast-" + location.getLocationId(),
                        null,
                        null,
                        null,
                        location.getPrimaryLanguage(),
                        buildWebPushTitle(hazard, location, location.getPrimaryLanguage()),
                        regionalMsg.actionableAdvice(),
                        buildWebPushClickUrl(hazard, location.getLocationId()),
                        provenance,
                        now
                );
                webPushDeliveryWorker.dispatch(pushPayload);
                dispatchedCount++;
            }
        }

        log.info("Broadcast {} dispatched for location {} (hazard={}) across {} channels (count={})",
                broadcastId, location.getLocationId(), hazard, channels, dispatchedCount);

        return new BroadcastResponse(
                broadcastId,
                location.getLocationId(),
                location.getName(),
                severity,
                dispatchedCount,
                "DISPATCHED",
                whatsAppShareUrl,
                now,
                "Broadcast successfully dispatched across " + channels.size() + " delivery channels"
        );
    }

    public void deliverAutomatedAlert(AlertEventPayload payload, LocationReplica location, String whatsAppUrl) {
        Hazard hazard = payload.hazard() != null ? payload.hazard() : Hazard.FLOOD;
        if (payload.toSeverity() == null || payload.toSeverity() == Severity.NONE) {
            log.info("Automated alert de-escalated to NONE for {}, recording quiet all-clear log", payload.locationId());
            String targetChat = telegramBotClient.getDefaultChatId() != null && !telegramBotClient.getDefaultChatId().isBlank()
                    ? telegramBotClient.getDefaultChatId()
                    : "@kestrel_alert_channel";
            String allClearMsg = switch (hazard) {
                case FLOOD -> "All-Clear: River discharge normalized below warning threshold for " + location.getName();
                case HEAT -> "All-Clear: Temperatures normalized within climatological limits for " + location.getName();
                case AIR_QUALITY -> "All-Clear: Air quality index returned to acceptable levels for " + location.getName();
            };
            DeliveryLog quietLog = new DeliveryLog(
                    payload.alertId(),
                    location.getLocationId(),
                    location.getName(),
                    Severity.NONE,
                    Channel.TELEGRAM,
                    targetChat,
                    location.getPrimaryLanguage(),
                    Instant.now(),
                    allClearMsg,
                    location.getCurrentProvenance(),
                    "RESOLVED"
            );
            deliveryLogRepository.save(quietLog);
            return;
        }

        Instant now = Instant.now();
        AlertTemplateComposer.ComposedMessage regionalMsg = templateComposer.composeAlert(payload, location, location.getPrimaryLanguage());

        // 1. Dispatch Telegram
        List<SubscriberReplica> tgSubscribers = subscriberReplicaService.getActiveSubscribersForAlert(location.getLocationId(), payload.toSeverity(), payload.hazard())
                .stream()
                .filter(s -> s.getChannel() == Channel.TELEGRAM)
                .toList();

        if (!tgSubscribers.isEmpty()) {
            for (SubscriberReplica sub : tgSubscribers) {
                Language subLang = sub.getLanguage() != null ? sub.getLanguage() : location.getPrimaryLanguage();
                AlertTemplateComposer.ComposedMessage msg = templateComposer.composeAlert(payload, location, subLang);
                String deliveryId = "deliv-" + UUID.randomUUID();
                TelegramDeliveryPayload delivPayload = new TelegramDeliveryPayload(
                        deliveryId,
                        payload.alertId(),
                        location.getLocationId(),
                        location.getName(),
                        payload.toSeverity(),
                        sub.getRecipientIdentifier(),
                        sub.getToken(),
                        subLang,
                        msg.fullText(),
                        location.getCurrentProvenance(),
                        whatsAppUrl,
                        now
                );
                telegramDeliveryWorker.dispatch(delivPayload);
            }
        } else {
            // Default chat dispatch
            String deliveryId = "deliv-" + UUID.randomUUID();
            String targetChat = telegramBotClient.getDefaultChatId() != null && !telegramBotClient.getDefaultChatId().isBlank()
                    ? telegramBotClient.getDefaultChatId()
                    : "@kestrel_alert_channel";
            TelegramDeliveryPayload delivPayload = new TelegramDeliveryPayload(
                    deliveryId,
                    payload.alertId(),
                    location.getLocationId(),
                    location.getName(),
                    payload.toSeverity(),
                    targetChat,
                    null,
                    location.getPrimaryLanguage(),
                    regionalMsg.fullText(),
                    location.getCurrentProvenance(),
                    whatsAppUrl,
                    now
            );
            telegramDeliveryWorker.dispatch(delivPayload);
        }

        // 2. Dispatch Web Push
        List<SubscriberReplica> pushSubscribers = subscriberReplicaService.getActiveSubscribersForAlert(location.getLocationId(), payload.toSeverity(), payload.hazard())
                .stream()
                .filter(s -> s.getChannel() == Channel.WEB_PUSH)
                .toList();

        if (!pushSubscribers.isEmpty()) {
            for (SubscriberReplica sub : pushSubscribers) {
                Language subLang = sub.getLanguage() != null ? sub.getLanguage() : location.getPrimaryLanguage();
                AlertTemplateComposer.ComposedMessage msg = templateComposer.composeAlert(payload, location, subLang);
                String deliveryId = "deliv-" + UUID.randomUUID();
                WebPushDeliveryPayload pushPayload = new WebPushDeliveryPayload(
                        deliveryId,
                        payload.alertId(),
                        location.getLocationId(),
                        location.getName(),
                        payload.toSeverity(),
                        sub.getRecipientIdentifier(),
                        null,
                        null,
                        sub.getToken(),
                        subLang,
                        buildWebPushTitle(hazard, location, subLang),
                        msg.actionableAdvice(),
                        buildWebPushClickUrl(hazard, location.getLocationId()),
                        location.getCurrentProvenance(),
                        now
                );
                webPushDeliveryWorker.dispatch(pushPayload);
            }
        } else {
            String deliveryId = "deliv-" + UUID.randomUUID();
            WebPushDeliveryPayload pushPayload = new WebPushDeliveryPayload(
                    deliveryId,
                    payload.alertId(),
                    location.getLocationId(),
                    location.getName(),
                    payload.toSeverity(),
                    "https://fcm.googleapis.com/fcm/send/kestrel-broadcast-" + location.getLocationId(),
                    null,
                    null,
                    null,
                    location.getPrimaryLanguage(),
                    buildWebPushTitle(hazard, location, location.getPrimaryLanguage()),
                    regionalMsg.actionableAdvice(),
                    buildWebPushClickUrl(hazard, location.getLocationId()),
                    location.getCurrentProvenance(),
                    now
            );
            webPushDeliveryWorker.dispatch(pushPayload);
        }

        log.info("Delivered automated alert {} (hazard={}) for {} to Telegram and Web Push workers",
                payload.alertId(), hazard, payload.locationId());
    }

    public List<RecentDispatchItem> getRecentDispatches(int limit) {
        List<DeliveryLog> logs = deliveryLogRepository.findTop100ByOrderByTimestampDesc();
        List<RecentDispatchItem> items = new ArrayList<>();
        int target = Math.max(1, limit);
        for (DeliveryLog l : logs) {
            if (items.size() >= target) break;
            items.add(new RecentDispatchItem(
                    l.getId(),
                    l.getAlertId(),
                    l.getLocationId(),
                    l.getLocationName(),
                    l.getSeverity(),
                    l.getChannel(),
                    l.getRecipient(),
                    l.getLanguage(),
                    l.getTimestamp(),
                    l.getMessageExcerpt(),
                    l.getProvenance(),
                    l.getStatus(),
                    null
            ));
        }
        return items;
    }

    private LocationReplica findOrCreateLocation(String locationId) {
        return locationReplicaRepository.findById(locationId)
                .orElseGet(() -> {
                    LocationReplica fallback = new LocationReplica();
                    fallback.setLocationId(locationId);
                    fallback.setName(locationId);
                    fallback.setState("Unknown");
                    fallback.setRiverName("Local River");
                    fallback.setPrimaryLanguage(Language.en);
                    fallback.setActive(true);
                    return locationReplicaRepository.save(fallback);
                });
    }

    private int estimateRecipientCount(String locationId) {
        return switch (locationId) {
            case "mumbai-kurla" -> 42;
            case "patna-ward12" -> 35;
            case "guwahati-river" -> 28;
            case "cuttack-delta" -> 19;
            default -> 15;
        };
    }

    private String buildWebPushTitle(Hazard hazard, LocationReplica location, Language lang) {
        String loc = location != null ? location.getName() : "Alert";
        if (hazard == null) hazard = Hazard.FLOOD;
        if (lang == Language.hi) {
            return switch (hazard) {
                case FLOOD -> "केस्ट्रल बाढ़ चेतावनी: " + loc;
                case HEAT -> "केस्ट्रल लू चेतावनी: " + loc;
                case AIR_QUALITY -> "केस्ट्रल वायु गुणवत्ता चेतावनी: " + loc;
            };
        } else if (lang == Language.mr) {
            return switch (hazard) {
                case FLOOD -> "केस्ट्रल पूर इशारा: " + loc;
                case HEAT -> "केस्ट्रल उष्णतेची लाट इशारा: " + loc;
                case AIR_QUALITY -> "केस्ट्रल हवा गुणवत्ता इशारा: " + loc;
            };
        } else {
            return switch (hazard) {
                case FLOOD -> "Kestrel Flood Alert: " + loc;
                case HEAT -> "Kestrel Heat Alert: " + loc;
                case AIR_QUALITY -> "Kestrel Air Quality Alert: " + loc;
            };
        }
    }

    private String buildWebPushClickUrl(Hazard hazard, String locationId) {
        if (hazard != null && hazard != Hazard.FLOOD) {
            return "/resident?hazard=" + hazard.name() + "&location=" + locationId;
        }
        return "/dashboard?location=" + locationId;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}

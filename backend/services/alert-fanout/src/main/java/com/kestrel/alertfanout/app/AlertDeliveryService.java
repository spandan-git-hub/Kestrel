package com.kestrel.alertfanout.app;

import com.kestrel.alertfanout.api.dto.BroadcastPreviewRequest;
import com.kestrel.alertfanout.api.dto.BroadcastPreviewResponse;
import com.kestrel.alertfanout.api.dto.BroadcastRequest;
import com.kestrel.alertfanout.api.dto.BroadcastResponse;
import com.kestrel.alertfanout.infra.client.TelegramBotClient;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.repository.DeliveryLogRepository;
import com.kestrel.alertfanout.infra.repository.LocationReplicaRepository;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final MockDispatchQueue mockDispatchQueue;
    private final TelegramBotClient telegramBotClient;

    public AlertDeliveryService(
            LocationReplicaRepository locationReplicaRepository,
            DeliveryLogRepository deliveryLogRepository,
            AlertTemplateComposer templateComposer,
            MockDispatchQueue mockDispatchQueue,
            TelegramBotClient telegramBotClient) {
        this.locationReplicaRepository = locationReplicaRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.templateComposer = templateComposer;
        this.mockDispatchQueue = mockDispatchQueue;
        this.telegramBotClient = telegramBotClient;
    }

    public BroadcastPreviewResponse previewBroadcast(BroadcastPreviewRequest request) {
        LocationReplica location = findOrCreateLocation(request.locationId());
        Severity severity = request.severity() != null ? request.severity() : Severity.WARNING;
        Provenance provenance = location.getCurrentProvenance() != null ? location.getCurrentProvenance() : Provenance.MODEL_ONLY;
        LocalDate peakDate = LocalDate.now().plusDays(2);

        AlertTemplateComposer.ComposedMessage enMsg = templateComposer.composeAlert(location, severity, provenance, peakDate, Language.en);
        AlertTemplateComposer.ComposedMessage hiMsg = templateComposer.composeAlert(location, severity, provenance, peakDate, Language.hi);
        AlertTemplateComposer.ComposedMessage mrMsg = templateComposer.composeAlert(location, severity, provenance, peakDate, Language.mr);

        String whatsAppShareUrl = templateComposer.generateWhatsAppShareUrl(
                location, severity, provenance, peakDate, request.customNote());

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
        LocationReplica location = findOrCreateLocation(request.locationId());
        Severity severity = request.severity() != null ? request.severity() : Severity.WARNING;
        Provenance provenance = location.getCurrentProvenance() != null ? location.getCurrentProvenance() : Provenance.MODEL_ONLY;
        LocalDate peakDate = LocalDate.now().plusDays(2);
        String broadcastId = "bc-" + UUID.randomUUID();
        Instant now = Instant.now();

        AlertTemplateComposer.ComposedMessage enMsg = templateComposer.composeAlert(location, severity, provenance, peakDate, Language.en);
        AlertTemplateComposer.ComposedMessage regionalMsg = templateComposer.composeAlert(location, severity, provenance, peakDate, location.getPrimaryLanguage());

        String whatsAppShareUrl = templateComposer.generateWhatsAppShareUrl(
                location, severity, provenance, peakDate, request.customNote());

        List<Channel> channels = (request.channels() != null && !request.channels().isEmpty())
                ? request.channels()
                : List.of(Channel.TELEGRAM, Channel.MOCK);

        int dispatchedCount = 0;

        // 1. Telegram Dispatch
        if (channels.contains(Channel.TELEGRAM)) {
            String telegramText = regionalMsg.fullText();
            if (request.customNote() != null && !request.customNote().isBlank()) {
                telegramText += "\n\n📌 Coordinator Note: " + request.customNote().trim();
            }
            TelegramBotClient.TelegramDeliveryResult tgResult = telegramBotClient.sendMessage(null, telegramText);

            DeliveryLog tgLog = new DeliveryLog(
                    broadcastId,
                    location.getLocationId(),
                    location.getName(),
                    severity,
                    Channel.TELEGRAM,
                    telegramBotClient.isConfigured() ? "@kestrel_alert_channel" : "mock-telegram-subscriber",
                    location.getPrimaryLanguage(),
                    now,
                    truncate(telegramText, 1000),
                    provenance,
                    tgResult.status()
            );
            DeliveryLog savedTg = deliveryLogRepository.save(tgLog);
            mockDispatchQueue.enqueue(new MockDispatchQueue.DispatchItem(
                    savedTg.getId(),
                    savedTg.getAlertId(),
                    savedTg.getLocationId(),
                    savedTg.getLocationName(),
                    savedTg.getSeverity(),
                    savedTg.getChannel(),
                    savedTg.getRecipient(),
                    savedTg.getLanguage(),
                    savedTg.getTimestamp(),
                    savedTg.getMessageExcerpt(),
                    savedTg.getProvenance(),
                    savedTg.getStatus(),
                    whatsAppShareUrl
            ));
            dispatchedCount++;
        }

        // 2. Mock Queue Dispatches (English + Regional)
        if (channels.contains(Channel.MOCK)) {
            DeliveryLog mockLogEn = new DeliveryLog(
                    broadcastId,
                    location.getLocationId(),
                    location.getName(),
                    severity,
                    Channel.MOCK,
                    "mock-community-feed",
                    Language.en,
                    now,
                    truncate(enMsg.fullText(), 1000),
                    provenance,
                    "DELIVERED"
            );
            DeliveryLog savedMockEn = deliveryLogRepository.save(mockLogEn);
            mockDispatchQueue.enqueue(new MockDispatchQueue.DispatchItem(
                    savedMockEn.getId(),
                    savedMockEn.getAlertId(),
                    savedMockEn.getLocationId(),
                    savedMockEn.getLocationName(),
                    savedMockEn.getSeverity(),
                    savedMockEn.getChannel(),
                    savedMockEn.getRecipient(),
                    savedMockEn.getLanguage(),
                    savedMockEn.getTimestamp(),
                    savedMockEn.getMessageExcerpt(),
                    savedMockEn.getProvenance(),
                    savedMockEn.getStatus(),
                    whatsAppShareUrl
            ));
            dispatchedCount++;

            if (location.getPrimaryLanguage() != Language.en) {
                DeliveryLog mockLogReg = new DeliveryLog(
                        broadcastId,
                        location.getLocationId(),
                        location.getName(),
                        severity,
                        Channel.MOCK,
                        "mock-regional-feed",
                        location.getPrimaryLanguage(),
                        now,
                        truncate(regionalMsg.fullText(), 1000),
                        provenance,
                        "DELIVERED"
                );
                DeliveryLog savedMockReg = deliveryLogRepository.save(mockLogReg);
                mockDispatchQueue.enqueue(new MockDispatchQueue.DispatchItem(
                        savedMockReg.getId(),
                        savedMockReg.getAlertId(),
                        savedMockReg.getLocationId(),
                        savedMockReg.getLocationName(),
                        savedMockReg.getSeverity(),
                        savedMockReg.getChannel(),
                        savedMockReg.getRecipient(),
                        savedMockReg.getLanguage(),
                        savedMockReg.getTimestamp(),
                        savedMockReg.getMessageExcerpt(),
                        savedMockReg.getProvenance(),
                        savedMockReg.getStatus(),
                        whatsAppShareUrl
                ));
                dispatchedCount++;
            }
        }

        log.info("Broadcast {} dispatched for location {} across {} channels (count={})",
                broadcastId, location.getLocationId(), channels, dispatchedCount);

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
        if (payload.toSeverity() == null || payload.toSeverity() == Severity.NONE) {
            log.info("Automated alert de-escalated to NONE for {}, recording quiet all-clear log", payload.locationId());
            DeliveryLog quietLog = new DeliveryLog(
                    payload.alertId(),
                    location.getLocationId(),
                    location.getName(),
                    Severity.NONE,
                    Channel.MOCK,
                    "mock-community-feed",
                    location.getPrimaryLanguage(),
                    Instant.now(),
                    "All-Clear: River discharge normalized below warning threshold for " + location.getName(),
                    location.getCurrentProvenance(),
                    "RESOLVED"
            );
            DeliveryLog saved = deliveryLogRepository.save(quietLog);
            mockDispatchQueue.enqueue(new MockDispatchQueue.DispatchItem(
                    saved.getId(),
                    saved.getAlertId(),
                    saved.getLocationId(),
                    saved.getLocationName(),
                    saved.getSeverity(),
                    saved.getChannel(),
                    saved.getRecipient(),
                    saved.getLanguage(),
                    saved.getTimestamp(),
                    saved.getMessageExcerpt(),
                    saved.getProvenance(),
                    saved.getStatus(),
                    whatsAppUrl
            ));
            return;
        }

        Instant now = Instant.now();
        AlertTemplateComposer.ComposedMessage regionalMsg = templateComposer.composeAlert(payload, location, location.getPrimaryLanguage());

        // Dispatch Telegram
        TelegramBotClient.TelegramDeliveryResult tgResult = telegramBotClient.sendMessage(null, regionalMsg.fullText());
        DeliveryLog tgLog = new DeliveryLog(
                payload.alertId(),
                location.getLocationId(),
                location.getName(),
                payload.toSeverity(),
                Channel.TELEGRAM,
                telegramBotClient.isConfigured() ? "@kestrel_alert_channel" : "mock-telegram-subscriber",
                location.getPrimaryLanguage(),
                now,
                truncate(regionalMsg.fullText(), 1000),
                location.getCurrentProvenance(),
                tgResult.status()
        );
        DeliveryLog savedTg = deliveryLogRepository.save(tgLog);
        mockDispatchQueue.enqueue(new MockDispatchQueue.DispatchItem(
                savedTg.getId(),
                savedTg.getAlertId(),
                savedTg.getLocationId(),
                savedTg.getLocationName(),
                savedTg.getSeverity(),
                savedTg.getChannel(),
                savedTg.getRecipient(),
                savedTg.getLanguage(),
                savedTg.getTimestamp(),
                savedTg.getMessageExcerpt(),
                savedTg.getProvenance(),
                savedTg.getStatus(),
                whatsAppUrl
        ));

        // Dispatch Mock Feed
        DeliveryLog mockLog = new DeliveryLog(
                payload.alertId(),
                location.getLocationId(),
                location.getName(),
                payload.toSeverity(),
                Channel.MOCK,
                "mock-community-feed",
                location.getPrimaryLanguage(),
                now,
                truncate(regionalMsg.fullText(), 1000),
                location.getCurrentProvenance(),
                "DELIVERED"
        );
        DeliveryLog savedMock = deliveryLogRepository.save(mockLog);
        mockDispatchQueue.enqueue(new MockDispatchQueue.DispatchItem(
                savedMock.getId(),
                savedMock.getAlertId(),
                savedMock.getLocationId(),
                savedMock.getLocationName(),
                savedMock.getSeverity(),
                savedMock.getChannel(),
                savedMock.getRecipient(),
                savedMock.getLanguage(),
                savedMock.getTimestamp(),
                savedMock.getMessageExcerpt(),
                savedMock.getProvenance(),
                savedMock.getStatus(),
                whatsAppUrl
        ));

        log.info("Delivered automated alert {} for {} to Telegram and Mock queues", payload.alertId(), payload.locationId());
    }

    public List<MockDispatchQueue.DispatchItem> getRecentDispatches(int limit) {
        if (mockDispatchQueue.size() > 0) {
            return mockDispatchQueue.getRecent(limit);
        }
        // Fallback to database
        List<DeliveryLog> logs = deliveryLogRepository.findTop100ByOrderByTimestampDesc();
        List<MockDispatchQueue.DispatchItem> items = new ArrayList<>();
        int count = 0;
        for (DeliveryLog l : logs) {
            if (count >= limit) break;
            items.add(new MockDispatchQueue.DispatchItem(
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
            count++;
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

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}

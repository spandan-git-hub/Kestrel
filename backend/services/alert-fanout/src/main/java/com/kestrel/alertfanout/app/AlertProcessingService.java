package com.kestrel.alertfanout.app;

import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.repository.ActiveAlertRepository;
import com.kestrel.alertfanout.infra.repository.LocationReplicaRepository;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AlertProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AlertProcessingService.class);

    private final ActiveAlertRepository activeAlertRepository;
    private final LocationReplicaRepository replicaRepository;
    private final AlertTemplateComposer templateComposer;

    public AlertProcessingService(
            ActiveAlertRepository activeAlertRepository,
            LocationReplicaRepository replicaRepository,
            AlertTemplateComposer templateComposer) {
        this.activeAlertRepository = activeAlertRepository;
        this.replicaRepository = replicaRepository;
        this.templateComposer = templateComposer;
    }

    @Transactional
    public ActiveAlertEntity processAlertEvent(AlertEventPayload payload) {
        log.info("Processing AlertEvent: alertId={}, locationId={}, stage={}, {} -> {}, autoSend={}",
                payload.alertId(), payload.locationId(), payload.stage(),
                payload.fromSeverity(), payload.toSeverity(), payload.autoSend());

        LocationReplica location = replicaRepository.findById(payload.locationId())
                .orElseGet(() -> {
                    log.warn("LocationReplica not found for {}, creating default fallback", payload.locationId());
                    LocationReplica fallback = new LocationReplica();
                    fallback.setLocationId(payload.locationId());
                    fallback.setName(payload.locationId());
                    fallback.setState("Unknown");
                    fallback.setRiverName("Local River");
                    fallback.setPrimaryLanguage(Language.en);
                    fallback.setActive(true);
                    return replicaRepository.save(fallback);
                });

        AlertTemplateComposer.ComposedMessage enMsg = templateComposer.composeAlert(payload, location, Language.en);
        AlertTemplateComposer.ComposedMessage hiMsg = templateComposer.composeAlert(payload, location, Language.hi);
        AlertTemplateComposer.ComposedMessage mrMsg = templateComposer.composeAlert(payload, location, Language.mr);
        String whatsAppUrl = templateComposer.generateWhatsAppShareUrl(payload, location);

        ActiveAlertEntity entity = activeAlertRepository.findByLocationId(payload.locationId())
                .orElse(new ActiveAlertEntity());

        entity.setLocationId(payload.locationId());
        entity.setAlertId(payload.alertId());
        entity.setHazard(payload.hazard());
        entity.setStage(payload.stage());
        entity.setFromSeverity(payload.fromSeverity());
        entity.setToSeverity(payload.toSeverity());
        entity.setProvenance(location.getCurrentProvenance() != null ? location.getCurrentProvenance() : payload.provenance());
        entity.setFirstExceedanceDate(payload.firstExceedanceDate());
        entity.setPeakShare(payload.peakShare());
        entity.setAutoSend(payload.autoSend());
        entity.setComposedEnglish(enMsg.fullText());
        entity.setComposedHindi(hiMsg.fullText());
        entity.setComposedMarathi(mrMsg.fullText());
        entity.setWhatsAppUrl(whatsAppUrl);
        entity.setReceivedAt(Instant.now());

        ActiveAlertEntity saved = activeAlertRepository.save(entity);
        log.info("Persisted ActiveAlert for locationId={} with severity={}", payload.locationId(), payload.toSeverity());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<ActiveAlertEntity> getActiveAlert(String locationId) {
        return activeAlertRepository.findByLocationId(locationId);
    }
}

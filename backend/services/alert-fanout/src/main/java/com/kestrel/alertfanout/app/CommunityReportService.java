package com.kestrel.alertfanout.app;

import com.kestrel.alertfanout.api.dto.CreateReportRequest;
import com.kestrel.alertfanout.api.dto.ReportResponse;
import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;
import com.kestrel.alertfanout.infra.entity.CommunityReport;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.repository.ActiveAlertRepository;
import com.kestrel.alertfanout.infra.repository.CommunityReportRepository;
import com.kestrel.alertfanout.infra.repository.LocationReplicaRepository;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.ProvenanceUpdatePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CommunityReportService {

    private static final Logger log = LoggerFactory.getLogger(CommunityReportService.class);

    private final CommunityReportRepository communityReportRepository;
    private final LocationReplicaRepository locationReplicaRepository;
    private final ActiveAlertRepository activeAlertRepository;
    private final AlertTemplateComposer templateComposer;
    private final ApplicationEventPublisher eventPublisher;
    private final RestClient restClient;
    private final String rulesEngineUrl;

    public CommunityReportService(
            CommunityReportRepository communityReportRepository,
            LocationReplicaRepository locationReplicaRepository,
            ActiveAlertRepository activeAlertRepository,
            AlertTemplateComposer templateComposer,
            ApplicationEventPublisher eventPublisher,
            @Value("${kestrel.rules-engine.url:http://localhost:8083}") String rulesEngineUrl) {
        this.communityReportRepository = communityReportRepository;
        this.locationReplicaRepository = locationReplicaRepository;
        this.activeAlertRepository = activeAlertRepository;
        this.templateComposer = templateComposer;
        this.eventPublisher = eventPublisher;
        this.rulesEngineUrl = rulesEngineUrl;
        this.restClient = RestClient.builder().baseUrl(rulesEngineUrl).build();
    }

    @Transactional
    public ReportResponse recordCommunityReport(CreateReportRequest request) {
        log.info("Recording community ground report for locationId={}, category={}",
                request.locationId(), request.category());

        LocationReplica location = locationReplicaRepository.findById(request.locationId())
                .orElseGet(() -> {
                    LocationReplica fallback = new LocationReplica();
                    fallback.setLocationId(request.locationId());
                    fallback.setName(request.locationId());
                    fallback.setState("Unknown");
                    fallback.setRiverName("Local River");
                    fallback.setPrimaryLanguage(Language.en);
                    fallback.setActive(true);
                    return locationReplicaRepository.save(fallback);
                });

        // 1. Save community ground report
        CommunityReport report = new CommunityReport(
                request.locationId(),
                request.category(),
                request.description(),
                request.reporterAlias()
        );
        CommunityReport savedReport = communityReportRepository.save(report);

        // 2. Upgrade location replica provenance
        location.setCurrentProvenance(Provenance.MODEL_PLUS_REPORTS);
        location.setReportCount(location.getReportCount() + 1);
        location.setLastReportedAt(Instant.now());
        LocationReplica savedLocation = locationReplicaRepository.save(location);

        // 3. Upgrade active alert provenance and recompose localized alert text
        activeAlertRepository.findByLocationId(request.locationId()).ifPresent(activeAlert -> {
            activeAlert.setProvenance(Provenance.MODEL_PLUS_REPORTS);

            AlertTemplateComposer.ComposedMessage enMsg = templateComposer.composeAlert(
                    savedLocation, activeAlert.getToSeverity(), Provenance.MODEL_PLUS_REPORTS,
                    activeAlert.getFirstExceedanceDate(), Language.en);
            AlertTemplateComposer.ComposedMessage hiMsg = templateComposer.composeAlert(
                    savedLocation, activeAlert.getToSeverity(), Provenance.MODEL_PLUS_REPORTS,
                    activeAlert.getFirstExceedanceDate(), Language.hi);
            AlertTemplateComposer.ComposedMessage mrMsg = templateComposer.composeAlert(
                    savedLocation, activeAlert.getToSeverity(), Provenance.MODEL_PLUS_REPORTS,
                    activeAlert.getFirstExceedanceDate(), Language.mr);
            String whatsAppUrl = templateComposer.generateWhatsAppShareUrl(
                    savedLocation, activeAlert.getToSeverity(), Provenance.MODEL_PLUS_REPORTS,
                    activeAlert.getFirstExceedanceDate(), null);

            activeAlert.setComposedEnglish(enMsg.fullText());
            activeAlert.setComposedHindi(hiMsg.fullText());
            activeAlert.setComposedMarathi(mrMsg.fullText());
            activeAlert.setWhatsAppUrl(whatsAppUrl);
            activeAlertRepository.save(activeAlert);
            log.info("Recomposed and upgraded ActiveAlert for {} to MODEL_PLUS_REPORTS", request.locationId());
        });

        // 4. Publish ProvenanceUpdatePayload via local Spring ApplicationEvent
        ProvenanceUpdatePayload updatePayload = new ProvenanceUpdatePayload(
                request.locationId(),
                Provenance.MODEL_PLUS_REPORTS,
                savedLocation.getReportCount(),
                Instant.now()
        );

        EventEnvelope<ProvenanceUpdatePayload> envelope = new EventEnvelope<>(
                "prov-" + UUID.randomUUID(),
                "ProvenanceUpdated",
                1,
                Instant.now(),
                "alert-fanout",
                null,
                updatePayload
        );
        try {
            eventPublisher.publishEvent(envelope);
        } catch (Exception ex) {
            log.warn("Local event publishing failed: {}", ex.getMessage());
        }

        // 5. Notify rules-engine over HTTP to update its active alert status
        notifyRulesEngineHttp(envelope);

        return new ReportResponse(
                savedReport.getId(),
                savedReport.getLocationId(),
                savedReport.getCategory(),
                Provenance.MODEL_PLUS_REPORTS,
                "Ground report recorded. Provenance upgraded to MODEL_PLUS_REPORTS.",
                savedReport.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CommunityReport> getReportsForLocation(String locationId) {
        return communityReportRepository.findByLocationIdOrderByCreatedAtDesc(locationId);
    }

    @Transactional(readOnly = true)
    public List<CommunityReport> getRecentReports() {
        return communityReportRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private void notifyRulesEngineHttp(EventEnvelope<ProvenanceUpdatePayload> envelope) {
        try {
            restClient.post()
                    .uri("/api/v1/rules/events/provenance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(envelope)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully notified rules-engine of provenance upgrade for {}", envelope.payload().locationId());
        } catch (Exception ex) {
            log.debug("HTTP notification to rules-engine skipped or failed (rules-engine may not be running locally): {}",
                    ex.getMessage());
        }
    }
}

package com.kestrel.alertfanout.api;

import com.kestrel.alertfanout.api.dto.BroadcastPreviewRequest;
import com.kestrel.alertfanout.api.dto.BroadcastPreviewResponse;
import com.kestrel.alertfanout.api.dto.BroadcastRequest;
import com.kestrel.alertfanout.api.dto.BroadcastResponse;
import com.kestrel.alertfanout.api.dto.CreateReportRequest;
import com.kestrel.alertfanout.api.dto.RecentDispatchItem;
import com.kestrel.alertfanout.api.dto.ReportResponse;
import com.kestrel.alertfanout.app.AlertDeliveryService;
import com.kestrel.alertfanout.app.AlertProcessingService;
import com.kestrel.alertfanout.app.CommunityReportService;
import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;
import com.kestrel.alertfanout.infra.entity.CommunityReport;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fanout")
public class FanoutController {

    private static final Logger log = LoggerFactory.getLogger(FanoutController.class);

    private final AlertDeliveryService alertDeliveryService;
    private final CommunityReportService communityReportService;
    private final AlertProcessingService alertProcessingService;
    private final com.kestrel.alertfanout.infra.limiter.TelegramRateLimiter telegramRateLimiter;

    public FanoutController(
            AlertDeliveryService alertDeliveryService,
            CommunityReportService communityReportService,
            AlertProcessingService alertProcessingService,
            com.kestrel.alertfanout.infra.limiter.TelegramRateLimiter telegramRateLimiter) {
        this.alertDeliveryService = alertDeliveryService;
        this.communityReportService = communityReportService;
        this.alertProcessingService = alertProcessingService;
        this.telegramRateLimiter = telegramRateLimiter;
    }

    @GetMapping("/rate-limits/telegram")
    public ResponseEntity<com.kestrel.alertfanout.infra.limiter.TelegramRateLimiter.RateLimitStatus> getTelegramRateLimitStatus() {
        return ResponseEntity.ok(telegramRateLimiter.getStatus());
    }

    @PostMapping("/broadcast/preview")
    public ResponseEntity<BroadcastPreviewResponse> previewBroadcast(
            @Valid @RequestBody BroadcastPreviewRequest request) {
        log.info("Generating broadcast preview for location={}", request.locationId());
        BroadcastPreviewResponse response = alertDeliveryService.previewBroadcast(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/broadcast")
    public ResponseEntity<BroadcastResponse> sendBroadcast(
            @Valid @RequestBody BroadcastRequest request) {
        log.info("Sending broadcast alert for location={}, severity={}", request.locationId(), request.severity());
        BroadcastResponse response = alertDeliveryService.executeBroadcast(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentDispatchItem>> getRecentDispatches(
            @RequestParam(defaultValue = "50") int limit) {
        List<RecentDispatchItem> items = alertDeliveryService.getRecentDispatches(limit);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ReportResponse> submitGroundReport(
            @Valid @RequestBody CreateReportRequest request) {
        log.info("Received ground report for location={}", request.locationId());
        ReportResponse response = communityReportService.recordCommunityReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reports/{locationId}")
    public ResponseEntity<List<CommunityReport>> getReportsForLocation(
            @PathVariable String locationId) {
        return ResponseEntity.ok(communityReportService.getReportsForLocation(locationId));
    }

    @GetMapping("/active/{locationId}")
    public ResponseEntity<ActiveAlertEntity> getActiveAlert(
            @PathVariable String locationId) {
        return alertProcessingService.getActiveAlert(locationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

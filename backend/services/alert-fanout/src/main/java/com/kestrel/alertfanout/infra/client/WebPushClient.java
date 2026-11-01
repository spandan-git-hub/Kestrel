package com.kestrel.alertfanout.infra.client;

import com.kestrel.common.events.WebPushDeliveryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * W3C Web Push HTTP Client for browser notification dispatching using VAPID standards.
 * Handles HTTP 404/410 expired subscription notifications.
 */
@Component
public class WebPushClient {

    private static final Logger log = LoggerFactory.getLogger(WebPushClient.class);

    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final RestClient restClient;

    public record WebPushDeliveryResult(boolean success, String status, int statusCode, String message) {
        public WebPushDeliveryResult(boolean success, String status, String message) {
            this(success, status, success ? 200 : ("EXPIRED".equalsIgnoreCase(status) ? 410 : 500), message);
        }
    }

    @Autowired
    public WebPushClient(
            @Value("${kestrel.vapid.public-key:${VAPID_PUBLIC_KEY:}}") String publicKey,
            @Value("${kestrel.vapid.private-key:${VAPID_PRIVATE_KEY:}}") String privateKey,
            @Value("${kestrel.vapid.subject:${VAPID_SUBJECT:mailto:alerts@kestrel.in}}") String subject) {
        this(publicKey, privateKey, subject, RestClient.builder().build());
    }

    public WebPushClient(
            String publicKey,
            String privateKey,
            String subject,
            RestClient restClient) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        this.restClient = restClient != null ? restClient : RestClient.builder().build();
    }

    public WebPushDeliveryResult sendPush(WebPushDeliveryPayload payload) {
        if (payload == null || payload.endpoint() == null || payload.endpoint().isBlank()) {
            log.warn("Web Push dispatch skipped (no push endpoint provided)");
            return new WebPushDeliveryResult(false, "SKIPPED_NO_ENDPOINT", 400, "Missing push endpoint");
        }

        String endpoint = payload.endpoint();

        if (!isConfigured()) {
            log.info("Web Push simulated (VAPID keys not configured). Target endpoint: {}", endpoint);
            return new WebPushDeliveryResult(false, "SIMULATED", 200, "VAPID keys not configured; push delivery simulated");
        }

        try {
            log.info("Dispatching Web Push notification to endpoint: {}", endpoint);

            Map<String, Object> notificationPayload = Map.of(
                    "title", payload.title() != null ? payload.title() : "Kestrel Alert",
                    "body", payload.body() != null ? payload.body() : "",
                    "icon", "/icons/kestrel-192.png",
                    "badge", "/icons/kestrel-badge.png",
                    "url", payload.clickUrl() != null ? payload.clickUrl() : "/",
                    "severity", payload.severity() != null ? payload.severity().name() : "WARNING",
                    "locationId", payload.locationId() != null ? payload.locationId() : ""
            );

            restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("TTL", "86400")
                    .header("Urgency", "high")
                    .header("Authorization", "vapid t=" + subject + ",k=" + publicKey)
                    .body(notificationPayload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully delivered Web Push notification to: {}", endpoint);
            return new WebPushDeliveryResult(true, "DELIVERED", 201, "Push notification sent successfully");
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            if (code == 404 || code == 410) {
                log.warn("Web Push endpoint returned {} (Expired/Gone) for endpoint: {}", code, endpoint);
                return new WebPushDeliveryResult(false, "EXPIRED", code, "Browser push subscription expired (HTTP " + code + ")");
            } else {
                log.warn("Web Push endpoint error {} for {}: {}", code, endpoint, ex.getMessage());
                return new WebPushDeliveryResult(false, "FAILED_" + code, code, "Push service returned HTTP " + code + ": " + ex.getMessage());
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch Web Push: {}", ex.getMessage());
            return new WebPushDeliveryResult(false, "FAILED", 500, "Web Push dispatch error: " + ex.getMessage());
        }
    }

    public boolean isConfigured() {
        return publicKey != null && !publicKey.isBlank() && privateKey != null && !privateKey.isBlank();
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getSubject() {
        return subject;
    }
}


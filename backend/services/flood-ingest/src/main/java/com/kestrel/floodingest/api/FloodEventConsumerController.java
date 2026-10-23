package com.kestrel.floodingest.api;

import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.floodingest.app.FloodForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flood/events")
public class FloodEventConsumerController {

    private static final Logger log = LoggerFactory.getLogger(FloodEventConsumerController.class);

    private final FloodForecastService forecastService;

    public FloodEventConsumerController(FloodForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @PostMapping("/location-upserted")
    public ResponseEntity<Void> onLocationUpserted(
            @RequestBody EventEnvelope<LocationUpsertedPayload> envelope) {
        log.info("Received LocationUpserted event: eventId={}, location={}",
                envelope.eventId(), envelope.payload().locationId());
        forecastService.upsertLocationReplica(envelope.payload());
        return ResponseEntity.ok().build();
    }

    @org.springframework.context.event.EventListener
    public void onLocationUpsertedLocal(EventEnvelope<?> envelope) {
        if ("LocationUpserted".equals(envelope.eventType()) && envelope.payload() instanceof LocationUpsertedPayload payload) {
            log.info("Received LocationUpserted via local ApplicationEvent: eventId={}, location={}",
                    envelope.eventId(), payload.locationId());
            forecastService.upsertLocationReplica(payload);
        }
    }
}

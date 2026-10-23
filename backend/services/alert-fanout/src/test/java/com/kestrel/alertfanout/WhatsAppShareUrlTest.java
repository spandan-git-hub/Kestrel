package com.kestrel.alertfanout;

import com.kestrel.alertfanout.app.AlertTemplateComposer;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppShareUrlTest {

    private AlertTemplateComposer composer;
    private LocationReplica mumbaiLocation;

    @BeforeEach
    void setUp() {
        composer = new AlertTemplateComposer();
        mumbaiLocation = new LocationReplica(
                "mumbai-kurla",
                "Kurla Ward L, Mumbai",
                "Maharashtra",
                19.0726,
                72.8797,
                "Mithi River",
                45.0,
                85.0,
                140.0,
                Language.mr,
                true
        );
    }

    @Test
    @DisplayName("Generate WhatsApp share URL with valid UTF-8 encoding and bilingual text")
    void testGenerateWhatsAppShareUrl() {
        AlertEventPayload payload = new AlertEventPayload(
                "alert-010",
                "mumbai-kurla",
                Hazard.FLOOD,
                "DECISION",
                Severity.NONE,
                Severity.WARNING,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 10, 24),
                0.68,
                true
        );

        String url = composer.generateWhatsAppShareUrl(payload, mumbaiLocation);

        assertThat(url).startsWith("https://wa.me/?text=");

        String encodedQuery = url.substring("https://wa.me/?text=".length());
        String decoded = URLDecoder.decode(encodedQuery, StandardCharsets.UTF_8);

        // Verify decoded text contains header, English section, Marathi section, and provenance note
        assertThat(decoded).contains("KESTREL FLOOD ALERT");
        assertThat(decoded).contains("Location: Kurla Ward L, Mumbai");
        assertThat(decoded).contains("English:");
        assertThat(decoded).contains("मराठी:");
        assertThat(decoded).contains("पूर इशारा");
        assertThat(decoded).contains("Provenance: Model Guidance Only — Not an official warning. Confirm locally.");
    }
}

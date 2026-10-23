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

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTemplateComposerTest {

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
    @DisplayName("Compose English WARNING alert with mandatory provenance disclaimer")
    void testComposeEnglishWarningAlert() {
        AlertEventPayload payload = new AlertEventPayload(
                "alert-001",
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

        AlertTemplateComposer.ComposedMessage message = composer.composeAlert(payload, mumbaiLocation, Language.en);

        assertThat(message.title()).contains("⚠️ KESTREL FLOOD WARNING: Kurla Ward L, Mumbai");
        assertThat(message.body()).contains("dangerous levels within the next 48 hours");
        assertThat(message.body()).contains("24 Oct 2026");
        assertThat(message.actionableAdvice()).contains("Move livestock and valuables to upper floors");
        assertThat(message.provenanceNote()).contains("Model Guidance Only — Not an official warning. Confirm locally.");
        assertThat(message.fullText()).contains("Provenance: Model Guidance Only — Not an official warning. Confirm locally.");
    }

    @Test
    @DisplayName("Compose Hindi WARNING alert with accurate Devanagari 'केस्ट्रल' transliteration")
    void testComposeHindiWarningAlert() {
        AlertEventPayload payload = new AlertEventPayload(
                "alert-002",
                "patna-ward12",
                Hazard.FLOOD,
                "DECISION",
                Severity.WATCH,
                Severity.WARNING,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 10, 25),
                0.72,
                true
        );

        LocationReplica patnaLocation = new LocationReplica(
                "patna-ward12",
                "Ward 12, Patna",
                "Bihar",
                25.6127,
                85.1589,
                "Ganges River",
                38000.0,
                48000.0,
                62000.0,
                Language.hi,
                true
        );

        AlertTemplateComposer.ComposedMessage message = composer.composeAlert(payload, patnaLocation, Language.hi);

        assertThat(message.title()).contains("⚠️ केस्ट्रल बाढ़ चेतावनी: Ward 12, Patna");
        assertThat(message.body()).contains("खतरनाक सीमा");
        assertThat(message.body()).contains("25 Oct 2026");
        assertThat(message.actionableAdvice()).contains("पशुओं और कीमती सामान को ऊंचे स्थानों पर पहुंचाएं");
        assertThat(message.provenanceNote()).contains("केवल मॉडल अनुमान — यह आधिकारिक चेतावनी नहीं है।");
        assertThat(message.fullText()).contains("स्रोत: केवल मॉडल अनुमान");
    }

    @Test
    @DisplayName("Compose Marathi WARNING alert with accurate Devanagari 'केस्ट्रल' transliteration")
    void testComposeMarathiWarningAlert() {
        AlertEventPayload payload = new AlertEventPayload(
                "alert-003",
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

        AlertTemplateComposer.ComposedMessage message = composer.composeAlert(payload, mumbaiLocation, Language.mr);

        assertThat(message.title()).contains("⚠️ केस्ट्रल पूर इशारा: Kurla Ward L, Mumbai");
        assertThat(message.body()).contains("धोकादायक पातळी");
        assertThat(message.actionableAdvice()).contains("जनावरे व महत्त्वाची सामग्री सुरक्षित किंवा उंच जागी हलवा");
        assertThat(message.provenanceNote()).contains("केवळ मॉडेल अंदाज — हा अधिकृत इशारा नाही.");
        assertThat(message.fullText()).contains("माहिती स्रोत: केवळ मॉडेल अंदाज");
    }

    @Test
    @DisplayName("Upgraded provenance MODEL_PLUS_REPORTS is reflected across all languages")
    void testUpgradedProvenanceAcrossLanguages() {
        mumbaiLocation.setCurrentProvenance(Provenance.MODEL_PLUS_REPORTS);

        AlertEventPayload payload = new AlertEventPayload(
                "alert-004",
                "mumbai-kurla",
                Hazard.FLOOD,
                "DECISION",
                Severity.WARNING,
                Severity.WARNING,
                Provenance.MODEL_PLUS_REPORTS,
                Instant.now(),
                LocalDate.of(2026, 10, 24),
                0.68,
                false
        );

        AlertTemplateComposer.ComposedMessage en = composer.composeAlert(payload, mumbaiLocation, Language.en);
        AlertTemplateComposer.ComposedMessage hi = composer.composeAlert(payload, mumbaiLocation, Language.hi);
        AlertTemplateComposer.ComposedMessage mr = composer.composeAlert(payload, mumbaiLocation, Language.mr);

        assertThat(en.provenanceNote()).contains("Model Guidance + Community Reports");
        assertThat(hi.provenanceNote()).contains("मॉडल अनुमान + स्थानीय जन रिपोर्ट");
        assertThat(mr.provenanceNote()).contains("मॉडेल अंदाज + स्थानिक नागरिकांचे अहवाल");
    }

    @Test
    @DisplayName("Compose SEVERE emergency alert has urgency across languages")
    void testComposeSevereAlert() {
        AlertEventPayload payload = new AlertEventPayload(
                "alert-005",
                "mumbai-kurla",
                Hazard.FLOOD,
                "DECISION",
                Severity.WARNING,
                Severity.SEVERE,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 10, 24),
                0.85,
                true
        );

        AlertTemplateComposer.ComposedMessage en = composer.composeAlert(payload, mumbaiLocation, Language.en);
        AlertTemplateComposer.ComposedMessage mr = composer.composeAlert(payload, mumbaiLocation, Language.mr);

        assertThat(en.title()).contains("🚨 KESTREL SEVERE FLOOD EMERGENCY");
        assertThat(en.actionableAdvice()).contains("Evacuate low-lying areas immediately");

        assertThat(mr.title()).contains("🚨 केस्ट्रल अतिगंभीर पूर आणीबाणी");
        assertThat(mr.actionableAdvice()).contains("सखल भागातील जागा त्वरित रिकामी करा");
    }
}

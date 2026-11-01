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

    @Test
    @DisplayName("PT-FANOUT-MH-01: Compose Heat WARNING alert in English and Hindi with mandatory provenance")
    void testComposeHeatWarningAlertEnglishAndHindi() {
        LocationReplica delhiLocation = new LocationReplica(
                "delhi-central",
                "Delhi Central",
                "Delhi",
                28.6139,
                77.2090,
                null,
                0, 0, 0,
                Language.hi,
                true
        );

        AlertEventPayload payload = new AlertEventPayload(
                "alert-heat-001",
                "delhi-central",
                Hazard.HEAT,
                "DECISION",
                Severity.WATCH,
                Severity.WARNING,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 11, 2),
                0.80,
                true
        );

        AlertTemplateComposer.ComposedMessage en = composer.composeAlert(payload, delhiLocation, Language.en);
        AlertTemplateComposer.ComposedMessage hi = composer.composeAlert(payload, delhiLocation, Language.hi);

        assertThat(en.title()).contains("⚠️ KESTREL HEAT WAVE WARNING: Delhi Central");
        assertThat(en.body()).contains("Heat wave conditions likely on consecutive days");
        assertThat(en.actionableAdvice()).contains("Avoid going out between 12 noon and 3 PM; drink water frequently");
        assertThat(en.provenanceNote()).contains("Guidance based on numerical model forecasts. Please verify locally before taking emergency action. Not an official government decree.");

        assertThat(hi.title()).contains("⚠️ केस्ट्रल लू चेतावनी: Delhi Central");
        assertThat(hi.actionableAdvice()).contains("दोपहर 12 बजे से 3 बजे के बीच धूप में निकलने से बचें; पर्याप्त पानी पिएं");
        assertThat(hi.provenanceNote()).contains("संख्यात्मक मॉडल पूर्वानुमान पर आधारित मार्गदर्शन");
    }

    @Test
    @DisplayName("PT-FANOUT-MH-01: Compose Heat SEVERE emergency alert in English and Hindi")
    void testComposeHeatSevereAlert() {
        LocationReplica delhiLocation = new LocationReplica(
                "delhi-central",
                "Delhi Central",
                "Delhi",
                28.6139,
                77.2090,
                null,
                0, 0, 0,
                Language.hi,
                true
        );

        AlertEventPayload payload = new AlertEventPayload(
                "alert-heat-002",
                "delhi-central",
                Hazard.HEAT,
                "DECISION",
                Severity.WARNING,
                Severity.SEVERE,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 11, 2),
                0.90,
                true
        );

        AlertTemplateComposer.ComposedMessage en = composer.composeAlert(payload, delhiLocation, Language.en);
        AlertTemplateComposer.ComposedMessage hi = composer.composeAlert(payload, delhiLocation, Language.hi);

        assertThat(en.title()).contains("🚨 KESTREL SEVERE HEAT WAVE EMERGENCY: Delhi Central");
        assertThat(en.actionableAdvice()).contains("Stay indoors during hottest hours and seek medical help if someone feels dizzy or faint");

        assertThat(hi.title()).contains("🚨 केस्ट्रल भीषण लू आपातकाल: Delhi Central");
        assertThat(hi.actionableAdvice()).contains("भीषण लू की संभावना। सबसे गर्म घंटों में घर के अंदर रहें और चक्कर या बेहोशी महसूस होने पर तत्काल डॉक्टर से संपर्क करें");
    }

    @Test
    @DisplayName("PT-FANOUT-MH-01: Compose Air Quality WARNING alert in English and Hindi with N95 advice")
    void testComposeAirQualityWarningAlert() {
        LocationReplica anandVihar = new LocationReplica(
                "delhi-anand-vihar",
                "Anand Vihar, Delhi",
                "Delhi",
                28.6469,
                77.3160,
                null,
                0, 0, 0,
                Language.hi,
                true
        );

        AlertEventPayload payload = new AlertEventPayload(
                "alert-aq-001",
                "delhi-anand-vihar",
                Hazard.AIR_QUALITY,
                "DECISION",
                Severity.WATCH,
                Severity.WARNING,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 11, 2),
                0.75,
                true
        );

        AlertTemplateComposer.ComposedMessage en = composer.composeAlert(payload, anandVihar, Language.en);
        AlertTemplateComposer.ComposedMessage hi = composer.composeAlert(payload, anandVihar, Language.hi);

        assertThat(en.title()).contains("⚠️ KESTREL AIR QUALITY WARNING: Anand Vihar, Delhi");
        assertThat(en.body()).contains("Very Poor category");
        assertThat(en.actionableAdvice()).contains("Avoid outdoor exercise, keep windows closed, and wear an N95 mask if outdoors");
        assertThat(en.provenanceNote()).contains("Guidance based on numerical model forecasts. Please verify locally before taking emergency action. Not an official government decree.");

        assertThat(hi.title()).contains("⚠️ केस्ट्रल वायु गुणवत्ता चेतावनी: Anand Vihar, Delhi");
        assertThat(hi.actionableAdvice()).contains("वायु गुणवत्ता बहुत खराब है। बाहर व्यायाम करने से बचें, खिड़कियां बंद रखें और बाहर जाते समय N95 मास्क पहनें");
    }

    @Test
    @DisplayName("PT-FANOUT-MH-01: Compose Air Quality SEVERE emergency alert in English and Hindi")
    void testComposeAirQualitySevereAlert() {
        LocationReplica anandVihar = new LocationReplica(
                "delhi-anand-vihar",
                "Anand Vihar, Delhi",
                "Delhi",
                28.6469,
                77.3160,
                null,
                0, 0, 0,
                Language.hi,
                true
        );

        AlertEventPayload payload = new AlertEventPayload(
                "alert-aq-002",
                "delhi-anand-vihar",
                Hazard.AIR_QUALITY,
                "DECISION",
                Severity.WARNING,
                Severity.SEVERE,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 11, 2),
                0.95,
                true
        );

        AlertTemplateComposer.ComposedMessage en = composer.composeAlert(payload, anandVihar, Language.en);
        AlertTemplateComposer.ComposedMessage hi = composer.composeAlert(payload, anandVihar, Language.hi);

        assertThat(en.title()).contains("🚨 KESTREL SEVERE AIR QUALITY EMERGENCY: Anand Vihar, Delhi");
        assertThat(en.actionableAdvice()).contains("Stay indoors as much as possible and seek immediate medical help for breathing difficulties");

        assertThat(hi.title()).contains("🚨 केस्ट्रल गंभीर वायु गुणवत्ता आपातकाल: Anand Vihar, Delhi");
        assertThat(hi.actionableAdvice()).contains("वायु गुणवत्ता गंभीर स्तर पर है। जितना संभव हो घर के अंदर रहें और सांस लेने में कठिनाई होने पर तुरंत चिकित्सा सहायता लें");
    }

    @Test
    @DisplayName("PT-FANOUT-MH-01 & PT-FANOUT-MH-02: Compose multi-hazard WhatsApp text and URL")
    void testWhatsAppTextMultiHazard() {
        LocationReplica delhiLocation = new LocationReplica(
                "delhi-central",
                "Delhi Central",
                "Delhi",
                28.6139,
                77.2090,
                null,
                0, 0, 0,
                Language.hi,
                true
        );

        // Heat WhatsApp text
        String heatText = composer.composeWhatsAppText(Hazard.HEAT, delhiLocation, Severity.WARNING, Provenance.MODEL_ONLY, LocalDate.of(2026, 11, 2), "Schools advised morning hours.");
        assertThat(heatText).contains("⚠️ KESTREL HEAT WAVE ALERT / लू चेतावनी");
        assertThat(heatText).contains("Location: Delhi Central");
        assertThat(heatText).contains("Avoid going out between 12 noon and 3 PM; drink water frequently");
        assertThat(heatText).contains("Note: Schools advised morning hours.");
        assertThat(heatText).contains("Guidance based on numerical model forecasts");

        String heatUrl = composer.generateWhatsAppShareUrl(Hazard.HEAT, delhiLocation, Severity.WARNING, Provenance.MODEL_ONLY, LocalDate.of(2026, 11, 2), null);
        assertThat(heatUrl).startsWith("https://wa.me/?text=");
        assertThat(heatUrl).contains("KESTREL+HEAT+WAVE+ALERT");

        // Air Quality WhatsApp text
        String aqText = composer.composeWhatsAppText(Hazard.AIR_QUALITY, delhiLocation, Severity.SEVERE, Provenance.MODEL_ONLY, LocalDate.of(2026, 11, 2), null);
        assertThat(aqText).contains("🚨 KESTREL AIR QUALITY ALERT / वायु गुणवत्ता चेतावनी");
        assertThat(aqText).contains("Stay indoors as much as possible and seek immediate medical help for breathing difficulties");
        assertThat(aqText).contains("Guidance based on numerical model forecasts");

        // Air Quality Warning WhatsApp text (N95 mask advice)
        String aqWarningText = composer.composeWhatsAppText(Hazard.AIR_QUALITY, delhiLocation, Severity.WARNING, Provenance.MODEL_ONLY, LocalDate.of(2026, 11, 2), null);
        assertThat(aqWarningText).contains("⚠️ KESTREL AIR QUALITY ALERT / वायु गुणवत्ता चेतावनी");
        assertThat(aqWarningText).contains("wear an N95 mask");

        String aqUrl = composer.generateWhatsAppShareUrl(Hazard.AIR_QUALITY, delhiLocation, Severity.SEVERE, Provenance.MODEL_ONLY, LocalDate.of(2026, 11, 2), null);
        assertThat(aqUrl).startsWith("https://wa.me/?text=");
        assertThat(aqUrl).contains("KESTREL+AIR+QUALITY+ALERT");
    }
}

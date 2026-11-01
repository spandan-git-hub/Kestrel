package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.AqSignalPayload;
import com.kestrel.common.events.AqSignalPayload.CurrentPollutants;
import com.kestrel.common.events.AqSignalPayload.ForecastDayPeak;
import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AqRule - Air Quality Evaluation Tests")
class AqRuleTest {

    private AqRule aqRule;
    private Instant now;

    @BeforeEach
    void setUp() {
        aqRule = new AqRule();
        now = Instant.parse("2026-11-01T04:00:00Z");
    }

    @Test
    @DisplayName("Calm air quality returns Severity.NONE")
    void calmAirQualityReturnsNone() {
        // Good category (PM2.5 = 20, PM10 = 35, NO2 = 25, SO2 = 15, O3 = 30, CO = 0.5)
        CurrentPollutants current = new CurrentPollutants(20.0, 35.0, 25.0, 15.0, 30.0, 0.5, 24);
        ForecastDayPeak peak1 = new ForecastDayPeak(LocalDate.of(2026, 11, 2), 22.0, 40.0, 25.0, 15.0, 30.0, 0.5);
        ForecastDayPeak peak2 = new ForecastDayPeak(LocalDate.of(2026, 11, 3), 25.0, 45.0, 25.0, 15.0, 30.0, 0.5);

        AqSignalPayload payload = new AqSignalPayload(
                "delhi-anand-vihar", "run-1", now, now, current, List.of(peak1, peak2)
        );

        AqEvaluation eval = aqRule.evaluate(payload);
        assertEquals(Severity.NONE, eval.candidateSeverity());
        assertTrue(eval.currentAqi() <= 100);
        assertNull(eval.firstExceedanceDate());
    }

    @Test
    @DisplayName("Current Poor air quality returns Severity.WATCH")
    void currentPoorAirQualityReturnsWatch() {
        // Poor category: PM2.5 = 100 (sub-index in 201-300 range)
        CurrentPollutants current = new CurrentPollutants(100.0, 150.0, 50.0, 30.0, 40.0, 1.0, 24);

        AqSignalPayload payload = new AqSignalPayload(
                "delhi-anand-vihar", "run-2", now, now, current, Collections.emptyList()
        );

        AqEvaluation eval = aqRule.evaluate(payload);
        assertEquals(Severity.WATCH, eval.candidateSeverity());
        assertTrue(eval.currentAqi() >= 201 && eval.currentAqi() <= 300);
        assertNotNull(eval.firstExceedanceDate());
    }

    @Test
    @DisplayName("Current Very Poor air quality returns Severity.WARNING")
    void currentVeryPoorAirQualityReturnsWarning() {
        // Very Poor category: PM2.5 = 150 (sub-index 323)
        CurrentPollutants current = new CurrentPollutants(150.0, 200.0, 60.0, 40.0, 40.0, 1.2, 24);

        AqSignalPayload payload = new AqSignalPayload(
                "delhi-anand-vihar", "run-3", now, now, current, Collections.emptyList()
        );

        AqEvaluation eval = aqRule.evaluate(payload);
        assertEquals(Severity.WARNING, eval.candidateSeverity());
        assertEquals(323, eval.currentAqi());
        assertNotNull(eval.firstExceedanceDate());
    }

    @Test
    @DisplayName("Current Severe air quality returns Severity.SEVERE")
    void currentSevereAirQualityReturnsSevere() {
        // Severe category: PM2.5 = 300 (sub-index in 401-500 range)
        CurrentPollutants current = new CurrentPollutants(300.0, 450.0, 150.0, 80.0, 60.0, 2.5, 24);

        AqSignalPayload payload = new AqSignalPayload(
                "delhi-anand-vihar", "run-4", now, now, current, Collections.emptyList()
        );

        AqEvaluation eval = aqRule.evaluate(payload);
        assertEquals(Severity.SEVERE, eval.candidateSeverity());
        assertTrue(eval.currentAqi() >= 401);
        assertNotNull(eval.firstExceedanceDate());
    }

    @Test
    @DisplayName("Calm current but forecast peak reaches Very Poor returns Severity.WARNING and firstExceedanceDate")
    void forecastPeakExceedanceReturnsWarningWithDate() {
        CurrentPollutants current = new CurrentPollutants(25.0, 45.0, 30.0, 20.0, 35.0, 0.6, 24); // Good
        LocalDate exceedanceDate = LocalDate.of(2026, 11, 3);
        ForecastDayPeak peak1 = new ForecastDayPeak(LocalDate.of(2026, 11, 2), 35.0, 60.0, 30.0, 20.0, 35.0, 0.6); // Satisfactory
        ForecastDayPeak peak2 = new ForecastDayPeak(exceedanceDate, 150.0, 220.0, 50.0, 30.0, 40.0, 1.0); // Very Poor (323)

        AqSignalPayload payload = new AqSignalPayload(
                "delhi-anand-vihar", "run-5", now, now, current, List.of(peak1, peak2)
        );

        AqEvaluation eval = aqRule.evaluate(payload);
        assertEquals(Severity.WARNING, eval.candidateSeverity());
        assertEquals(exceedanceDate, eval.firstExceedanceDate());
        assertEquals(323, eval.peakAqi());
    }

    @Test
    @DisplayName("Null payload returns Severity.NONE safely")
    void nullPayloadReturnsNone() {
        AqEvaluation eval = aqRule.evaluate(null);
        assertEquals(Severity.NONE, eval.candidateSeverity());
        assertEquals(0, eval.peakAqi());
        assertNull(eval.firstExceedanceDate());
    }
}


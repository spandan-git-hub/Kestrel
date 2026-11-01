package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.AqSignalPayload;
import com.kestrel.common.events.Pollutant;
import com.kestrel.common.events.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Pure domain AqRule evaluating official Central Pollution Control Board (CPCB) standards:
 *
 * 1. Evaluates current 24h/8h trailing rolling average concentrations.
 * 2. Evaluates projected 3-day peak forecast rolling concentrations.
 * 3. Enforces CPCB severity mapping:
 *    - Poor (201-300)      => WATCH
 *    - Very Poor (301-400) => WARNING
 *    - Severe (401-500)    => SEVERE
 *    - Good/Sat/Mod        => NONE
 */
public class AqRule {

    private static final Logger log = LoggerFactory.getLogger(AqRule.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public AqEvaluation evaluate(AqSignalPayload payload) {
        if (payload == null) {
            return new AqEvaluation(Severity.NONE, null, 0, 0, null, Collections.emptyMap());
        }

        int currentAqi = 0;
        Severity currentSeverity = Severity.NONE;
        Pollutant currentDominant = null;
        Map<Pollutant, Integer> currentSubIndices = Collections.emptyMap();

        if (payload.current() != null) {
            Map<Pollutant, Double> currentMap = toPollutantMap(payload.current());
            try {
                AqiCalculator.AqiResult currentResult = AqiCalculator.calculateDetailedAqi(currentMap);
                currentAqi = currentResult.aqi();
                currentSeverity = AqiCalculator.toSeverity(currentResult.category());
                currentDominant = currentResult.dominantPollutant();
                currentSubIndices = currentResult.subIndices();
            } catch (InsufficientDataException e) {
                log.debug("Current pollutants insufficient for CPCB AQI: {}", e.getMessage());
            }
        }

        int peakAqi = currentAqi;
        Severity highestSeverity = currentSeverity;
        Pollutant peakDominant = currentDominant;
        LocalDate firstExceedanceDate = null;

        LocalDate today = payload.dataAsOf() != null
                ? LocalDate.ofInstant(payload.dataAsOf(), IST)
                : LocalDate.now(IST);

        if (currentSeverity != Severity.NONE) {
            firstExceedanceDate = today;
        }

        if (payload.forecastPeaks() != null) {
            for (AqSignalPayload.ForecastDayPeak peak : payload.forecastPeaks()) {
                Map<Pollutant, Double> peakMap = toPollutantMap(peak);
                try {
                    AqiCalculator.AqiResult peakResult = AqiCalculator.calculateDetailedAqi(peakMap);
                    int dayAqi = peakResult.aqi();
                    Severity daySeverity = AqiCalculator.toSeverity(peakResult.category());

                    if (dayAqi > peakAqi) {
                        peakAqi = dayAqi;
                        peakDominant = peakResult.dominantPollutant();
                    }

                    if (daySeverity.ordinal() > highestSeverity.ordinal()) {
                        highestSeverity = daySeverity;
                    }

                    if (daySeverity != Severity.NONE && firstExceedanceDate == null) {
                        firstExceedanceDate = peak.date();
                    }
                } catch (InsufficientDataException e) {
                    log.debug("Forecast peak insufficient for CPCB AQI on {}: {}", peak.date(), e.getMessage());
                }
            }
        }

        return new AqEvaluation(
                highestSeverity,
                firstExceedanceDate,
                currentAqi,
                peakAqi,
                peakDominant,
                currentSubIndices
        );
    }

    private Map<Pollutant, Double> toPollutantMap(AqSignalPayload.CurrentPollutants c) {
        Map<Pollutant, Double> map = new EnumMap<>(Pollutant.class);
        map.put(Pollutant.PM25, c.pm25_24h());
        map.put(Pollutant.PM10, c.pm10_24h());
        map.put(Pollutant.NO2, c.no2_24h());
        map.put(Pollutant.SO2, c.so2_24h());
        map.put(Pollutant.O3, c.o3_8h());
        map.put(Pollutant.CO, c.co_8h_mgm3());
        return map;
    }

    private Map<Pollutant, Double> toPollutantMap(AqSignalPayload.ForecastDayPeak p) {
        Map<Pollutant, Double> map = new EnumMap<>(Pollutant.class);
        map.put(Pollutant.PM25, p.pm25_24h());
        map.put(Pollutant.PM10, p.pm10_24h());
        map.put(Pollutant.NO2, p.no2_24h());
        map.put(Pollutant.SO2, p.so2_24h());
        map.put(Pollutant.O3, p.o3_8h());
        map.put(Pollutant.CO, p.co_8h_mgm3());
        return map;
    }
}


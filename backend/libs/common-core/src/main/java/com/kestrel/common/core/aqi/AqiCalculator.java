package com.kestrel.common.core.aqi;

import com.kestrel.common.events.AqiCategory;
import com.kestrel.common.events.Pollutant;
import com.kestrel.common.events.Severity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Pure domain calculator implementing the official Central Pollution Control Board (CPCB)
 * National Air Quality Index (AQI) linear interpolation methodology:
 *
 *   I = ((I_hi - I_lo) / (B_hi - B_lo)) * (C - B_lo) + I_lo
 *
 * Mandatory CPCB Criteria:
 * 1. Requires at least 3 pollutants monitored.
 * 2. At least one of PM2.5 or PM10 MUST be present.
 * 3. Overall AQI = max(sub-index_p) across valid pollutants.
 *
 * Acceptance verification:
 * - PM2.5 = 45 µg/m³ => AQI 75 (Satisfactory)
 * - PM2.5 = 150 µg/m³ => AQI 323 (Very Poor)
 */
public class AqiCalculator {

    public record Breakpoint(double bLo, double bHi, int iLo, int iHi) {}

    public record AqiResult(
            int aqi,
            AqiCategory category,
            Pollutant dominantPollutant,
            Map<Pollutant, Integer> subIndices
    ) {}

    private static final Map<Pollutant, List<Breakpoint>> BREAKPOINTS = new EnumMap<>(Pollutant.class);

    static {
        // PM2.5 (24h, µg/m³)
        BREAKPOINTS.put(Pollutant.PM25, List.of(
                new Breakpoint(0.0, 30.0, 0, 50),
                new Breakpoint(31.0, 60.0, 51, 100),
                new Breakpoint(61.0, 90.0, 101, 200),
                new Breakpoint(91.0, 120.0, 201, 300),
                new Breakpoint(121.0, 250.0, 301, 400),
                new Breakpoint(251.0, 400.0, 401, 500)
        ));

        // PM10 (24h, µg/m³)
        BREAKPOINTS.put(Pollutant.PM10, List.of(
                new Breakpoint(0.0, 50.0, 0, 50),
                new Breakpoint(51.0, 100.0, 51, 100),
                new Breakpoint(101.0, 250.0, 101, 200),
                new Breakpoint(251.0, 350.0, 201, 300),
                new Breakpoint(351.0, 430.0, 301, 400),
                new Breakpoint(431.0, 550.0, 401, 500)
        ));

        // NO2 (24h, µg/m³)
        BREAKPOINTS.put(Pollutant.NO2, List.of(
                new Breakpoint(0.0, 40.0, 0, 50),
                new Breakpoint(41.0, 80.0, 51, 100),
                new Breakpoint(81.0, 180.0, 101, 200),
                new Breakpoint(181.0, 280.0, 201, 300),
                new Breakpoint(281.0, 400.0, 301, 400),
                new Breakpoint(401.0, 550.0, 401, 500)
        ));

        // SO2 (24h, µg/m³)
        BREAKPOINTS.put(Pollutant.SO2, List.of(
                new Breakpoint(0.0, 40.0, 0, 50),
                new Breakpoint(41.0, 80.0, 51, 100),
                new Breakpoint(81.0, 380.0, 101, 200),
                new Breakpoint(381.0, 800.0, 201, 300),
                new Breakpoint(801.0, 1600.0, 301, 400),
                new Breakpoint(1601.0, 2000.0, 401, 500)
        ));

        // O3 (8h, µg/m³)
        BREAKPOINTS.put(Pollutant.O3, List.of(
                new Breakpoint(0.0, 50.0, 0, 50),
                new Breakpoint(51.0, 100.0, 51, 100),
                new Breakpoint(101.0, 168.0, 101, 200),
                new Breakpoint(169.0, 208.0, 201, 300),
                new Breakpoint(209.0, 748.0, 301, 400),
                new Breakpoint(749.0, 1000.0, 401, 500)
        ));

        // CO (8h, mg/m³)
        BREAKPOINTS.put(Pollutant.CO, List.of(
                new Breakpoint(0.0, 1.0, 0, 50),
                new Breakpoint(1.1, 2.0, 51, 100),
                new Breakpoint(2.1, 10.0, 101, 200),
                new Breakpoint(10.1, 17.0, 201, 300),
                new Breakpoint(17.1, 34.0, 301, 400),
                new Breakpoint(34.1, 50.0, 401, 500)
        ));
    }

    /**
     * Calculates the CPCB linear interpolated sub-index for a single criteria pollutant.
     */
    public static int calculateSubIndex(Pollutant pollutant, double concentration) {
        if (pollutant == null) {
            throw new IllegalArgumentException("Pollutant cannot be null");
        }
        double c = Math.max(0.0, concentration);
        List<Breakpoint> list = BREAKPOINTS.get(pollutant);
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("No breakpoints configured for pollutant " + pollutant);
        }

        for (int i = 0; i < list.size(); i++) {
            Breakpoint bp = list.get(i);
            boolean isLast = (i == list.size() - 1);
            if (c <= bp.bHi() || isLast) {
                double clamped = Math.max(bp.bLo(), Math.min(bp.bHi(), c));
                double sub = ((double) (bp.iHi() - bp.iLo()) / (bp.bHi() - bp.bLo())) * (clamped - bp.bLo()) + bp.iLo();
                return Math.min(500, (int) Math.round(sub));
            }
        }
        return 500;
    }

    /**
     * Calculates the overall CPCB AQI value.
     * Enforces mandatory CPCB criteria: >= 3 pollutants with PM2.5 or PM10.
     * Throws InsufficientDataException if criteria not met.
     */
    public static int calculateAqi(Map<Pollutant, Double> concentrations) {
        return calculateDetailedAqi(concentrations).aqi();
    }

    /**
     * Calculates overall AQI and sub-indices with full metadata.
     */
    public static AqiResult calculateDetailedAqi(Map<Pollutant, Double> concentrations) {
        if (concentrations == null) {
            throw new InsufficientDataException("Pollutant concentrations map cannot be null");
        }

        Map<Pollutant, Integer> subIndices = new EnumMap<>(Pollutant.class);
        for (Map.Entry<Pollutant, Double> entry : concentrations.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isNaN()) {
                subIndices.put(entry.getKey(), calculateSubIndex(entry.getKey(), entry.getValue()));
            }
        }

        // CPCB Rule 1: At least 3 pollutants required
        if (subIndices.size() < 3) {
            throw new InsufficientDataException(
                    "CPCB AQI requires at least 3 criteria pollutants (provided: " + subIndices.size() + ")"
            );
        }

        // CPCB Rule 2: At least one of PM2.5 or PM10 must be present
        if (!subIndices.containsKey(Pollutant.PM25) && !subIndices.containsKey(Pollutant.PM10)) {
            throw new InsufficientDataException(
                    "CPCB AQI requires at least one particulate matter indicator: PM2.5 or PM10"
            );
        }

        int maxAqi = 0;
        Pollutant dominant = null;

        for (Map.Entry<Pollutant, Integer> entry : subIndices.entrySet()) {
            if (entry.getValue() > maxAqi) {
                maxAqi = entry.getValue();
                dominant = entry.getKey();
            }
        }

        AqiCategory category = AqiCategory.fromAqi(maxAqi);
        return new AqiResult(maxAqi, category, dominant, Collections.unmodifiableMap(subIndices));
    }

    public static Severity toSeverity(AqiCategory category) {
        return category != null ? category.severity() : Severity.NONE;
    }

    public static Severity toSeverity(int aqi) {
        return toSeverity(AqiCategory.fromAqi(aqi));
    }
}


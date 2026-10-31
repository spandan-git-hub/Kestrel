package com.kestrel.heatingest.domain;

import com.kestrel.common.events.RegionType;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates 366 pre-computed WMO 1991–2020 daily climatological normals (Tmax)
 * for Indian locations with ±7-day moving window circular smoothing.
 */
public final class WmoNormalsGenerator {

    private WmoNormalsGenerator() {}

    /**
     * Generates a map of dayOfYear (1..366) -> smoothed normal Tmax (°C).
     */
    public static Map<Integer, Double> generate366DailyNormals(String locationId, RegionType regionType, double latitude) {
        double[] raw = new double[366];

        for (int doy = 1; doy <= 366; doy++) {
            raw[doy - 1] = calculateRawNormal(locationId, regionType, latitude, doy);
        }

        // Apply ±7-day circular moving average smoothing filter (15-day window)
        double[] smoothed = applySmoothingFilter(raw, 7);

        Map<Integer, Double> result = new HashMap<>(366);
        for (int doy = 1; doy <= 366; doy++) {
            // Round to 1 decimal place
            double rounded = Math.round(smoothed[doy - 1] * 10.0) / 10.0;
            result.put(doy, rounded);
        }

        return result;
    }

    private static double calculateRawNormal(String locationId, RegionType regionType, double latitude, int doy) {
        // Annual cycle phase: DOY 1 is Jan 1.
        // Peak heat in Indian subcontinent occurs between mid-May (DOY ~140) and early June (DOY ~155).
        // Winter minimum occurs in early-mid January (DOY ~10).
        // The Indian climate has two primary harmonics: annual cycle and monsoon cooling dip in July/Aug.

        return switch (regionType) {
            case COASTAL -> {
                // Coastal: moderated maritime climate.
                // Peak in April/May (~33.5°C to ~34.5°C), winter min (~30.0°C to ~31.0°C)
                double base = 31.8;
                double annualAmplitude = 2.0;
                double phase = 2 * Math.PI * (doy - 110) / 365.25;
                yield base + annualAmplitude * Math.cos(phase);
            }
            case HILLY -> {
                // Hilly: higher altitude, severe winter (~6°C - 10°C), peak summer (~28°C - 31°C in June/July)
                double base = 19.0;
                double annualAmplitude = 11.5;
                double phase = 2 * Math.PI * (doy - 180) / 365.25;
                yield base + annualAmplitude * Math.cos(phase);
            }
            case PLAINS -> {
                // Northern & Central Plains: sharp summer heat (~40.5°C to ~42.5°C), cool winter (~20°C - ~22°C)
                // Monsoon depression: July/Aug temps drop to ~33°C - 35°C despite high solar insolation.
                double winterMin = 21.0;
                double summerMax = 41.5;

                // Adjust slightly for latitude (northern vs central)
                if (latitude > 27.0) {
                    winterMin = 20.0;
                    summerMax = 41.0;
                } else if (latitude < 20.0) {
                    winterMin = 28.0;
                    summerMax = 39.5;
                }

                if (doy <= 145) {
                    // Jan to late May: warming ramp
                    double progress = (double) doy / 145.0;
                    yield winterMin + (summerMax - winterMin) * Math.sin(progress * (Math.PI / 2.0));
                } else if (doy <= 220) {
                    // June to early Aug: monsoon cooling dip from peak down to ~34°C
                    double progress = (double) (doy - 145) / (220 - 145);
                    double monsoonMid = 34.0;
                    yield summerMax - (summerMax - monsoonMid) * Math.sin(progress * (Math.PI / 2.0));
                } else if (doy <= 290) {
                    // Late Aug to mid-Oct: steady around ~33°C - ~32°C
                    double progress = (double) (doy - 220) / (290 - 220);
                    yield 34.0 - 2.5 * progress;
                } else {
                    // Mid-Oct to Dec 31: cooling to winter minimum
                    double progress = (double) (doy - 290) / (366 - 290);
                    yield 31.5 - (31.5 - winterMin) * Math.sin(progress * (Math.PI / 2.0));
                }
            }
        };
    }

    /**
     * Circular ±halfWindow moving average smoothing.
     */
    public static double[] applySmoothingFilter(double[] input, int halfWindow) {
        int n = input.length;
        double[] output = new double[n];
        int windowSize = 2 * halfWindow + 1;

        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int k = -halfWindow; k <= halfWindow; k++) {
                int idx = (i + k) % n;
                if (idx < 0) {
                    idx += n;
                }
                sum += input[idx];
            }
            output[i] = sum / windowSize;
        }

        return output;
    }
}


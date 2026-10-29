package com.kestrel.floodingest.domain;

import java.util.List;
import java.util.Objects;

/**
 * Gumbel (Extreme Value Type I) distribution calculation for flood return periods (T2, T5, T20).
 * Standard hydrological method used by GloFAS and river basin authorities.
 */
public class GumbelDistribution {

    private static final double EULER_MASCHERONI = 0.5772156649015329;
    private static final double SQRT_6_OVER_PI = Math.sqrt(6.0) / Math.PI;

    private final double locationMu;
    private final double scaleBeta;
    private final double mean;
    private final double stdDev;
    private final int sampleCount;

    public GumbelDistribution(double locationMu, double scaleBeta) {
        this(locationMu, scaleBeta, 0.0, 0.0, 0);
    }

    public GumbelDistribution(double locationMu, double scaleBeta, double mean, double stdDev, int sampleCount) {
        if (scaleBeta <= 0) {
            throw new IllegalArgumentException("Scale parameter beta must be positive: " + scaleBeta);
        }
        this.locationMu = locationMu;
        this.scaleBeta = scaleBeta;
        this.mean = mean;
        this.stdDev = stdDev;
        this.sampleCount = sampleCount;
    }

    public static GumbelDistribution fit(List<Double> annualMaxDischarges) {
        Objects.requireNonNull(annualMaxDischarges, "annualMaxDischarges cannot be null");
        if (annualMaxDischarges.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 observations to fit Gumbel distribution");
        }

        double n = annualMaxDischarges.size();
        double mean = annualMaxDischarges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double variance = annualMaxDischarges.stream()
                .mapToDouble(x -> Math.pow(x - mean, 2))
                .sum() / (n - 1);
        double stdDev = Math.sqrt(variance);

        double beta = stdDev * SQRT_6_OVER_PI;
        double mu = mean - (EULER_MASCHERONI * beta);

        return new GumbelDistribution(mu, beta, mean, stdDev, annualMaxDischarges.size());
    }

    public double dischargeForReturnPeriod(double returnPeriodYears) {
        if (returnPeriodYears <= 1.0) {
            throw new IllegalArgumentException("Return period must be > 1.0 years");
        }
        double pNonExceedance = 1.0 - (1.0 / returnPeriodYears);
        double y = -Math.log(-Math.log(pNonExceedance));
        double discharge = locationMu + (scaleBeta * y);
        return Math.round(discharge * 100.0) / 100.0;
    }

    public ReturnPeriodThresholds calculateThresholds() {
        double t2 = dischargeForReturnPeriod(2.0);
        double t5 = dischargeForReturnPeriod(5.0);
        double t20 = dischargeForReturnPeriod(20.0);
        return new ReturnPeriodThresholds(t2, t5, t20);
    }

    public double getLocationMu() {
        return locationMu;
    }

    public double getScaleBeta() {
        return scaleBeta;
    }

    public double getMean() {
        return mean;
    }

    public double getStdDev() {
        return stdDev;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public GumbelFitSummary getFitSummary() {
        return new GumbelFitSummary(sampleCount, mean, stdDev, locationMu, scaleBeta, calculateThresholds());
    }

    public record ReturnPeriodThresholds(double t2, double t5, double t20) {}

    public record GumbelFitSummary(
            int sampleCount,
            double mean,
            double stdDev,
            double locationMu,
            double scaleBeta,
            ReturnPeriodThresholds thresholds
    ) {}
}

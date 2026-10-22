package com.kestrel.floodingest.domain;

import com.kestrel.floodingest.domain.GumbelDistribution.ReturnPeriodThresholds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GumbelDistributionTest {

    @Test
    @DisplayName("Calculates monotonic return period discharges: T20 > T5 > T2")
    void shouldCalculateMonotonicThresholds() {
        GumbelDistribution dist = new GumbelDistribution(50.0, 15.0);
        ReturnPeriodThresholds thresholds = dist.calculateThresholds();

        assertThat(thresholds.t2()).isGreaterThan(0.0);
        assertThat(thresholds.t5()).isGreaterThan(thresholds.t2());
        assertThat(thresholds.t20()).isGreaterThan(thresholds.t5());
    }

    @Test
    @DisplayName("Fits Gumbel distribution from annual peak discharges")
    void shouldFitFromAnnualPeaks() {
        List<Double> annualPeaks = List.of(
                35.0, 42.0, 58.0, 48.0, 65.0, 39.0, 72.0, 51.0, 44.0, 61.0
        );

        GumbelDistribution fitted = GumbelDistribution.fit(annualPeaks);
        assertThat(fitted.getLocationMu()).isGreaterThan(0.0);
        assertThat(fitted.getScaleBeta()).isGreaterThan(0.0);

        ReturnPeriodThresholds thresholds = fitted.calculateThresholds();
        assertThat(thresholds.t2()).isGreaterThan(40.0);
        assertThat(thresholds.t20()).isGreaterThan(thresholds.t5());
    }

    @Test
    @DisplayName("Validates input constraints: beta > 0 and return period > 1")
    void shouldValidateInputs() {
        assertThatThrownBy(() -> new GumbelDistribution(50.0, -2.0))
                .isInstanceOf(IllegalArgumentException.class);

        GumbelDistribution dist = new GumbelDistribution(50.0, 10.0);
        assertThatThrownBy(() -> dist.dischargeForReturnPeriod(1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

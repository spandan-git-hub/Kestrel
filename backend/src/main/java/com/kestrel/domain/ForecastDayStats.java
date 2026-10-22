package com.kestrel.domain;

import java.time.LocalDate;

public record ForecastDayStats(
    LocalDate date,
    double qMin,
    double qP25,
    double qMedian,
    double qP75,
    double qMax,
    double share2yr,
    double share5yr,
    double share20yr
) {}

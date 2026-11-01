package com.kestrel.common.events;

/**
 * Official Central Pollution Control Board (CPCB) National Air Quality Index (AQI) categories.
 */
public enum AqiCategory {
    GOOD(0, 50, Severity.NONE),
    SATISFACTORY(51, 100, Severity.NONE),
    MODERATE(101, 200, Severity.NONE),
    POOR(201, 300, Severity.WATCH),
    VERY_POOR(301, 400, Severity.WARNING),
    SEVERE(401, 500, Severity.SEVERE);

    private final int minAqi;
    private final int maxAqi;
    private final Severity severity;

    AqiCategory(int minAqi, int maxAqi, Severity severity) {
        this.minAqi = minAqi;
        this.maxAqi = maxAqi;
        this.severity = severity;
    }

    public int minAqi() {
        return minAqi;
    }

    public int maxAqi() {
        return maxAqi;
    }

    public Severity severity() {
        return severity;
    }

    public static AqiCategory fromAqi(int aqi) {
        if (aqi <= 50) return GOOD;
        if (aqi <= 100) return SATISFACTORY;
        if (aqi <= 200) return MODERATE;
        if (aqi <= 300) return POOR;
        if (aqi <= 400) return VERY_POOR;
        return SEVERE;
    }
}


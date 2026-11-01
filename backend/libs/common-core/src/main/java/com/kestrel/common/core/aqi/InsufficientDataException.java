package com.kestrel.common.core.aqi;

/**
 * Thrown when air quality telemetry does not satisfy official CPCB minimum data criteria:
 * - Minimum of 3 criteria pollutants required.
 * - At least one of PM2.5 or PM10 must be present.
 */
public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String message) {
        super(message);
    }
}


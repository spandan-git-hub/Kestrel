package com.kestrel.domain;

public enum Provenance {
    MODEL_ONLY,           // Auto-computed from Open-Meteo ensemble forecast
    MODEL_PLUS_REPORTS,   // Corroborated by resident ground reports
    OFFICIAL_CONFIRMED    // Officially confirmed or manual coordinator broadcast
}

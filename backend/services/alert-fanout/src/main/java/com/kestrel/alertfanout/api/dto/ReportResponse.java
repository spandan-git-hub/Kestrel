package com.kestrel.alertfanout.api.dto;

import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.ReportCategory;

import java.time.Instant;

public record ReportResponse(
        Long reportId,
        String locationId,
        ReportCategory category,
        Provenance newProvenance,
        String message,
        Instant reportedAt
) {}

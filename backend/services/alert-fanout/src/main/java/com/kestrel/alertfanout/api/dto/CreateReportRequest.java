package com.kestrel.alertfanout.api.dto;

import com.kestrel.common.events.ReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReportRequest(
        @NotBlank(message = "locationId is required")
        String locationId,
        @NotNull(message = "category is required")
        ReportCategory category,
        String description,
        String reporterAlias
) {}

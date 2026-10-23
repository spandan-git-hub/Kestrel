package com.kestrel.alertfanout.api.dto;

import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BroadcastPreviewRequest(
        @NotBlank(message = "locationId is required")
        String locationId,
        Hazard hazard,
        @NotNull(message = "severity is required")
        Severity severity,
        String customNote
) {}

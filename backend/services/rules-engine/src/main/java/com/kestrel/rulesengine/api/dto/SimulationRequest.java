package com.kestrel.rulesengine.api.dto;

import com.kestrel.common.events.SimulationScenario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SimulationRequest(
        @NotNull(message = "scenario is required")
        SimulationScenario scenario,

        @NotBlank(message = "locationId is required")
        String locationId
) {}

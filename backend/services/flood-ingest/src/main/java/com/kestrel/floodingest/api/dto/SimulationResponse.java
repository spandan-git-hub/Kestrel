package com.kestrel.floodingest.api.dto;

import com.kestrel.common.events.SimulationScenario;

public record SimulationResponse(
    SimulationScenario scenario,
    String locationId,
    String message
) {}

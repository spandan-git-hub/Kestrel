package com.kestrel.common.events;

import java.time.Instant;

public record ProvenanceUpdatePayload(
        String locationId,
        Provenance provenance,
        int reportCount,
        Instant updatedAt
) {}

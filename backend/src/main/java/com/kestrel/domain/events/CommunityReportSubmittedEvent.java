package com.kestrel.domain.events;

import com.kestrel.domain.ReportCategory;
import java.time.Instant;

public record CommunityReportSubmittedEvent(
    Long reportId,
    String locationId,
    ReportCategory category,
    Instant timestamp
) {}

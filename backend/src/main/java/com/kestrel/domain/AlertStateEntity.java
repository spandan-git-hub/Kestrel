package com.kestrel.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alert_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStateEntity {

    @Id
    private String locationId;

    @Enumerated(EnumType.STRING)
    private Hazard hazard;

    @Enumerated(EnumType.STRING)
    private Severity currentSeverity;

    @Enumerated(EnumType.STRING)
    private Severity candidateSeverity;

    private Integer candidateRuns;
    private String activeAlertId;

    @Enumerated(EnumType.STRING)
    private Provenance provenance;

    private Instant lastSignalAt;
    private Instant lastTransitionAt;
    private LocalDate firstExceedanceDate;
    private Double peakShare;
    private Boolean isStale;
}

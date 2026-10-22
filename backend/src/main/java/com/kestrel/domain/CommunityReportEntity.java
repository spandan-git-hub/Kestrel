package com.kestrel.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "community_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String locationId;

    @Enumerated(EnumType.STRING)
    private ReportCategory category;

    private String description;
    private String reporterToken;
    private Instant createdAt;
}

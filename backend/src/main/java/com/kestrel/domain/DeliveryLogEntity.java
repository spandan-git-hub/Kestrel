package com.kestrel.domain;

import jakarta.persistence.Column;
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
@Table(name = "delivery_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String alertId;
    private String locationId;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    private String recipient;

    @Enumerated(EnumType.STRING)
    private Language language;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Instant sentAt;
}

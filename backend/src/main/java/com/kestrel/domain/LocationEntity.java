package com.kestrel.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationEntity {

    @Id
    private String id;

    private String name;
    private String state;
    private Double latitude;
    private Double longitude;
    private String riverName;
    private Double threshold2yr;
    private Double threshold5yr;
    private Double threshold20yr;

    @Enumerated(EnumType.STRING)
    private Language primaryLanguage;
}

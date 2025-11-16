package com.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para información básica de una estación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationInfo {

    private String stationId;
    private String stationName;
    private Double latitude;
    private Double longitude;
    private boolean active;
}
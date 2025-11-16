package com.weather.dto;

import com.weather.model.WeatherMeasurement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationDataResponse {

    private String stationId;
    private String stationName;
    private Double latitude;
    private Double longitude;
    private int totalMeasurements;
    private List<WeatherMeasurement> measurements;
}
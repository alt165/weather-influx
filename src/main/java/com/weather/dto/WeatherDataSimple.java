package com.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO con datos meteorológicos esenciales de una estación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDataSimple {

    // Información de la estación
    private String stationId;
    private String stationName;
    private Instant timestamp;

    // Temperatura
    private Double temp;              // Temperatura actual (°C)
    private Double tempAvg7Days;      // Promedio 7 días
    private Double windChill;         // Sensación térmica
    private Double windChillAvg7Days; // Promedio 7 días
    private Double dewPoint;          // Punto de rocío
    private Double dewPointAvg7Days;  // Promedio 7 días
    private Double wetBulb;           // Bulbo húmedo
    private Double wetBulbAvg7Days;   // Promedio 7 días

    // Humedad
    private Double hum;               // Humedad actual (%)
    private Double humAvg7Days;       // Promedio 7 días

    // Viento
    private Double windSpeedLast;     // Velocidad actual
    private Double windDirLast;       // Dirección actual (grados)

    // Lluvia
    private Double rainfallDayMm;     // Lluvia del día (mm)
    private Double rainfallMonthMm;   // Lluvia del mes (mm)
}
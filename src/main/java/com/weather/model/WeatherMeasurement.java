package com.weather.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Modelo que representa una medición meteorológica de una estación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherMeasurement {

    private String stationId;
    private String stationName;
    private Instant timestamp;

    // Temperatura
    private Double temp;                    // Temperatura exterior (°C o °F)
    private Double tempIn;                  // Temperatura interior
    private Double dewPoint;                // Punto de rocío
    private Double dewPointIn;              // Punto de rocío interior
    private Double heatIndex;               // Índice de calor
    private Double heatIndexIn;             // Índice de calor interior
    private Double windChill;               // Sensación térmica con viento
    private Double wetBulb;                 // Temperatura de bulbo húmedo
    private Double wetBulbIn;               // Temperatura de bulbo húmedo interior
    private Double thwIndex;                // Índice THW
    private Double thswIndex;               // Índice THSW

    // Humedad
    private Double hum;                     // Humedad exterior (%)
    private Double humIn;                   // Humedad interior (%)

    // Presión atmosférica
    private Double barAbsolute;             // Presión absoluta (hPa o inHg)
    private Double barSeaLevel;             // Presión al nivel del mar
    private Double barOffset;               // Offset de presión
    private String barTrend;                // Tendencia de presión

    // Viento
    private Double windSpeedLast;           // Última velocidad del viento
    private Double windSpeedAvgLast1Min;    // Velocidad promedio último minuto
    private Double windSpeedAvgLast2Min;    // Velocidad promedio últimos 2 minutos
    private Double windSpeedAvgLast10Min;   // Velocidad promedio últimos 10 minutos
    private Double windSpeedHiLast2Min;     // Ráfaga máxima últimos 2 minutos
    private Double windSpeedHiLast10Min;    // Ráfaga máxima últimos 10 minutos
    private Double windDirLast;             // Última dirección del viento (grados)
    private Double windDirScalarAvgLast1Min;  // Dirección promedio último minuto
    private Double windDirScalarAvgLast2Min;  // Dirección promedio últimos 2 minutos
    private Double windDirScalarAvgLast10Min; // Dirección promedio últimos 10 minutos
    private Double windDirAtHiSpeedLast2Min;  // Dirección en ráfaga máxima 2 min
    private Double windDirAtHiSpeedLast10Min; // Dirección en ráfaga máxima 10 min
    private Double windRunDay;              // Distancia recorrida por el viento en el día

    // Lluvia
    private Double rainfallDailyMm;         // Lluvia diaria (mm)
    private Double rainfallDailyIn;         // Lluvia diaria (pulgadas)
    private Double rainfallDayMm;           // Lluvia del día (mm)
    private Double rainfallMonthMm;         // Lluvia del mes (mm)
    private Double rainfallYearMm;          // Lluvia del año (mm)
    private Double rainfallLast15MinMm;     // Lluvia últimos 15 minutos (mm)
    private Double rainfallLast60MinMm;     // Lluvia última hora (mm)
    private Double rainfallLast24HrMm;      // Lluvia últimas 24 horas (mm)
    private Double rainRateLastMm;          // Tasa de lluvia actual (mm/hr)
    private Double rainRateHiMm;            // Tasa de lluvia máxima (mm/hr)
    private Double rainRateHiLast15MinMm;   // Tasa máxima últimos 15 min (mm/hr)

    // Radiación solar y UV
    private Double solarRad;                // Radiación solar (W/m²)
    private Double solarEnergyDay;          // Energía solar del día
    private Double uvIndex;                 // Índice UV
    private Double uvDoseDay;               // Dosis UV del día

    // Evapotranspiración
    private Double etDay;                   // ET del día
    private Double etMonth;                 // ET del mes
    private Double etYear;                  // ET del año

    // Ubicación
    private Double latitude;                // Latitud
    private Double longitude;               // Longitud
    private Double elevation;               // Elevación (metros)
}
package com.weather.service;

import com.weather.dto.StationDataResponse;
import com.weather.dto.StationInfo;
import com.weather.model.WeatherMeasurement;
import com.weather.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para la lógica de negocio de datos meteorológicos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherRepository weatherRepository;

    @Value("${weather.query.default-days:3}")
    private int defaultDays;

    /**
     * Obtiene todas las estaciones activas con su información básica
     */
    public List<StationInfo> getAllStations() {
        log.info("Fetching all active stations");

        List<String> stationIds = weatherRepository.getActiveStations();
        List<StationInfo> stations = new ArrayList<>();

        for (String stationId : stationIds) {
            Map<String, Object> basicInfo = weatherRepository.getStationBasicInfo(stationId);

            StationInfo info = StationInfo.builder()
                    .stationId(stationId)
                    .stationName((String) basicInfo.get("stationName"))
                    .latitude((Double) basicInfo.get("latitude"))
                    .longitude((Double) basicInfo.get("longitude"))
                    .active(true)
                    .build();

            stations.add(info);
        }

        return stations;
    }

    /**
     * Obtiene datos meteorológicos simplificados de una estación
     */
    public Map<String, Object> getStationWeatherDataSimple(String stationId) {
        log.info("Fetching simplified weather data for station {}", stationId);
        return weatherRepository.getStationWeatherData(stationId);
    }

    /**
     * Obtiene los datos de una estación específica
     */
    public StationDataResponse getStationData(String stationId, Integer days) {
        int queryDays = days != null ? days : defaultDays;
        log.info("Fetching data for station {} for the last {} days", stationId, queryDays);

        List<WeatherMeasurement> measurements = weatherRepository.getStationMeasurements(stationId, queryDays);

        if (measurements.isEmpty()) {
            log.warn("No measurements found for station {}", stationId);
            return StationDataResponse.builder()
                    .stationId(stationId)
                    .stationName(stationId)
                    .totalMeasurements(0)
                    .measurements(Collections.emptyList())
                    .build();
        }

        WeatherMeasurement latest = measurements.get(measurements.size() - 1);

        return StationDataResponse.builder()
                .stationId(stationId)
                .stationName(latest.getStationName())
                .latitude(latest.getLatitude())
                .longitude(latest.getLongitude())
                .totalMeasurements(measurements.size())
                .measurements(measurements)
                .build();
    }

    /**
     * Obtiene los datos de todas las estaciones
     */
    public Map<String, StationDataResponse> getAllStationsData(Integer days) {
        int queryDays = days != null ? days : defaultDays;
        log.info("Fetching data for all stations for the last {} days", queryDays);

        List<WeatherMeasurement> allMeasurements = weatherRepository.getAllStationsMeasurements(queryDays);

        Map<String, List<WeatherMeasurement>> measurementsByStation = allMeasurements.stream()
                .collect(Collectors.groupingBy(WeatherMeasurement::getStationId));

        Map<String, StationDataResponse> result = new HashMap<>();

        measurementsByStation.forEach((stationId, measurements) -> {
            if (!measurements.isEmpty()) {
                WeatherMeasurement latest = measurements.stream()
                        .max(Comparator.comparing(WeatherMeasurement::getTimestamp))
                        .orElse(measurements.get(0));

                result.put(stationId, StationDataResponse.builder()
                        .stationId(stationId)
                        .stationName(latest.getStationName())
                        .latitude(latest.getLatitude())
                        .longitude(latest.getLongitude())
                        .totalMeasurements(measurements.size())
                        .measurements(measurements)
                        .build());
            }
        });

        log.info("Retrieved data for {} stations", result.size());
        return result;
    }

    /**
     * Obtiene las últimas mediciones de todas las estaciones
     */
    public List<WeatherMeasurement> getLatestMeasurements() {
        log.info("Fetching latest measurements for all stations");
        return weatherRepository.getLatestMeasurements();
    }

    /**
     * Obtiene estadísticas resumidas de una estación
     */
    public Map<String, Object> getStationStatistics(String stationId, Integer days) {
        int queryDays = days != null ? days : defaultDays;
        log.info("Calculating statistics for station {} for the last {} days", stationId, queryDays);

        List<WeatherMeasurement> measurements = weatherRepository.getStationMeasurements(stationId, queryDays);

        if (measurements.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("stationId", stationId);
        stats.put("totalMeasurements", measurements.size());
        stats.put("period", queryDays + " days");

        // Calcular estadísticas para temperatura
        calculateFieldStats(measurements, "temp", stats,
                m -> m.getTemp() != null ? m.getTemp() : null);

        // Calcular estadísticas para humedad
        calculateFieldStats(measurements, "hum", stats,
                m -> m.getHum() != null ? m.getHum() : null);

        // Calcular estadísticas para velocidad del viento
        calculateFieldStats(measurements, "windSpeedLast", stats,
                m -> m.getWindSpeedLast() != null ? m.getWindSpeedLast() : null);

        // Calcular estadísticas para presión atmosférica
        calculateFieldStats(measurements, "barSeaLevel", stats,
                m -> m.getBarSeaLevel() != null ? m.getBarSeaLevel() : null);

        // Calcular total de lluvia
        double totalRainfall = measurements.stream()
                .filter(m -> m.getRainfallDayMm() != null)
                .mapToDouble(WeatherMeasurement::getRainfallDayMm)
                .max()
                .orElse(0.0);
        stats.put("totalRainfallDayMm", totalRainfall);

        return stats;
    }

    private void calculateFieldStats(List<WeatherMeasurement> measurements,
                                     String fieldName,
                                     Map<String, Object> stats,
                                     java.util.function.Function<WeatherMeasurement, Double> getter) {
        DoubleSummaryStatistics fieldStats = measurements.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        if (fieldStats.getCount() > 0) {
            Map<String, Double> fieldMap = new HashMap<>();
            fieldMap.put("min", fieldStats.getMin());
            fieldMap.put("max", fieldStats.getMax());
            fieldMap.put("avg", fieldStats.getAverage());
            stats.put(fieldName, fieldMap);
        }
    }
}
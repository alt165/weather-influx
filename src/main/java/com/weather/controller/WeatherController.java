package com.weather.controller;

import com.weather.dto.StationDataResponse;
import com.weather.dto.StationInfo;
import com.weather.model.WeatherMeasurement;
import com.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para endpoints de datos meteorológicos
 */
@Slf4j
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * GET /weather/stations
     * Obtiene la lista de todas las estaciones activas
     */
    @GetMapping("/stations")
    public ResponseEntity<List<StationInfo>> getAllStations() {
        log.info("GET /weather/stations - Fetching all stations");
        List<StationInfo> stations = weatherService.getAllStations();
        return ResponseEntity.ok(stations);
    }

    /**
     * GET /weather/stations/{stationId}/simple
     * Obtiene datos meteorológicos simplificados de una estación
     * Incluye: temp, wind chill, punto de rocío, bulbo húmedo, humedad (actuales y promedios 7 días)
     *         velocidad y dirección del viento, lluvia diaria y mensual
     *
     * @param stationId ID de la estación
     */
    @GetMapping("/stations/{stationId}/simple")
    public ResponseEntity<Map<String, Object>> getStationWeatherDataSimple(
            @PathVariable String stationId) {
        log.info("GET /weather/stations/{}/simple - Fetching simplified weather data", stationId);
        Map<String, Object> data = weatherService.getStationWeatherDataSimple(stationId);

        if (data.isEmpty()) {
            log.warn("No data found for station {}", stationId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(data);
    }

    /**
     * GET /weather/stations/{stationId}
     * Obtiene los datos de una estación específica
     *
     * @param stationId ID de la estación
     * @param days Número de días de datos a recuperar (opcional, default: 3)
     */
    @GetMapping("/stations/{stationId}")
    public ResponseEntity<StationDataResponse> getStationData(
            @PathVariable String stationId,
            @RequestParam(required = false) Integer days) {
        log.info("GET /weather/stations/{} - Fetching station data for {} days", stationId, days);
        StationDataResponse response = weatherService.getStationData(stationId, days);

        if (response.getTotalMeasurements() == 0) {
            log.warn("No data found for station {}", stationId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /weather/stations/data/all
     * Obtiene los datos de todas las estaciones
     *
     * @param days Número de días de datos a recuperar (opcional, default: 3)
     */
    @GetMapping("/stations/data/all")
    public ResponseEntity<Map<String, StationDataResponse>> getAllStationsData(
            @RequestParam(required = false) Integer days) {
        log.info("GET /weather/stations/data/all - Fetching data for all stations for {} days", days);
        Map<String, StationDataResponse> data = weatherService.getAllStationsData(days);
        return ResponseEntity.ok(data);
    }

    /**
     * GET /weather/latest
     * Obtiene las últimas mediciones de todas las estaciones
     */
    @GetMapping("/latest")
    public ResponseEntity<List<WeatherMeasurement>> getLatestMeasurements() {
        log.info("GET /weather/latest - Fetching latest measurements");
        List<WeatherMeasurement> measurements = weatherService.getLatestMeasurements();
        return ResponseEntity.ok(measurements);
    }

    /**
     * GET /weather/stations/{stationId}/statistics
     * Obtiene estadísticas resumidas de una estación
     *
     * @param stationId ID de la estación
     * @param days Número de días para calcular estadísticas (opcional, default: 3)
     */
    @GetMapping("/stations/{stationId}/statistics")
    public ResponseEntity<Map<String, Object>> getStationStatistics(
            @PathVariable String stationId,
            @RequestParam(required = false) Integer days) {
        log.info("GET /weather/stations/{}/statistics - Calculating statistics for {} days",
                stationId, days);
        Map<String, Object> stats = weatherService.getStationStatistics(stationId, days);

        if (stats.isEmpty()) {
            log.warn("No data found for station {}", stationId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /weather/health
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        log.debug("GET /weather/health - Health check");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "weather-backend"
        ));
    }
}
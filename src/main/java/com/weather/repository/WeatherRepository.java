package com.weather.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.weather.config.InfluxDBConfig;
import com.weather.model.WeatherMeasurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio para consultas a InfluxDB
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class WeatherRepository {

    private final InfluxDBClient influxDBClient;
    private final InfluxDBConfig influxDBConfig;

    /**
     * Obtiene las mediciones de una estación específica en los últimos N días
     */
    public List<WeatherMeasurement> getStationMeasurements(String stationId, int days) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: -%dd)
              |> filter(fn: (r) => r["station_id"] == "%s")
              |> filter(fn: (r) => r["_field"] != "elevation" and r["_field"] != "latitude" and r["_field"] != "longitude")
              |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
              |> sort(columns: ["_time"], desc: false)
            """, influxDBConfig.getBucket(), days, stationId);

        return executeQuery(flux);
    }

    /**
     * Obtiene las mediciones de todas las estaciones en los últimos N días
     */
    public List<WeatherMeasurement> getAllStationsMeasurements(int days) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: -%dd)
              |> filter(fn: (r) => r["_field"] != "elevation" and r["_field"] != "latitude" and r["_field"] != "longitude")
              |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
              |> sort(columns: ["_time"], desc: false)
            """, influxDBConfig.getBucket(), days);

        return executeQuery(flux);
    }

    /**
     * Obtiene la última medición de cada estación
     */
    public List<WeatherMeasurement> getLatestMeasurements() {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: -1h)
              |> filter(fn: (r) => r["_field"] != "elevation" and r["_field"] != "latitude" and r["_field"] != "longitude")
              |> last()
              |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
            """, influxDBConfig.getBucket());

        return executeQuery(flux);
    }

    /**
     * Obtiene la lista de estaciones activas
     */
    public List<String> getActiveStations() {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: -24h)
              |> filter(fn: (r) => exists r.station_id)
              |> keep(columns: ["station_id"])
              |> distinct(column: "station_id")
            """, influxDBConfig.getBucket());

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);
        List<String> stations = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String stationId = (String) record.getValueByKey("station_id");
                if (stationId != null && !stations.contains(stationId)) {
                    stations.add(stationId);
                }
            }
        }

        log.debug("Found {} active stations", stations.size());
        return stations;
    }

    /**
     * Obtiene información básica de una estación (nombre y coordenadas)
     */
    public Map<String, Object> getStationBasicInfo(String stationId) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: -24h)
              |> filter(fn: (r) => r["station_id"] == "%s")
              |> limit(n: 1)
            """, influxDBConfig.getBucket(), stationId);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);

        Map<String, Object> info = new HashMap<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                log.debug("Record values for station {}: {}", stationId, record.getValues());

                info.put("stationId", record.getValueByKey("station_id"));
                info.put("stationName", record.getValueByKey("station_name"));

                Object lat = record.getValueByKey("latitude");
                Object lon = record.getValueByKey("longitude");
                Object elev = record.getValueByKey("elevation");

                log.debug("Latitude raw: {} (type: {})", lat, lat != null ? lat.getClass().getName() : "null");
                log.debug("Longitude raw: {} (type: {})", lon, lon != null ? lon.getClass().getName() : "null");
                log.debug("Elevation raw: {} (type: {})", elev, elev != null ? elev.getClass().getName() : "null");

                if (lat instanceof Number) {
                    info.put("latitude", ((Number) lat).doubleValue());
                } else if (lat instanceof String) {
                    try {
                        info.put("latitude", Double.parseDouble((String) lat));
                    } catch (NumberFormatException e) {
                        log.warn("Cannot parse latitude: {}", lat);
                    }
                }

                if (lon instanceof Number) {
                    info.put("longitude", ((Number) lon).doubleValue());
                } else if (lon instanceof String) {
                    try {
                        info.put("longitude", Double.parseDouble((String) lon));
                    } catch (NumberFormatException e) {
                        log.warn("Cannot parse longitude: {}", lon);
                    }
                }

                if (elev instanceof Number) {
                    info.put("elevation", ((Number) elev).doubleValue());
                } else if (elev instanceof String) {
                    try {
                        info.put("elevation", Double.parseDouble((String) elev));
                    } catch (NumberFormatException e) {
                        log.warn("Cannot parse elevation: {}", elev);
                    }
                }

                log.debug("Final info map: {}", info);
                break; // Solo necesitamos un registro
            }
        }

        return info;
    }

    /**
     * Obtiene datos meteorológicos simplificados de una estación
     */
    public Map<String, Object> getStationWeatherData(String stationId) {
        // Datos actuales
        String fluxCurrent = String.format("""
            from(bucket: "%s")
              |> range(start: -1h)
              |> filter(fn: (r) => r["station_id"] == "%s")
              |> filter(fn: (r) => 
                  r["_field"] == "temp" or 
                  r["_field"] == "wind_chill" or 
                  r["_field"] == "dew_point" or 
                  r["_field"] == "wet_bulb" or 
                  r["_field"] == "hum" or 
                  r["_field"] == "wind_speed_last" or 
                  r["_field"] == "wind_dir_last" or 
                  r["_field"] == "rainfall_day_mm" or 
                  r["_field"] == "rainfall_month_mm"
              )
              |> last()
              |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
            """, influxDBConfig.getBucket(), stationId);

        // Promedios de 7 días
        String fluxAvg = String.format("""
            from(bucket: "%s")
              |> range(start: -7d)
              |> filter(fn: (r) => r["station_id"] == "%s")
              |> filter(fn: (r) => 
                  r["_field"] == "temp" or 
                  r["_field"] == "wind_chill" or 
                  r["_field"] == "dew_point" or 
                  r["_field"] == "wet_bulb" or 
                  r["_field"] == "hum"
              )
              |> mean()
              |> pivot(rowKey:["station_id"], columnKey: ["_field"], valueColumn: "_value")
            """, influxDBConfig.getBucket(), stationId);

        QueryApi queryApi = influxDBClient.getQueryApi();

        Map<String, Object> result = new HashMap<>();

        // Obtener datos actuales
        List<FluxTable> currentTables = queryApi.query(fluxCurrent);
        for (FluxTable table : currentTables) {
            for (FluxRecord record : table.getRecords()) {
                result.put("timestamp", record.getTime());
                result.put("stationId", record.getValueByKey("station_id"));
                result.put("stationName", record.getValueByKey("station_name"));
                result.put("temp", record.getValueByKey("temp"));
                result.put("windChill", record.getValueByKey("wind_chill"));
                result.put("dewPoint", record.getValueByKey("dew_point"));
                result.put("wetBulb", record.getValueByKey("wet_bulb"));
                result.put("hum", record.getValueByKey("hum"));
                result.put("windSpeedLast", record.getValueByKey("wind_speed_last"));
                result.put("windDirLast", record.getValueByKey("wind_dir_last"));
                result.put("rainfallDayMm", record.getValueByKey("rainfall_day_mm"));
                result.put("rainfallMonthMm", record.getValueByKey("rainfall_month_mm"));
            }
        }

        // Obtener promedios
        List<FluxTable> avgTables = queryApi.query(fluxAvg);
        for (FluxTable table : avgTables) {
            for (FluxRecord record : table.getRecords()) {
                result.put("tempAvg7Days", record.getValueByKey("temp"));
                result.put("windChillAvg7Days", record.getValueByKey("wind_chill"));
                result.put("dewPointAvg7Days", record.getValueByKey("dew_point"));
                result.put("wetBulbAvg7Days", record.getValueByKey("wet_bulb"));
                result.put("humAvg7Days", record.getValueByKey("hum"));
            }
        }

        return result;
    }

    /**
     * Ejecuta una consulta Flux y mapea los resultados a WeatherMeasurement
     */
    private List<WeatherMeasurement> executeQuery(String flux) {
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);
        Map<String, WeatherMeasurement.WeatherMeasurementBuilder> measurementMap = new HashMap<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Instant time = record.getTime();
                String stationId = (String) record.getValueByKey("station_id");

                if (time == null || stationId == null) {
                    continue;
                }

                String key = stationId + "_" + time.toString();
                WeatherMeasurement.WeatherMeasurementBuilder builder =
                        measurementMap.computeIfAbsent(key, k -> WeatherMeasurement.builder()
                                .timestamp(time)
                                .stationId(stationId));

                // Mapear campos de texto
                setStringField(builder, record, "station_name", (b, v) -> b.stationName(v));
                setStringField(builder, record, "bar_trend", (b, v) -> b.barTrend(v));

                // Temperatura
                setDoubleField(builder, record, "temp", (b, v) -> b.temp(v));
                setDoubleField(builder, record, "temp_in", (b, v) -> b.tempIn(v));
                setDoubleField(builder, record, "dew_point", (b, v) -> b.dewPoint(v));
                setDoubleField(builder, record, "dew_point_in", (b, v) -> b.dewPointIn(v));
                setDoubleField(builder, record, "heat_index", (b, v) -> b.heatIndex(v));
                setDoubleField(builder, record, "heat_index_in", (b, v) -> b.heatIndexIn(v));
                setDoubleField(builder, record, "wind_chill", (b, v) -> b.windChill(v));
                setDoubleField(builder, record, "wet_bulb", (b, v) -> b.wetBulb(v));
                setDoubleField(builder, record, "wet_bulb_in", (b, v) -> b.wetBulbIn(v));
                setDoubleField(builder, record, "thw_index", (b, v) -> b.thwIndex(v));
                setDoubleField(builder, record, "thsw_index", (b, v) -> b.thswIndex(v));

                // Humedad
                setDoubleField(builder, record, "hum", (b, v) -> b.hum(v));
                setDoubleField(builder, record, "hum_in", (b, v) -> b.humIn(v));

                // Presión
                setDoubleField(builder, record, "bar_absolute", (b, v) -> b.barAbsolute(v));
                setDoubleField(builder, record, "bar_sea_level", (b, v) -> b.barSeaLevel(v));
                setDoubleField(builder, record, "bar_offset", (b, v) -> b.barOffset(v));

                // Viento
                setDoubleField(builder, record, "wind_speed_last", (b, v) -> b.windSpeedLast(v));
                setDoubleField(builder, record, "wind_speed_avg_last_1_min", (b, v) -> b.windSpeedAvgLast1Min(v));
                setDoubleField(builder, record, "wind_speed_avg_last_2_min", (b, v) -> b.windSpeedAvgLast2Min(v));
                setDoubleField(builder, record, "wind_speed_avg_last_10_min", (b, v) -> b.windSpeedAvgLast10Min(v));
                setDoubleField(builder, record, "wind_speed_hi_last_2_min", (b, v) -> b.windSpeedHiLast2Min(v));
                setDoubleField(builder, record, "wind_speed_hi_last_10_min", (b, v) -> b.windSpeedHiLast10Min(v));
                setDoubleField(builder, record, "wind_dir_last", (b, v) -> b.windDirLast(v));
                setDoubleField(builder, record, "wind_dir_scalar_avg_last_1_min", (b, v) -> b.windDirScalarAvgLast1Min(v));
                setDoubleField(builder, record, "wind_dir_scalar_avg_last_2_min", (b, v) -> b.windDirScalarAvgLast2Min(v));
                setDoubleField(builder, record, "wind_dir_scalar_avg_last_10_min", (b, v) -> b.windDirScalarAvgLast10Min(v));
                setDoubleField(builder, record, "wind_dir_at_hi_speed_last_2_min", (b, v) -> b.windDirAtHiSpeedLast2Min(v));
                setDoubleField(builder, record, "wind_dir_at_hi_speed_last_10_min", (b, v) -> b.windDirAtHiSpeedLast10Min(v));
                setDoubleField(builder, record, "wind_run_day", (b, v) -> b.windRunDay(v));

                // Lluvia
                setDoubleField(builder, record, "rainfall_daily_mm", (b, v) -> b.rainfallDailyMm(v));
                setDoubleField(builder, record, "rainfall_daily_in", (b, v) -> b.rainfallDailyIn(v));
                setDoubleField(builder, record, "rainfall_day_mm", (b, v) -> b.rainfallDayMm(v));
                setDoubleField(builder, record, "rainfall_month_mm", (b, v) -> b.rainfallMonthMm(v));
                setDoubleField(builder, record, "rainfall_year_mm", (b, v) -> b.rainfallYearMm(v));
                setDoubleField(builder, record, "rainfall_last_15_min_mm", (b, v) -> b.rainfallLast15MinMm(v));
                setDoubleField(builder, record, "rainfall_last_60_min_mm", (b, v) -> b.rainfallLast60MinMm(v));
                setDoubleField(builder, record, "rainfall_last_24_hr_mm", (b, v) -> b.rainfallLast24HrMm(v));
                setDoubleField(builder, record, "rain_rate_last_mm", (b, v) -> b.rainRateLastMm(v));
                setDoubleField(builder, record, "rain_rate_hi_mm", (b, v) -> b.rainRateHiMm(v));
                setDoubleField(builder, record, "rain_rate_hi_last_15_min_mm", (b, v) -> b.rainRateHiLast15MinMm(v));

                // Radiación solar y UV
                setDoubleField(builder, record, "solar_rad", (b, v) -> b.solarRad(v));
                setDoubleField(builder, record, "solar_energy_day", (b, v) -> b.solarEnergyDay(v));
                setDoubleField(builder, record, "uv_index", (b, v) -> b.uvIndex(v));
                setDoubleField(builder, record, "uv_dose_day", (b, v) -> b.uvDoseDay(v));

                // Evapotranspiración
                setDoubleField(builder, record, "et_day", (b, v) -> b.etDay(v));
                setDoubleField(builder, record, "et_month", (b, v) -> b.etMonth(v));
                setDoubleField(builder, record, "et_year", (b, v) -> b.etYear(v));

                // Ubicación - estos campos son tags en InfluxDB, no fields
                // Se obtienen directamente del record como tags
                Object lat = record.getValueByKey("latitude");
                Object lon = record.getValueByKey("longitude");
                Object elev = record.getValueByKey("elevation");

                if (lat instanceof Number) {
                    builder.latitude(((Number) lat).doubleValue());
                }
                if (lon instanceof Number) {
                    builder.longitude(((Number) lon).doubleValue());
                }
                if (elev instanceof Number) {
                    builder.elevation(((Number) elev).doubleValue());
                }
            }
        }

        List<WeatherMeasurement> measurements = measurementMap.values().stream()
                .map(WeatherMeasurement.WeatherMeasurementBuilder::build)
                .toList();

        log.debug("Retrieved {} measurements from InfluxDB", measurements.size());
        return measurements;
    }

    /**
     * Mapea un campo String del registro al builder
     */
    private void setStringField(WeatherMeasurement.WeatherMeasurementBuilder builder,
                                FluxRecord record,
                                String fieldName,
                                StringFieldSetter setter) {
        Object value = record.getValueByKey(fieldName);
        if (value instanceof String) {
            setter.set(builder, (String) value);
        }
    }

    /**
     * Mapea un campo Double del registro al builder
     */
    private void setDoubleField(WeatherMeasurement.WeatherMeasurementBuilder builder,
                                FluxRecord record,
                                String fieldName,
                                DoubleFieldSetter setter) {
        Object value = record.getValueByKey(fieldName);
        if (value instanceof Number) {
            setter.set(builder, ((Number) value).doubleValue());
        }
    }

    /**
     * Obtiene el nombre del bucket
     */
    private String getBucket() {
        return influxDBConfig.getBucket();
    }

    /**
     * Interfaz funcional para setters de campos String
     */
    @FunctionalInterface
    private interface StringFieldSetter {
        void set(WeatherMeasurement.WeatherMeasurementBuilder builder, String value);
    }

    /**
     * Interfaz funcional para setters de campos Double
     */
    @FunctionalInterface
    private interface DoubleFieldSetter {
        void set(WeatherMeasurement.WeatherMeasurementBuilder builder, Double value);
    }
}
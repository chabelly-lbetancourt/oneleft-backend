package es.upm.miw.oneleft.plans.infrastructure.weather;

import es.upm.miw.oneleft.plans.domain.model.Forecast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Client of Open-Meteo (free, no key). Each forecast is cached in Redis for a while ({@link WeatherConfig}) by place
 * (about 1 km) and hour, so that a popular plan does not ask again on every visit. The circuit breaker stops calling
 * the service while it keeps failing; then, and on any error, there is no forecast ({@code null}, not cached).
 */
@Component
class OpenMeteoClient {

    static final String CIRCUIT = "weather";
    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final RestClient http;
    private final CircuitBreaker breaker;

    OpenMeteoClient(RestClient openMeteo, CircuitBreakerFactory<?, ?> breakers) {
        this.http = openMeteo;
        this.breaker = breakers.create(CIRCUIT);
    }

    /** The hour of the forecast: the one closest to the time of the plan. */
    static Instant hourOf(Instant at) {
        return at.plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.HOURS);
    }

    @Cacheable(cacheNames = WeatherConfig.CACHE, unless = "#result == null",
            key = "T(java.lang.String).format('%.2f:%.2f:%s', #latitude, #longitude, "
                    + "T(es.upm.miw.oneleft.plans.infrastructure.weather.OpenMeteoClient).hourOf(#at))")
    public Forecast forecast(double latitude, double longitude, Instant at) {
        return breaker.run(() -> request(latitude, longitude, hourOf(at)), error -> {
            log.warn("No weather forecast: {}", error.toString());
            return null;
        });
    }

    private Forecast request(double latitude, double longitude, Instant hour) {
        var time = HOUR.format(LocalDateTime.ofInstant(hour, ZoneOffset.UTC));
        var response = http.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", "temperature_2m,precipitation_probability,wind_speed_10m")
                        .queryParam("timezone", "GMT")
                        .queryParam("start_hour", time)
                        .queryParam("end_hour", time)
                        .build())
                .retrieve()
                .body(Response.class);
        if (response == null || response.hourly() == null || response.hourly().time() == null
                || response.hourly().time().isEmpty()) {
            return null;
        }
        var hourly = response.hourly();
        return new Forecast(hour, hourly.temperature_2m().getFirst(), hourly.precipitation_probability().getFirst(),
                hourly.wind_speed_10m().getFirst());
    }

    /** The part of the Open-Meteo answer that is used: one value per hour. */
    record Response(Hourly hourly) {
    }

    @SuppressWarnings("java:S116") // Field names of the Open-Meteo JSON
    record Hourly(List<String> time, List<Double> temperature_2m, List<Integer> precipitation_probability,
                  List<Double> wind_speed_10m) {
    }
}

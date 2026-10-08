package es.upm.miw.oneleft.plans.infrastructure.weather;

import es.upm.miw.oneleft.plans.domain.model.Forecast;
import es.upm.miw.oneleft.plans.domain.port.out.WeatherForecasts;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/** Output adapter: the weather forecast from Open-Meteo, through its cached and protected client. */
@Component
public class OpenMeteoForecasts implements WeatherForecasts {

    private final OpenMeteoClient client;

    public OpenMeteoForecasts(OpenMeteoClient client) {
        this.client = client;
    }

    /** Two decimals: about 1 km, the same forecast for the plans around. */
    private static double rounded(double coordinate) {
        return Math.round(coordinate * 100) / 100.0;
    }

    @Override
    public Optional<Forecast> forecast(double latitude, double longitude, Instant at) {
        return Optional.ofNullable(client.forecast(rounded(latitude), rounded(longitude), OpenMeteoClient.hourOf(at)));
    }
}

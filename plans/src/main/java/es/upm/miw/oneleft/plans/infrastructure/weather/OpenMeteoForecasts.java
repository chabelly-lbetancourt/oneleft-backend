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

    @Override
    public Optional<Forecast> forecast(double latitude, double longitude, Instant at) {
        return Optional.ofNullable(client.forecast(latitude, longitude, at));
    }
}

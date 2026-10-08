package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.Forecast;

import java.time.Instant;
import java.util.Optional;

/**
 * Output port to a weather service (HU-026). Empty when there is no forecast for that place and time or the service
 * does not answer: the plan is shown the same, without it.
 */
public interface WeatherForecasts {

    Optional<Forecast> forecast(double latitude, double longitude, Instant at);
}

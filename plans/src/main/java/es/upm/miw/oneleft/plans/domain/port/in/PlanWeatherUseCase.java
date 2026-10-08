package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Forecast;

import java.util.Optional;
import java.util.UUID;

/** Weather forecast of a plan (HU-026): only for upcoming outdoor plans. */
public interface PlanWeatherUseCase {

    Optional<Forecast> weather(UUID planId);
}

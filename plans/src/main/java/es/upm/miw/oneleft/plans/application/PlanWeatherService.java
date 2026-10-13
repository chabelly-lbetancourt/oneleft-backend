package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Forecast;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.port.in.PlanWeatherUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import es.upm.miw.oneleft.plans.domain.port.out.WeatherForecasts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Weather forecast of a plan (HU-026). Only outdoor plans that have not started: for the others, or when the weather
 * service fails, there is simply no forecast.
 */
@Service
public class PlanWeatherService implements PlanWeatherUseCase {

    private final PlanRepository plans;
    private final WeatherForecasts forecasts;

    public PlanWeatherService(PlanRepository plans, WeatherForecasts forecasts) {
        this.plans = plans;
        this.forecasts = forecasts;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Forecast> weather(UUID planId) {
        var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
        var upcoming = plan.status() == PlanStatus.OPEN || plan.status() == PlanStatus.FULL;
        if (!upcoming || !plan.activity().outdoor()) {
            return Optional.empty();
        }
        var point = plan.meetingPoint();
        return forecasts.forecast(point.latitude(), point.longitude(), plan.startsAt());
    }
}

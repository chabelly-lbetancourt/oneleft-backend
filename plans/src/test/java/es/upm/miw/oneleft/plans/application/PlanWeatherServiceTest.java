package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Forecast;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.port.out.WeatherForecasts;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-026: the forecast of a plan, only for upcoming outdoor plans. */
class PlanWeatherServiceTest {

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final List<Instant> asked = new ArrayList<>();
    private Optional<Forecast> answer = Optional.empty();
    private final WeatherForecasts forecasts = (latitude, longitude, at) -> {
        asked.add(at);
        return answer;
    };
    private final PlanWeatherService service = new PlanWeatherService(plans, forecasts);

    @Test
    void anUpcomingOutdoorPlanGetsTheForecastAtItsTimeAndPlace() {
        var plan = plans.save(padelPlan(ana(), Duration.ofHours(2), CLOCK));
        answer = Optional.of(new Forecast(plan.startsAt(), 17.4, 70, 12.6));

        assertThat(service.weather(plan.id())).isEqualTo(answer);
        assertThat(asked).containsExactly(plan.startsAt());
    }

    @Test
    void withoutAnswerFromTheServiceThereIsNoForecast() {
        var plan = plans.save(padelPlan(ana(), Duration.ofHours(2), CLOCK));

        assertThat(service.weather(plan.id())).isEmpty();
    }

    @Test
    void indoorAndStartedPlansHaveNoForecastAndTheServiceIsNotAsked() {
        var cinema = plans.save(Plan.publish(ana(), Activity.CINEMA, "Double feature", null, COURTS,
                CLOCK.instant().plus(Duration.ofHours(2)), 2, Level.BEGINNER, CLOCK));
        var started = plans.save(padelPlan(ana(), Duration.ofHours(1), CLOCK)
                .advance(CLOCK.instant().plus(Duration.ofHours(2))));

        assertThat(service.weather(cinema.id())).isEmpty();
        assertThat(service.weather(started.id())).isEmpty();
        assertThat(asked).isEmpty();
    }

    @Test
    void anUnknownPlanIsNotFound() {
        var unknown = UUID.randomUUID();
        assertThatThrownBy(() -> service.weather(unknown)).isInstanceOf(PlanNotFoundException.class);
    }
}

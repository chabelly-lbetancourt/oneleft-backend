package es.upm.miw.oneleft.plans.infrastructure.weather;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanController;
import es.upm.miw.oneleft.plans.infrastructure.rest.WeatherController;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-026 with the weather service simulated by WireMock, the cache in a real Redis and the circuit breaker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlanWeatherIntegrationTest {

    @RegisterExtension
    static final WireMockExtension OPEN_METEO = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private static final String FORECAST = "/v1/forecast";

    @DynamicPropertySource
    static void weatherService(DynamicPropertyRegistry registry) {
        registry.add("oneleft.weather.url", OPEN_METEO::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlanRepository plans;

    @Autowired
    private CacheManager caches;

    @Autowired
    private CircuitBreakerRegistry breakers;

    private final Clock clock = Clock.systemUTC();

    private static RequestPostProcessor user() {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("name", "Lucía"));
    }

    private ResultActions visit(String path) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(path).with(user()));
    }

    private static String weatherOf(Plan plan) {
        return PlanController.PLANS + WeatherController.WEATHER.replace("{planId}", plan.id().toString());
    }

    private Plan plan(Activity activity) {
        return plans.save(Plan.publish(ana(), activity, "Plan with weather", null, COURTS,
                clock.instant().plus(Duration.ofHours(2)), 2, Level.INTERMEDIATE, clock));
    }

    @BeforeEach
    void reset() {
        OPEN_METEO.resetAll();
        caches.getCache(WeatherConfig.CACHE).clear();
        breakers.circuitBreaker(OpenMeteoClient.CIRCUIT).reset();
    }

    @Test
    void anOutdoorPlanShowsTheForecastOfItsHourAndAsksOnlyOnce() throws Exception {
        var plan = plan(Activity.FOOTBALL);
        var hour = plan.startsAt().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.HOURS);
        var time = hour.toString().substring(0, 16);
        OPEN_METEO.stubFor(get(urlPathEqualTo(FORECAST)).willReturn(okJson("""
                {"hourly": {"time": ["%s"], "temperature_2m": [17.4], "precipitation_probability": [70],
                 "wind_speed_10m": [12.6]}}""".formatted(time))));

        for (var visit = 0; visit < 2; visit++) {
            visit(weatherOf(plan))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.time").value(hour.toString()))
                    .andExpect(jsonPath("$.temperature").value(17.4))
                    .andExpect(jsonPath("$.precipitationProbability").value(70))
                    .andExpect(jsonPath("$.windSpeed").value(12.6))
                    .andExpect(jsonPath("$.rainLikely").value(true));
        }

        // The second visit comes from the Redis cache
        OPEN_METEO.verify(1, getRequestedFor(urlPathEqualTo(FORECAST))
                .withQueryParam("start_hour", equalTo(time))
                .withQueryParam("hourly", containing("precipitation_probability")));
    }

    @Test
    void anIndoorPlanHasNoForecastAndTheServiceIsNotAsked() throws Exception {
        var plan = plan(Activity.CINEMA);

        visit(weatherOf(plan))
                .andExpect(status().isNoContent());

        OPEN_METEO.verify(0, getRequestedFor(urlPathEqualTo(FORECAST)));
    }

    @Test
    void whenTheServiceFailsThePlanIsShownWithoutForecast() throws Exception {
        var plan = plan(Activity.RUNNING);
        OPEN_METEO.stubFor(get(urlPathEqualTo(FORECAST)).willReturn(aResponse().withStatus(500)));

        visit(weatherOf(plan))
                .andExpect(status().isNoContent());
        visit(PlanController.PLANS + "/" + plan.id())
                .andExpect(status().isOk());
    }

    @Test
    void afterRepeatedFailuresTheCircuitOpensAndTheServiceIsLeftAlone() throws Exception {
        var plan = plan(Activity.CYCLING);
        OPEN_METEO.stubFor(get(urlPathEqualTo(FORECAST)).willReturn(aResponse().withStatus(503)));

        for (var visit = 0; visit < 8; visit++) {
            visit(weatherOf(plan))
                    .andExpect(status().isNoContent());
        }

        // Five failed calls open the circuit: the other three visits do not reach the service
        OPEN_METEO.verify(5, getRequestedFor(urlPathEqualTo(FORECAST)));
    }

    @Test
    void anUnknownPlanIsNotFound() throws Exception {
        visit(PlanController.PLANS + "/" + UUID.randomUUID() + "/weather")
                .andExpect(status().isNotFound());
    }
}

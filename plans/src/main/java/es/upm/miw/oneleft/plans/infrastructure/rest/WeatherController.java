package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Forecast;
import es.upm.miw.oneleft.plans.domain.port.in.PlanWeatherUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(PlanController.PLANS)
@Tag(name = "Plans", description = "Plans for the next few hours with free spots")
public class WeatherController {

    public static final String WEATHER = PlanController.PLAN + "/weather";

    private final PlanWeatherUseCase weather;

    public WeatherController(PlanWeatherUseCase weather) {
        this.weather = weather;
    }

    @GetMapping(WEATHER)
    @Operation(summary = "Weather forecast of a plan (HU-026)",
            description = "Forecast from Open-Meteo at the time and place of an upcoming outdoor plan. No content for "
                    + "indoor or started plans, or when the weather service does not answer.")
    @ApiResponse(responseCode = "200", description = "Forecast")
    @ApiResponse(responseCode = "204", description = "No forecast", content = @Content)
    @ApiResponse(responseCode = "404", description = "plan.notFound", content = @Content)
    public ResponseEntity<WeatherResponse> weather(@PathVariable UUID planId) {
        return weather.weather(planId).map(WeatherResponse::of).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Schema(description = "Weather forecast at the time and place of the plan")
    public record WeatherResponse(@Schema(description = "Hour of the forecast") Instant time,
                                  @Schema(description = "°C", example = "17.4") double temperature,
                                  @Schema(description = "Chance of rain, %", example = "70") int precipitationProbability,
                                  @Schema(description = "km/h", example = "12.6") double windSpeed,
                                  @Schema(description = "Chance of rain of 60 % or more") boolean rainLikely) {

        static WeatherResponse of(Forecast forecast) {
            return new WeatherResponse(forecast.time(), forecast.temperature(), forecast.precipitationProbability(),
                    forecast.windSpeed(), forecast.rainLikely());
        }
    }
}

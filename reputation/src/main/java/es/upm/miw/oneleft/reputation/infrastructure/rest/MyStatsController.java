package es.upm.miw.oneleft.reputation.infrastructure.rest;

import es.upm.miw.oneleft.reputation.domain.model.ActivityPlans;
import es.upm.miw.oneleft.reputation.domain.model.MonthPlans;
import es.upm.miw.oneleft.reputation.domain.model.MyStats;
import es.upm.miw.oneleft.reputation.domain.model.StatsQuery;
import es.upm.miw.oneleft.reputation.domain.port.in.MyStatsUseCase;
import es.upm.miw.oneleft.reputation.infrastructure.config.KeycloakJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** My statistics (HU-043): only mine, so the person is the one of the token and never part of the path. */
@RestController
@RequestMapping(ReputationController.REPUTATION)
@Tag(name = "My statistics", description = "How much and what I play")
public class MyStatsController {

    public static final String MY_STATS = ReputationController.ME + "/stats";

    private final MyStatsUseCase stats;

    public MyStatsController(MyStatsUseCase stats) {
        this.stats = stats;
    }

    @GetMapping(MY_STATS)
    @Operation(summary = "My statistics (HU-043)",
            description = "The plans I arrived at this month, my favourite activity, the plans I organized against "
                    + "those of someone else I joined, the activities I have done the most and the latest months. "
                    + "Only plans that were held count, with a check-in, as for the badges and the statistics of a "
                    + "person (HU-048); months are those of Spain. Worked out from the attendance events: nothing is "
                    + "kept twice.")
    @ApiResponse(responseCode = "200", description = "My statistics")
    @ApiResponse(responseCode = "400", description = "stats.invalid: 1 to 12 months and 1 to 20 activities",
            content = @Content)
    public MyStatsResponse mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                @Parameter(description = "The latest months to bring, this one included (6)")
                                @RequestParam(required = false) Integer months,
                                @Parameter(description = "The activities to bring, the most done first (5)")
                                @RequestParam(required = false) Integer activities) {
        var query = new StatsQuery(months == null ? StatsQuery.DEFAULT_MONTHS : months,
                activities == null ? StatsQuery.DEFAULT_ACTIVITIES : activities);
        return MyStatsResponse.of(stats.myStats(KeycloakJwt.userId(jwt), query));
    }

    @Schema(description = "My statistics: how much and what I play")
    public record MyStatsResponse(@Schema(description = "The plans I arrived at this month", example = "6")
                                  int plansThisMonth,
                                  @Schema(description = "The plans I arrived at, ever", example = "42")
                                  int plansCompleted,
                                  @Schema(description = "The plans I organized that were held, ever", example = "12")
                                  int plansOrganized,
                                  @Schema(description = "The plans of someone else I arrived at, ever",
                                          example = "30") int plansJoined,
                                  @Schema(description = "The activity with the most plans; among equals, the one "
                                          + "done the latest. Null without plans", example = "PADEL")
                                  String favoriteActivity,
                                  @Schema(description = "The activities I have done the most, the most first")
                                  List<ActivityResponse> activities,
                                  @Schema(description = "The latest months, the oldest first and this one the last, "
                                          + "also those without plans") List<MonthResponse> months) {

        static MyStatsResponse of(MyStats stats) {
            return new MyStatsResponse(stats.plansThisMonth(), stats.plansCompleted(), stats.plansOrganized(),
                    stats.plansJoined(), stats.favoriteActivity().orElse(null),
                    stats.activities().stream().map(ActivityResponse::of).toList(),
                    stats.months().stream().map(MonthResponse::of).toList());
        }
    }

    @Schema(description = "The plans I arrived at of one activity")
    public record ActivityResponse(@Schema(example = "PADEL") String activity, @Schema(example = "17") int plans) {

        static ActivityResponse of(ActivityPlans activity) {
            return new ActivityResponse(activity.activity(), activity.plans());
        }
    }

    @Schema(description = "My plans in one month")
    public record MonthResponse(@Schema(description = "The year and the month", example = "2026-11") String month,
                                @Schema(description = "The plans I arrived at", example = "6") int plans,
                                @Schema(description = "The plans I organized that were held", example = "2")
                                int organized,
                                @Schema(description = "The plans of someone else I arrived at", example = "4")
                                int joined) {

        static MonthResponse of(MonthPlans month) {
            return new MonthResponse(month.month().toString(), month.completed(), month.organized(), month.joined());
        }
    }
}

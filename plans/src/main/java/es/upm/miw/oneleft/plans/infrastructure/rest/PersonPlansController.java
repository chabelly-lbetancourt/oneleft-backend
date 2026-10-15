package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.RecentPlan;
import es.upm.miw.oneleft.plans.domain.port.in.PersonPlansUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(PlanController.PLANS)
@Tag(name = "Plans", description = "Plans for the next few hours with free spots")
public class PersonPlansController {

    public static final String RECENT = "/people/{userId}/recent";

    private final PersonPlansUseCase personPlans;

    public PersonPlansController(PersonPlansUseCase personPlans) {
        this.personPlans = personPlans;
    }

    @GetMapping(RECENT)
    @Operation(summary = "The last plans of someone (HU-048)",
            description = "The last 5 plans held that they organized or arrived at, the latest first, without the "
                    + "meeting point. Only if their profile is public, it is mine or I follow them.")
    @ApiResponse(responseCode = "200", description = "Their recent plans")
    @ApiResponse(responseCode = "403", description = "social.private", content = @Content)
    @ApiResponse(responseCode = "404", description = "person.notFound", content = @Content)
    @ApiResponse(responseCode = "503", description = "social.unavailable", content = @Content)
    public List<RecentPlanResponse> recent(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                           @PathVariable UUID userId) {
        return personPlans.recentPlansOf(UUID.fromString(jwt.getSubject()), userId).stream()
                .map(RecentPlanResponse::of).toList();
    }

    @Schema(description = "A plan someone took part in: what and when, never where")
    public record RecentPlanResponse(UUID planId, @Schema(example = "Rodaje suave por el parque") String title,
                                     Activity activity, Instant startsAt,
                                     @Schema(description = "ORGANIZER or ATTENDEE") String role) {

        static RecentPlanResponse of(RecentPlan plan) {
            return new RecentPlanResponse(plan.planId(), plan.title(), plan.activity(), plan.startsAt(),
                    plan.organizer() ? "ORGANIZER" : "ATTENDEE");
        }
    }
}

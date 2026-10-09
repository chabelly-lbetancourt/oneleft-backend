package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.port.in.ArrivalUseCase;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.ArrivalRequest;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** «On my way» and «running late» (HU-040). */
@RestController
@RequestMapping(PlanController.PLANS)
@Tag(name = "Plans", description = "Plans for the next few hours with free spots")
public class ArrivalController {

    public static final String MY_ARRIVAL = PlanController.PLAN + "/arrivals/me";

    private final ArrivalUseCase arrivals;

    public ArrivalController(ArrivalUseCase arrivals) {
        this.arrivals = arrivals;
    }

    @PutMapping(MY_ARRIVAL)
    @Operation(summary = "I'm on my way, or running late (HU-040)",
            description = "Only the organizer and the participants, until the plan starts. It replaces my previous "
                    + "status, and the rest of the group gets `plan-arrival` in their real-time stream.")
    @ApiResponse(responseCode = "200", description = "The plan with the statuses of the group")
    @ApiResponse(responseCode = "400", description = "arrival.minutes", content = @Content)
    @ApiResponse(responseCode = "409", description = "plan.notParticipant or plan.started", content = @Content)
    public PlanResponse announce(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                 @PathVariable UUID planId, @Valid @RequestBody ArrivalRequest request) {
        var userId = UUID.fromString(jwt.getSubject());
        return PlanResponse.of(arrivals.announce(planId, userId, request.status(), request.minutesLate()), userId);
    }

    @DeleteMapping(MY_ARRIVAL)
    @Operation(summary = "Take back my status of arrival (HU-040)")
    @ApiResponse(responseCode = "200", description = "The plan with the statuses of the group")
    @ApiResponse(responseCode = "409", description = "plan.notParticipant or plan.started", content = @Content)
    public PlanResponse clear(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId) {
        var userId = UUID.fromString(jwt.getSubject());
        return PlanResponse.of(arrivals.clear(planId, userId), userId);
    }
}

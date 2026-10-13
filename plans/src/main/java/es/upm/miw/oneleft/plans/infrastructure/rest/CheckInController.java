package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.port.in.CheckInUseCase;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.CheckInRequest;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.CheckInResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Check-in at the meeting point (HU-025). */
@RestController
@RequestMapping(PlanController.PLANS)
@Tag(name = "Plans", description = "Plans for the next few hours with free spots")
public class CheckInController {

    public static final String CHECK_INS = PlanController.PLAN + "/check-ins";
    public static final String MY_CHECK_IN = CHECK_INS + "/me";

    private final CheckInUseCase checkIns;

    public CheckInController(CheckInUseCase checkIns) {
        this.checkIns = checkIns;
    }

    @PostMapping(MY_CHECK_IN)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "I'm at the meeting point (HU-025)",
            description = "Only the organizer and the participants, from 15 minutes before the start to 30 minutes "
                    + "after it, and within 150 m of the meeting point. The location is only used for that check "
                    + "and is not stored. Checking in again returns the first check-in. The rest of the group gets "
                    + "`plan-check-in` in their real-time stream.")
    @ApiResponse(responseCode = "201", description = "My check-in")
    @ApiResponse(responseCode = "400", description = "Coordinates missing or out of range", content = @Content)
    @ApiResponse(responseCode = "404", description = "plan.notFound", content = @Content)
    @ApiResponse(responseCode = "409", description = "plan.notParticipant, plan.notOpen, checkIn.notYet, "
            + "checkIn.closed or checkIn.tooFar", content = @Content)
    public CheckInResponse checkIn(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID planId, @Valid @RequestBody CheckInRequest request) {
        var userId = UUID.fromString(jwt.getSubject());
        return CheckInResponse.of(checkIns.checkIn(planId, userId, request.toDomain()));
    }

    @GetMapping(CHECK_INS)
    @Operation(summary = "Who has arrived at the meeting point (HU-025)",
            description = "Only the organizer and the participants, in order of arrival.")
    @ApiResponse(responseCode = "200", description = "The check-ins of the group")
    @ApiResponse(responseCode = "404", description = "plan.notFound", content = @Content)
    @ApiResponse(responseCode = "409", description = "plan.notParticipant", content = @Content)
    public List<CheckInResponse> checkIns(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID planId) {
        var userId = UUID.fromString(jwt.getSubject());
        return checkIns.checkIns(planId, userId).stream().map(CheckInResponse::of).toList();
    }
}

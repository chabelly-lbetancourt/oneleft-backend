package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.JoinRequest;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.RequestStatus;
import es.upm.miw.oneleft.plans.domain.port.in.ManageJoinRequestsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Requests for a spot in plans whose organizer chooses who comes (HU-069). */
@RestController
@RequestMapping(PlanController.PLANS + PlanController.PLAN + "/requests")
@Tag(name = "Plans", description = "Publish and look up plans")
public class JoinRequestController {

    private final ManageJoinRequestsUseCase requests;

    public JoinRequestController(ManageJoinRequestsUseCase requests) {
        this.requests = requests;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ask for a spot (HU-069)",
            description = "In a plan whose organizer chooses who comes, with an optional short message. The organizer "
                    + "is told in real time. Someone whose request was rejected cannot ask again; whoever withdrew can.")
    @ApiResponse(responseCode = "201", description = "Request sent")
    @ApiResponse(responseCode = "409", description = "plan.approvalNotRequired, plan.alreadyRequested, "
            + "plan.requestRejected, plan.full, plan.started, plan.ownPlan, plan.alreadyJoined", content = @Content)
    public JoinRequestDto ask(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                              @PathVariable UUID planId, @Valid @RequestBody(required = false) RequestMessage body) {
        var userId = UUID.fromString(jwt.getSubject());
        var plan = requests.request(planId, userId, jwt.getClaimAsString("name"), body == null ? null : body.message());
        return JoinRequestDto.of(plan, plan.requestOf(userId).orElseThrow());
    }

    @GetMapping
    @Operation(summary = "The requests of my plan (HU-069)", description = "Only for the organizer. Name and message: "
            + "no contact data or location.")
    @ApiResponse(responseCode = "403", description = "plan.notOrganizer", content = @Content)
    public List<JoinRequestDto> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID planId) {
        var plan = requests.requestsOf(planId, UUID.fromString(jwt.getSubject()));
        return plan.requests().stream().map(request -> JoinRequestDto.of(plan, request)).toList();
    }

    @PostMapping("/{userId}/accept")
    @Operation(summary = "Accept a request (HU-069)",
            description = "The person takes the spot and is told. If the plan fills up, the other requests are "
                    + "rejected on their own with a kind notice.")
    public JoinRequestDto accept(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                 @PathVariable UUID planId, @PathVariable UUID userId) {
        var decision = requests.accept(planId, UUID.fromString(jwt.getSubject()), userId);
        return JoinRequestDto.of(decision.plan(), decision.request());
    }

    @PostMapping("/{userId}/reject")
    @Operation(summary = "Reject a request (HU-069)")
    public JoinRequestDto reject(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                 @PathVariable UUID planId, @PathVariable UUID userId) {
        var decision = requests.reject(planId, UUID.fromString(jwt.getSubject()), userId);
        return JoinRequestDto.of(decision.plan(), decision.request());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Withdraw my request (HU-069)")
    public void withdraw(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId) {
        requests.withdraw(planId, UUID.fromString(jwt.getSubject()));
    }

    @Schema(description = "A short message with the request")
    public record RequestMessage(@Size(max = JoinRequest.MAX_MESSAGE_LENGTH) @Schema(example = "¡Me encantaría!")
                              String message) {
    }

    @Schema(description = "A request for a spot")
    public record JoinRequestDto(UUID userId, @Schema(example = "Lucía") String name, String message,
                                 RequestStatus status, Instant requestedAt, Instant decidedAt,
                                 @Schema(example = "25–34", description = "Age band of the person when they asked; null if unknown") String ageBand) {

        /** The status is the one now: a pending request expires when the plan starts. */
        static JoinRequestDto of(Plan plan, JoinRequest request) {
            return new JoinRequestDto(request.userId(), request.name(), request.message(),
                    plan.statusOf(request, Instant.now()), request.requestedAt(), request.decidedAt(),
                    request.ageBand());
        }
    }
}

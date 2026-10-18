package es.upm.miw.oneleft.reputation.infrastructure.rest;

import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
import es.upm.miw.oneleft.reputation.domain.model.PlanKudos;
import es.upm.miw.oneleft.reputation.domain.port.in.KudosUseCase;
import es.upm.miw.oneleft.reputation.infrastructure.config.KeycloakJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Kudos between the people of a plan (HU-060). */
@RestController
@RequestMapping(ReputationController.REPUTATION)
@Tag(name = "Kudos", description = "Recognition between the people who arrived at the same plan")
public class KudosController {

    public static final String PLAN_KUDOS = "/plans/{planId}/kudos";
    public static final String PERSON_KUDOS = ReputationController.PERSON + "/kudos";

    private final KudosUseCase kudos;

    public KudosController(KudosUseCase kudos) {
        this.kudos = kudos;
    }

    @PostMapping(PLAN_KUDOS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Give a kudos to someone of the plan",
            description = "To someone else who arrived at the plan (check-in), if I arrived too, up to a week after "
                    + "it; GREAT_ORGANIZER only to its organizer. One per person and plan: +5 in their reputation and "
                    + "a notice in real time.")
    @ApiResponse(responseCode = "201", description = "The kudos")
    @ApiResponse(responseCode = "400", description = "kudos.self, kudos.notOrganizer or kudos.receiverNotAttended",
            content = @Content)
    @ApiResponse(responseCode = "403", description = "kudos.notAttended: I did not arrive at the plan",
            content = @Content)
    @ApiResponse(responseCode = "409", description = "kudos.alreadyGiven or kudos.closed", content = @Content)
    public KudosResponse give(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId,
                              @Valid @RequestBody GiveKudosRequest request) {
        return KudosResponse.of(kudos.give(KeycloakJwt.userId(jwt), KeycloakJwt.name(jwt), planId,
                request.receiverId(), request.kind()));
    }

    @GetMapping(PLAN_KUDOS)
    @Operation(summary = "The kudos of a plan for me",
            description = "Whether I can still give them and until when, and the others who arrived, with the kudos I "
                    + "gave each one. Empty if I did not arrive.")
    @ApiResponse(responseCode = "200", description = "The kudos of the plan")
    public PlanKudosResponse ofPlan(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                    @PathVariable UUID planId) {
        return PlanKudosResponse.of(kudos.kudosOf(KeycloakJwt.userId(jwt), planId));
    }

    @GetMapping(PERSON_KUDOS)
    @Operation(summary = "The kudos someone has received", description = "By kind, every kind with its count.")
    @ApiResponse(responseCode = "200", description = "The counts")
    public List<KindCountResponse> receivedBy(@PathVariable UUID userId) {
        return kudos.receivedBy(userId).entrySet().stream()
                .map(count -> new KindCountResponse(count.getKey(), count.getValue())).toList();
    }

    @Schema(description = "A kudos for someone who arrived at the plan")
    public record GiveKudosRequest(@NotNull UUID receiverId, @NotNull KudosKind kind) {
    }

    @Schema(description = "A kudos given")
    public record KudosResponse(UUID receiverId, UUID planId, KudosKind kind, Instant givenAt) {

        static KudosResponse of(Kudos kudos) {
            return new KudosResponse(kudos.receiverId(), kudos.planId(), kudos.kind(), kudos.givenAt());
        }
    }

    @Schema(description = "The kudos of a plan for me")
    public record PlanKudosResponse(@Schema(description = "I can still give kudos") boolean open,
                                    @Schema(description = "Until when; null if I did not arrive") Instant until,
                                    List<PersonResponse> people) {

        static PlanKudosResponse of(PlanKudos plan) {
            return new PlanKudosResponse(plan.open(), plan.until(), plan.people().stream()
                    .map(person -> new PersonResponse(person.userId(), person.organizer(), person.given())).toList());
        }
    }

    @Schema(description = "Someone else who arrived at the plan")
    public record PersonResponse(UUID userId, boolean organizer,
                                 @Schema(description = "The kudos I gave them, or null") KudosKind given) {
    }

    @Schema(description = "How many kudos of a kind")
    public record KindCountResponse(KudosKind kind, long count) {
    }
}

package es.upm.miw.oneleft.reputation.infrastructure.rest;

import es.upm.miw.oneleft.reputation.domain.model.Endorsement;
import es.upm.miw.oneleft.reputation.domain.model.EndorsementCount;
import es.upm.miw.oneleft.reputation.domain.model.PersonEndorsements;
import es.upm.miw.oneleft.reputation.domain.model.SkillLevel;
import es.upm.miw.oneleft.reputation.domain.port.in.EndorsementsUseCase;
import es.upm.miw.oneleft.reputation.infrastructure.config.KeycloakJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

/** Endorsements of the level of someone in an activity (HU-061). */
@RestController
@RequestMapping(ReputationController.REPUTATION)
@Tag(name = "Endorsements", description = "Whoever played with someone vouches for their level in an activity")
public class EndorsementController {

    public static final String PERSON_ENDORSEMENTS = ReputationController.PERSON + "/endorsements";

    private final EndorsementsUseCase endorsements;

    public EndorsementController(EndorsementsUseCase endorsements) {
        this.endorsements = endorsements;
    }

    @PostMapping(PERSON_ENDORSEMENTS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Endorse the level of someone in an activity",
            description = "Only if we both arrived (check-in) at a plan of that activity. One per person and "
                    + "activity, for good, with the level I vouch for: +8 in their reputation, half each time for "
                    + "the same person within 30 days, nothing between a pair that keeps endorsing each other, and "
                    + "at most 5 a day that give points (HU-066). The endorsement is kept even if it gives nothing.")
    @ApiResponse(responseCode = "201", description = "The endorsement")
    @ApiResponse(responseCode = "400", description = "endorsement.self, or validation", content = @Content)
    @ApiResponse(responseCode = "403", description = "endorsement.notPlayedTogether: we never arrived at the same "
            + "plan of that activity", content = @Content)
    @ApiResponse(responseCode = "409", description = "endorsement.alreadyGiven", content = @Content)
    public EndorsementResponse endorse(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @PathVariable UUID userId, @Valid @RequestBody EndorseRequest request) {
        return EndorsementResponse.of(endorsements.endorse(KeycloakJwt.userId(jwt), userId, request.activity(),
                request.level()));
    }

    @GetMapping(PERSON_ENDORSEMENTS)
    @Operation(summary = "The endorsements of someone",
            description = "How many people vouch for each level of each of their activities («Intermediate in padel "
                    + "(endorsed by 7)»: the count of the level they declare), the ones I gave them, and the "
                    + "activities I can still endorse them in. Never who the others are.")
    @ApiResponse(responseCode = "200", description = "Their endorsements")
    public PersonEndorsementsResponse of(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                         @PathVariable UUID userId) {
        return PersonEndorsementsResponse.of(userId, endorsements.endorsementsOf(KeycloakJwt.userId(jwt), userId));
    }

    @Schema(description = "The level I vouch for in an activity")
    public record EndorseRequest(@Schema(description = "The code of the activity", example = "PADEL")
                                 @NotBlank @Size(max = Endorsement.MAX_ACTIVITY_LENGTH) String activity,
                                 @Schema(description = "The level they declare, which I vouch for")
                                 @NotNull SkillLevel level) {
    }

    @Schema(description = "An endorsement given")
    public record EndorsementResponse(UUID receiverId, @Schema(example = "PADEL") String activity, SkillLevel level,
                                      Instant givenAt) {

        static EndorsementResponse of(Endorsement endorsement) {
            return new EndorsementResponse(endorsement.receiverId(), endorsement.activity(), endorsement.level(),
                    endorsement.givenAt());
        }
    }

    @Schema(description = "The endorsements of someone, as I see them")
    public record PersonEndorsementsResponse(UUID userId,
                                             @Schema(description = "By activity and level, in that order")
                                             List<CountResponse> endorsements,
                                             @Schema(description = "The ones I gave them") List<MineResponse> mine,
                                             @Schema(description = "The activities I can still endorse them in: we "
                                                     + "arrived at a plan of it and I have not yet. Empty for myself")
                                             List<String> endorsable) {

        static PersonEndorsementsResponse of(UUID userId, PersonEndorsements person) {
            return new PersonEndorsementsResponse(userId,
                    person.counts().stream().map(CountResponse::of).toList(),
                    person.mine().stream().map(MineResponse::of).toList(), person.endorsable());
        }
    }

    @Schema(description = "How many people vouch for a level in an activity")
    public record CountResponse(@Schema(example = "PADEL") String activity, SkillLevel level,
                                @Schema(example = "7") long count) {

        static CountResponse of(EndorsementCount count) {
            return new CountResponse(count.activity(), count.level(), count.count());
        }
    }

    @Schema(description = "An endorsement I gave them")
    public record MineResponse(@Schema(example = "PADEL") String activity, SkillLevel level, Instant givenAt) {

        static MineResponse of(Endorsement endorsement) {
            return new MineResponse(endorsement.activity(), endorsement.level(), endorsement.givenAt());
        }
    }
}

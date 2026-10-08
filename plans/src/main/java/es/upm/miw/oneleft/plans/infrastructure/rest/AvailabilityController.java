package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Availability;
import es.upm.miw.oneleft.plans.domain.model.FreePerson;
import es.upm.miw.oneleft.plans.domain.model.Interest;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.port.in.AvailabilityUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** «I'm free now» (HU-035). */
@RestController
@RequestMapping(PlanController.PLANS)
@Tag(name = "Plans", description = "Plans for the next few hours with free spots")
public class AvailabilityController {

    public static final String MY_AVAILABILITY = "/availability/me";
    public static final String FREE_PEOPLE = PlanController.PLAN + "/free-people";

    private final AvailabilityUseCase availability;

    public AvailabilityController(AvailabilityUseCase availability) {
        this.availability = availability;
    }

    @PutMapping(MY_AVAILABILITY)
    @Operation(summary = "I'm free now (HU-035)",
            description = "Free mode for 1 to 3 hours around an approximate zone (rounded to about 1 km), for some "
                    + "activities (none for any). It replaces the previous one and ends when it expires, when I turn "
                    + "it off or when I join a plan.")
    @ApiResponse(responseCode = "200", description = "Free mode on")
    @ApiResponse(responseCode = "400", description = "availability.hours or availability.zone", content = @Content)
    public AvailabilityResponse start(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody AvailabilityRequest request) {
        var interests = request.interests() == null ? Set.<Interest>of() : request.interests().stream()
                .map(interest -> new Interest(interest.activity(), interest.level())).collect(Collectors.toSet());
        return AvailabilityResponse.of(availability.start(UUID.fromString(jwt.getSubject()), request.latitude(),
                request.longitude(), request.hours(), interests));
    }

    @GetMapping(MY_AVAILABILITY)
    @Operation(summary = "My free mode", description = "No content when it is off or has expired.")
    @ApiResponse(responseCode = "200", description = "Free mode on")
    @ApiResponse(responseCode = "204", description = "Free mode off", content = @Content)
    public ResponseEntity<AvailabilityResponse> mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return availability.mine(UUID.fromString(jwt.getSubject())).map(AvailabilityResponse::of)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping(MY_AVAILABILITY)
    @Operation(summary = "I'm no longer free")
    @ApiResponse(responseCode = "204", description = "Free mode off")
    public ResponseEntity<Void> stop(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        availability.stop(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping(FREE_PEOPLE)
    @Operation(summary = "Free people near my plan (HU-035)",
            description = "Only for the organizer of an upcoming plan: people in free mode less than 5 km away who "
                    + "would do its activity, without names and with the distance rounded to 500 m. Empty for "
                    + "anyone else.")
    @ApiResponse(responseCode = "200", description = "Free people, nearest first")
    @ApiResponse(responseCode = "404", description = "plan.notFound", content = @Content)
    public List<FreePersonResponse> freePeople(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                               @PathVariable UUID planId) {
        return availability.freePeopleNear(planId, UUID.fromString(jwt.getSubject())).stream()
                .map(FreePersonResponse::of).toList();
    }

    @Schema(description = "An activity I would do and my level in it")
    public record InterestDto(@NotNull @Schema(example = "PADEL") Activity activity,
                              @Schema(example = "INTERMEDIATE") Level level) {
    }

    @Schema(description = "Free mode to turn on")
    public record AvailabilityRequest(
            @Schema(description = "1 to 3", example = "2") int hours,
            @NotNull @DecimalMin("-90") @DecimalMax("90") @Schema(example = "40.39") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") @Schema(example = "-3.63") Double longitude,
            @Schema(description = "None for any activity") List<@Valid InterestDto> interests) {
    }

    @Schema(description = "My free mode")
    public record AvailabilityResponse(Instant until, double latitude, double longitude, List<InterestDto> interests) {

        static AvailabilityResponse of(Availability availability) {
            return new AvailabilityResponse(availability.until(), availability.latitude(), availability.longitude(),
                    availability.interests().stream().map(interest -> new InterestDto(interest.activity(),
                            interest.level())).toList());
        }
    }

    @Schema(description = "Someone free near the plan: no name and no place")
    public record FreePersonResponse(@Schema(description = "Rounded to 500 m", example = "1500") long distanceMeters,
                                     @Schema(description = "In the activity of the plan; null if not said")
                                     Level level,
                                     @Schema(description = "What they would do") Set<Activity> activities) {

        static FreePersonResponse of(FreePerson person) {
            return new FreePersonResponse(person.distanceMeters(), person.level(), person.activities());
        }
    }
}

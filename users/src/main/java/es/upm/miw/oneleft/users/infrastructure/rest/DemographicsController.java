package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.model.AgeBand;
import es.upm.miw.oneleft.users.domain.model.Demographics;
import es.upm.miw.oneleft.users.domain.model.Gender;
import es.upm.miw.oneleft.users.domain.model.ProfileNotFoundException;
import es.upm.miw.oneleft.users.domain.port.in.ManageDemographicsUseCase;
import es.upm.miw.oneleft.users.domain.port.in.ManageProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Date of birth and gender of the signed-in person (HU-070). */
@RestController
@RequestMapping(UserController.USERS + "/me/demographics")
@Tag(name = "Profile", description = "Own profile")
public class DemographicsController {

    private final ManageDemographicsUseCase demographics;
    private final ManageProfileUseCase profiles;
    private final Clock clock;

    public DemographicsController(ManageDemographicsUseCase demographics, ManageProfileUseCase profiles, Clock clock) {
        this.demographics = demographics;
        this.profiles = profiles;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "My date of birth and gender (HU-070)",
            description = "404 while they are not set. To publish or join a plan they are required and the person must "
                    + "be of age. Other services only get the gender and the age, never the date.")
    public DemographicsResponse mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return demographics.mine(UUID.fromString(jwt.getSubject())).map(this::response)
                .orElseThrow(DemographicsNotSetException::new);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Set or change my date of birth and gender (HU-070)",
            description = "18+ only. The date of birth can be changed only a couple of times.")
    public DemographicsResponse save(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody DemographicsRequest request) {
        return response(demographics.save(UUID.fromString(jwt.getSubject()), request.birthDate(), request.gender()));
    }

    /** What the plans service asks to decide whether I fit a plan: gender and age, never the date (HU-070). */
    @GetMapping("/audience")
    @Operation(summary = "What a plan needs to know about me (HU-070)",
            description = "Gender, age in years and age band: the minimum to decide whether I fit the audience of a plan. 404 "
                    + "while they are not set.")
    public AudienceResponse audience(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return demographics.mine(UUID.fromString(jwt.getSubject()))
                .map(found -> new AudienceResponse(found.gender(), found.age(clock), found.ageBand(clock).label(),
                        levelsOf(UUID.fromString(jwt.getSubject()))))
                .orElseThrow(DemographicsNotSetException::new);
    }

    /** My level in each activity of my hobbies; none if I have no profile yet. */
    private Map<String, String> levelsOf(UUID userId) {
        try {
            return profiles.profileOf(userId).hobbies().stream()
                    .collect(Collectors.toMap(hobby -> hobby.activity().name(), hobby -> hobby.level().name()));
        } catch (ProfileNotFoundException notFound) {
            return Map.of();
        }
    }

    private DemographicsResponse response(Demographics found) {
        return new DemographicsResponse(found.birthDate(), found.gender(), AgeBandDto.of(found.ageBand(clock)),
                Demographics.MAX_BIRTH_DATE_CHANGES - found.changes());
    }

    @Schema(description = "Date of birth and gender")
    public record DemographicsRequest(@NotNull @Past @Schema(example = "1995-04-12") LocalDate birthDate,
                                      @NotNull @Schema(example = "WOMAN") Gender gender) {
    }

    @Schema(description = "My date of birth and gender; others only ever see the age band")
    public record DemographicsResponse(LocalDate birthDate, Gender gender, AgeBandDto ageBand,
                                       @Schema(description = "How many times the date of birth can still change")
                                       int changesLeft) {
    }

    @Schema(description = "The age band shown to other people")
    public record AgeBandDto(@Schema(example = "25") int from, @Schema(example = "34") Integer to,
                             @Schema(example = "25–34") String label) {

        static AgeBandDto of(AgeBand band) {
            return new AgeBandDto(band.from(), band.to(), band.label());
        }
    }

    @Schema(description = "Gender and age, for the plans service")
    public record AudienceResponse(Gender gender, int age, String ageBand, Map<String, String> levels) {
    }

    /** Nothing set yet. */
    static class DemographicsNotSetException extends RuntimeException {
        DemographicsNotSetException() {
            super("The date of birth and the gender are not set");
        }
    }
}

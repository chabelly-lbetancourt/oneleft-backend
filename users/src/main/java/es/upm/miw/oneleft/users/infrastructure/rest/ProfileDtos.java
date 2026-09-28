package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.model.Activity;
import es.upm.miw.oneleft.users.domain.model.ApproximateZone;
import es.upm.miw.oneleft.users.domain.model.Hobby;
import es.upm.miw.oneleft.users.domain.model.Level;
import es.upm.miw.oneleft.users.domain.model.Profile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTOs of the profiles API.
 */
public final class ProfileDtos {

    private ProfileDtos() {
    }

    @Schema(description = "Usual zone. Coordinates are rounded to 2 decimals (~1.1 km) when stored")
    public record ZoneDto(
            @NotBlank @Size(max = ApproximateZone.MAX_NAME_LENGTH) @Schema(example = "Vallecas") String name,
            @NotNull @DecimalMin("-90") @DecimalMax("90") @Schema(example = "40.39") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") @Schema(example = "-3.62") Double longitude) {

        static ZoneDto of(ApproximateZone zone) {
            return zone == null ? null : new ZoneDto(zone.name(), zone.latitude(), zone.longitude());
        }

        ApproximateZone toDomain() {
            return new ApproximateZone(name, latitude, longitude);
        }
    }

    @Schema(description = "Hobby with its level")
    public record HobbyDto(@NotNull @Schema(example = "PADEL") Activity activity,
                           @NotNull @Schema(example = "INTERMEDIATE") Level level) {

        static HobbyDto of(Hobby hobby) {
            return new HobbyDto(hobby.activity(), hobby.level());
        }

        Hobby toDomain() {
            return new Hobby(activity, level);
        }
    }

    @Schema(description = "Own profile, with the approximate zone")
    public record ProfileResponse(UUID userId, @Schema(example = "Ana") String displayName, ZoneDto zone,
                                  List<HobbyDto> hobbies) {

        static ProfileResponse of(Profile profile) {
            return new ProfileResponse(profile.userId(), profile.displayName(), ZoneDto.of(profile.zone()),
                    profile.hobbies().stream().map(HobbyDto::of).toList());
        }
    }

    @Schema(description = "Profile visible to other participants: no coordinates")
    public record PublicProfileResponse(UUID userId, @Schema(example = "Ana") String displayName,
                                        @Schema(example = "Vallecas") String zoneName, List<HobbyDto> hobbies) {

        static PublicProfileResponse of(Profile profile) {
            return new PublicProfileResponse(profile.userId(), profile.displayName(),
                    profile.zone() == null ? null : profile.zone().name(),
                    profile.hobbies().stream().map(HobbyDto::of).toList());
        }
    }

    @Schema(description = "Changes to the own profile")
    public record UpdateProfileRequest(
            @NotBlank @Size(max = Profile.MAX_DISPLAY_NAME_LENGTH) @Schema(example = "Ana") String displayName,
            @Valid ZoneDto zone,
            @NotNull @Size(max = Profile.MAX_HOBBIES) List<@Valid @NotNull HobbyDto> hobbies) {
    }

    @Schema(description = "Catalog of activities and levels. Clients translate the codes")
    public record CatalogResponse(List<Activity> activities, List<Level> levels) {

        static CatalogResponse create() {
            return new CatalogResponse(List.of(Activity.values()), List.of(Level.values()));
        }
    }
}

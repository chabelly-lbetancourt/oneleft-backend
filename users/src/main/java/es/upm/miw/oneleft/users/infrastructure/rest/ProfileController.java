package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.port.in.ManageProfileUseCase;
import es.upm.miw.oneleft.users.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.users.infrastructure.rest.ProfileDtos.CatalogResponse;
import es.upm.miw.oneleft.users.infrastructure.rest.ProfileDtos.HobbyDto;
import es.upm.miw.oneleft.users.infrastructure.rest.ProfileDtos.ProfileResponse;
import es.upm.miw.oneleft.users.infrastructure.rest.ProfileDtos.PublicProfileResponse;
import es.upm.miw.oneleft.users.infrastructure.rest.ProfileDtos.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(UserController.USERS)
@Tag(name = "Profiles", description = "Display name, approximate zone and hobbies (HU-002)")
public class ProfileController {

    public static final String MY_PROFILE = "/me/profile";
    public static final String PROFILE = "/{userId}/profile";
    public static final String ACTIVITIES = "/activities";

    private final ManageProfileUseCase profiles;

    public ProfileController(ManageProfileUseCase profiles) {
        this.profiles = profiles;
    }

    @GetMapping(MY_PROFILE)
    @Operation(summary = "My profile", description = "Created with the Keycloak name on first access.")
    @ApiResponse(responseCode = "200", description = "Own profile")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public ProfileResponse myProfile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.of(profiles.myProfile(KeycloakJwt.toIdentity(jwt)));
    }

    @PutMapping(MY_PROFILE)
    @Operation(summary = "Edit my profile",
            description = "Replaces the display name, zone and hobbies. The zone is stored approximated.")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content)
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public ProfileResponse updateMyProfile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody UpdateProfileRequest request) {
        var zone = request.zone() == null ? null : request.zone().toDomain();
        var hobbies = request.hobbies().stream().map(HobbyDto::toDomain).toList();
        return ProfileResponse.of(
                profiles.updateMyProfile(KeycloakJwt.toIdentity(jwt), request.displayName(), zone, hobbies));
    }

    @GetMapping(PROFILE)
    @Operation(summary = "Another participant's profile", description = "No coordinates: only the zone name.")
    @ApiResponse(responseCode = "200", description = "Public profile")
    @ApiResponse(responseCode = "404", description = "The user has no profile", content = @Content)
    public PublicProfileResponse profileOf(@PathVariable UUID userId) {
        return PublicProfileResponse.of(profiles.profileOf(userId));
    }

    @GetMapping(ACTIVITIES)
    @Operation(summary = "Catalog of activities and levels")
    public CatalogResponse catalog() {
        return CatalogResponse.create();
    }
}

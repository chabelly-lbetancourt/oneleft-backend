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
@Tag(name = "Perfiles", description = "Nombre visible, zona aproximada y aficiones (HU-002)")
public class ProfileController {

    public static final String MY_PROFILE = "/me/profile";
    public static final String PROFILE = "/{userId}/profile";
    public static final String ACTIVITIES = "/activities";

    private final ManageProfileUseCase profiles;

    public ProfileController(ManageProfileUseCase profiles) {
        this.profiles = profiles;
    }

    @GetMapping(MY_PROFILE)
    @Operation(summary = "Mi perfil", description = "Si es la primera vez, se crea con el nombre de Keycloak.")
    @ApiResponse(responseCode = "200", description = "Perfil propio")
    @ApiResponse(responseCode = "401", description = "Falta el token o no es válido", content = @Content)
    public ProfileResponse myProfile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.of(profiles.myProfile(KeycloakJwt.toIdentity(jwt)));
    }

    @PutMapping(MY_PROFILE)
    @Operation(summary = "Editar mi perfil",
            description = "Sustituye el nombre visible, la zona y las aficiones. La zona se guarda aproximada.")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    @ApiResponse(responseCode = "400", description = "Datos no válidos", content = @Content)
    @ApiResponse(responseCode = "401", description = "Falta el token o no es válido", content = @Content)
    public ProfileResponse updateMyProfile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody UpdateProfileRequest request) {
        var zone = request.zone() == null ? null : request.zone().toDomain();
        var hobbies = request.hobbies().stream().map(HobbyDto::toDomain).toList();
        return ProfileResponse.of(
                profiles.updateMyProfile(KeycloakJwt.toIdentity(jwt), request.displayName(), zone, hobbies));
    }

    @GetMapping(PROFILE)
    @Operation(summary = "Perfil de otro participante", description = "Sin coordenadas: solo el nombre de la zona.")
    @ApiResponse(responseCode = "200", description = "Perfil público")
    @ApiResponse(responseCode = "404", description = "El usuario no tiene perfil", content = @Content)
    public PublicProfileResponse profileOf(@PathVariable UUID userId) {
        return PublicProfileResponse.of(profiles.profileOf(userId));
    }

    @GetMapping(ACTIVITIES)
    @Operation(summary = "Catálogo de actividades y niveles")
    public CatalogResponse catalog() {
        return CatalogResponse.create();
    }
}

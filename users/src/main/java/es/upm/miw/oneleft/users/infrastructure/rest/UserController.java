package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.port.in.GetCurrentUserUseCase;
import es.upm.miw.oneleft.users.infrastructure.config.KeycloakJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UserController.USERS)
@Tag(name = "Usuarios", description = "Usuarios de OneLeft")
public class UserController {

    public static final String USERS = "/api/v1/users";
    public static final String ME = "/me";

    private final GetCurrentUserUseCase getCurrentUser;

    public UserController(GetCurrentUserUseCase getCurrentUser) {
        this.getCurrentUser = getCurrentUser;
    }

    @GetMapping(ME)
    @Operation(summary = "Usuario autenticado",
            description = "Devuelve el usuario de OneLeft correspondiente al token de Keycloak de la petición.")
    @ApiResponse(responseCode = "200", description = "Usuario autenticado")
    @ApiResponse(responseCode = "401", description = "Falta el token o no es válido", content = @Content)
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return UserResponse.of(getCurrentUser.currentUser(KeycloakJwt.toIdentity(jwt)));
    }
}

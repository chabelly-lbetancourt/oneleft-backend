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
@Tag(name = "Users", description = "OneLeft users")
public class UserController {

    public static final String USERS = "/api/v1/users";
    public static final String ME = "/me";

    private final GetCurrentUserUseCase getCurrentUser;

    public UserController(GetCurrentUserUseCase getCurrentUser) {
        this.getCurrentUser = getCurrentUser;
    }

    @GetMapping(ME)
    @Operation(summary = "Authenticated user",
            description = "Returns the OneLeft user matching the Keycloak token of the request.")
    @ApiResponse(responseCode = "200", description = "Authenticated user")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return UserResponse.of(getCurrentUser.currentUser(KeycloakJwt.toIdentity(jwt)));
    }
}

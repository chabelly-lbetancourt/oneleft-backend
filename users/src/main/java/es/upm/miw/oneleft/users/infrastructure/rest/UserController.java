package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.port.in.GetCurrentUserUseCase;
import es.upm.miw.oneleft.users.infrastructure.config.KeycloakJwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UserController.USERS)
public class UserController {

    public static final String USERS = "/api/v1/users";
    public static final String ME = "/me";

    private final GetCurrentUserUseCase getCurrentUser;

    public UserController(GetCurrentUserUseCase getCurrentUser) {
        this.getCurrentUser = getCurrentUser;
    }

    @GetMapping(ME)
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.of(getCurrentUser.currentUser(KeycloakJwt.toIdentity(jwt)));
    }
}

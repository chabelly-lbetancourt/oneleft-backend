package es.upm.miw.oneleft.social.infrastructure.config;

import es.upm.miw.oneleft.social.domain.model.Caller;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;
import java.util.UUID;

/** Adapter between Keycloak JWTs and the OneLeft model. */
public final class KeycloakJwt {

    private KeycloakJwt() {
    }

    /** The Keycloak user id (subject) as the OneLeft user id. */
    public static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    /** Whoever calls, with the name of their account, or the user name without it. */
    public static Caller caller(Jwt jwt) {
        return new Caller(userId(jwt),
                Objects.requireNonNullElse(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username")));
    }
}

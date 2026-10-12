package es.upm.miw.oneleft.chat.infrastructure.config;

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

    /** The name shown next to the messages: the one of the account, or the user name without it. */
    public static String displayName(Jwt jwt) {
        return Objects.requireNonNullElse(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username"));
    }
}

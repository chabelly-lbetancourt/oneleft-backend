package es.upm.miw.oneleft.reputation.infrastructure.config;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Adapter between Keycloak JWTs and the OneLeft model. */
public final class KeycloakJwt {

    private static final String ADMIN = "admin";

    private KeycloakJwt() {
    }

    /** The Keycloak user id (subject) as the OneLeft user id. */
    public static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    /** The name shown of someone, from the token: their name, else their user name. */
    public static String name(Jwt jwt) {
        var name = jwt.getClaimAsString("name");
        return name == null || name.isBlank() ? jwt.getClaimAsString("preferred_username") : name;
    }

    /** Whether the realm role {@code admin} is in the token. */
    public static boolean isAdmin(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        return realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles
                && roles.contains(ADMIN);
    }
}

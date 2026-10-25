package es.upm.miw.oneleft.social.infrastructure.config;

import es.upm.miw.oneleft.social.domain.model.Caller;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adapter between Keycloak JWTs and the OneLeft model: the caller and their realm roles. */
public final class KeycloakJwt {

    private KeycloakJwt() {
    }

    /** The Keycloak user id (subject) as the OneLeft user id. */
    public static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    /** The realm roles of Keycloak ({@code realm_access.roles}). */
    public static Set<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return Set.of();
        }
        return roles.stream().map(String::valueOf).collect(Collectors.toSet());
    }

    /** Turns the realm roles into Spring Security authorities ({@code ROLE_ADMIN}, {@code ROLE_USER}...). */
    public static Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        return jwt -> {
            List<GrantedAuthority> authorities = realmRoles(jwt).stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
                    .toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    /** Whoever calls, with the name of their account, or the user name without it. */
    public static Caller caller(Jwt jwt) {
        return new Caller(userId(jwt),
                Objects.requireNonNullElse(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username")));
    }
}

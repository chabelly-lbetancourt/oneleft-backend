package es.upm.miw.oneleft.users.infrastructure.config;

import es.upm.miw.oneleft.users.domain.model.Identity;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptador entre los tokens JWT de Keycloak y el modelo de OneLeft.
 * Keycloak publica los roles del realm en el claim {@code realm_access.roles}.
 */
public final class KeycloakJwt {

    private KeycloakJwt() {
    }

    public static Set<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return Set.of();
        }
        return roles.stream().map(String::valueOf).collect(Collectors.toSet());
    }

    public static Identity toIdentity(Jwt jwt) {
        return new Identity(jwt.getSubject(), jwt.getClaimAsString("name"), jwt.getClaimAsString("email"),
                realmRoles(jwt));
    }

    /**
     * Convierte los roles del realm en autoridades de Spring Security ({@code ROLE_ADMIN}, {@code ROLE_USER}...).
     */
    public static Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        return jwt -> {
            List<GrantedAuthority> authorities = realmRoles(jwt).stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
                    .toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }
}

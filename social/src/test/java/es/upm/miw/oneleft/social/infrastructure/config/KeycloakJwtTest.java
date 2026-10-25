package es.upm.miw.oneleft.social.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The realm roles of Keycloak become authorities: the open reports are only for admins (HU-033). */
class KeycloakJwtTest {

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString());
    }

    @Test
    void theRealmRolesBecomeAuthorities() {
        var jwt = token().claim("realm_access", Map.of("roles", List.of("user", "admin"))).build();

        var authentication = KeycloakJwt.authenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(authentication.getName()).isEqualTo(jwt.getSubject());
    }

    @Test
    void withoutRolesThereAreNoAuthorities() {
        assertThat(KeycloakJwt.realmRoles(token().claim("name", "Lucía").build())).isEmpty();
        assertThat(KeycloakJwt.realmRoles(token().claim("realm_access", Map.of()).build())).isEmpty();
    }
}

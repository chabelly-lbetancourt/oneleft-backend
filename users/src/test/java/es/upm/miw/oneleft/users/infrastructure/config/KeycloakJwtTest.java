package es.upm.miw.oneleft.users.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtTest {

    private static Jwt jwt(Map<String, Object> claims) {
        var builder = Jwt.withTokenValue("token").header("alg", "RS256").subject("0b6f1c2e-0000-4000-8000-000000000001")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void readsRealmRoles() {
        var token = jwt(Map.of("realm_access", Map.of("roles", List.of("user", "admin"))));
        assertThat(KeycloakJwt.realmRoles(token)).containsExactlyInAnyOrder("user", "admin");
    }

    @Test
    void tokenWithoutRealmAccessHasNoRoles() {
        assertThat(KeycloakJwt.realmRoles(jwt(Map.of("email", "ana@oneleft.dev")))).isEmpty();
        assertThat(KeycloakJwt.realmRoles(jwt(Map.of("realm_access", Map.of("otro", "valor"))))).isEmpty();
    }

    @Test
    void convertsRealmRolesIntoAuthorities() {
        var token = jwt(Map.of("realm_access", Map.of("roles", List.of("user", "admin"))));
        var authentication = KeycloakJwt.authenticationConverter().convert(token);
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(authentication.getName()).isEqualTo(token.getSubject());
    }

    @Test
    void buildsIdentityFromClaims() {
        var token = jwt(Map.of("name", "Ana Pruebas", "email", "ana@oneleft.dev",
                "realm_access", Map.of("roles", List.of("user"))));
        var identity = KeycloakJwt.toIdentity(token);
        assertThat(identity.subject()).isEqualTo(token.getSubject());
        assertThat(identity.name()).isEqualTo("Ana Pruebas");
        assertThat(identity.email()).isEqualTo("ana@oneleft.dev");
        assertThat(identity.roles()).containsExactly("user");
    }
}

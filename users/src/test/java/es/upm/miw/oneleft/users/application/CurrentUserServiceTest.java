package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserServiceTest {

    private final CurrentUserService service = new CurrentUserService();
    private final String subject = UUID.randomUUID().toString();

    @Test
    void mapsIdentityToUserKeepingOnlyKnownRoles() {
        var identity = new Identity(subject, "Admin Pruebas", "admin@oneleft.dev",
                Set.of("admin", "user", "offline_access", "uma_authorization"));

        var user = service.currentUser(identity);

        assertThat(user.id()).isEqualTo(UUID.fromString(subject));
        assertThat(user.name()).isEqualTo("Admin Pruebas");
        assertThat(user.email()).isEqualTo("admin@oneleft.dev");
        assertThat(user.roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.USER);
    }

    @Test
    void usesEmailWhenNameIsMissing() {
        var user = service.currentUser(new Identity(subject, null, "ana@oneleft.dev", Set.of()));
        assertThat(user.name()).isEqualTo("ana@oneleft.dev");
        assertThat(user.roles()).containsExactly(Role.USER);
    }

    @Test
    void usesEmailWhenNameIsBlank() {
        var user = service.currentUser(new Identity(subject, "  ", "ana@oneleft.dev", Set.of()));
        assertThat(user.name()).isEqualTo("ana@oneleft.dev");
    }
}

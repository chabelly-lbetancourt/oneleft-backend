package es.upm.miw.oneleft.users.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final UUID ID = UUID.randomUUID();

    @Test
    void userWithoutRolesIsARegularUser() {
        var user = new User(ID, "Ana", "ana@oneleft.dev", Set.of());
        assertThat(user.roles()).containsExactly(Role.USER);
        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    void userWithNullRolesIsARegularUser() {
        assertThat(new User(ID, "Ana", "ana@oneleft.dev", null).roles()).containsExactly(Role.USER);
    }

    @Test
    void adminIsDetected() {
        var user = new User(ID, "Admin", "admin@oneleft.dev", Set.of(Role.USER, Role.ADMIN));
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void idIsMandatory() {
        assertThatThrownBy(() -> new User(null, "Ana", "ana@oneleft.dev", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emailMustBeValid() {
        assertThatThrownBy(() -> new User(ID, "Ana", "no-at-sign", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User(ID, "Ana", null, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

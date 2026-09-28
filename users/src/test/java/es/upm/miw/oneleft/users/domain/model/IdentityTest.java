package es.upm.miw.oneleft.users.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityTest {

    @Test
    void subjectIsMandatory() {
        assertThatThrownBy(() -> new Identity(" ", "Ana", "ana@oneleft.dev", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Identity(null, "Ana", "ana@oneleft.dev", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullRolesBecomeEmpty() {
        assertThat(new Identity("sub", "Ana", "ana@oneleft.dev", null).roles()).isEmpty();
    }
}

package es.upm.miw.oneleft.users.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void knownRolesAreTranslatedIgnoringCase() {
        assertThat(Role.fromName("admin")).contains(Role.ADMIN);
        assertThat(Role.fromName(" USER ")).contains(Role.USER);
    }

    @Test
    void unknownOrNullRolesAreIgnored() {
        assertThat(Role.fromName("offline_access")).isEmpty();
        assertThat(Role.fromName(null)).isEmpty();
    }
}

package es.upm.miw.oneleft.users.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Each broken rule has a stable code that clients translate. */
class ValidationCodesTest {

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ValidationException.class,
                exception -> org.assertj.core.api.Assertions.assertThat(exception.code()).isEqualTo(code));
    }

    @Test
    void zoneRules() {
        assertCode(() -> new ApproximateZone(" ", 40, -3), "zone.name");
        assertCode(() -> new ApproximateZone("Pole", 95, 0), "coordinates.outOfRange");
    }

    @Test
    void profileRules() {
        var id = UUID.randomUUID();
        assertCode(() -> new Profile(null, "Ana", null, List.of()), "profile.userRequired");
        assertCode(() -> new Profile(id, " ", null, List.of()), "profile.displayName");
        assertCode(() -> new Profile(id, "Ana", null, List.of(new Hobby(Activity.PADEL, Level.BEGINNER),
                new Hobby(Activity.PADEL, Level.ADVANCED))), "profile.duplicateActivity");
        var many = java.util.Arrays.stream(Activity.values()).map(a -> new Hobby(a, Level.BEGINNER)).toList();
        var eleven = new java.util.ArrayList<>(many);
        eleven.add(new Hobby(Activity.PADEL, Level.BEGINNER));
        assertCode(() -> new Profile(id, "Ana", null, eleven), "profile.tooManyHobbies");
        assertCode(() -> new Hobby(null, Level.BEGINNER), "hobby.incomplete");
    }

    @Test
    void identityAndUserRules() {
        assertCode(() -> new Identity(" ", "Ana", "ana@oneleft.dev", null), "identity.subjectRequired");
        assertCode(() -> new User(null, "Ana", "ana@oneleft.dev", null), "user.idRequired");
        assertCode(() -> new User(UUID.randomUUID(), "Ana", "no-at-sign", null), "user.invalidEmail");
    }
}

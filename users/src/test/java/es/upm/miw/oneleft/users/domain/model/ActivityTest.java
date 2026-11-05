package es.upm.miw.oneleft.users.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-068: a profile admits the new activities as hobbies; «other» is not a hobby. */
class ActivityTest {

    @Test
    void theCatalogHasTheNewActivitiesAndNoOther() {
        assertThat(Arrays.stream(Activity.values()).map(Activity::name))
                .contains("PADEL", "VOLLEYBALL", "YOGA", "THEATER", "ESCAPE_ROOM", "COFFEE")
                .doesNotContain("OTHER").hasSize(28).allMatch(code -> code.length() <= 20);
    }

    @Test
    void aProfileCanHaveANewActivityAsAHobby() {
        var profile = new Profile(UUID.randomUUID(), "Lucía", null,
                List.of(new Hobby(Activity.YOGA, Level.BEGINNER), new Hobby(Activity.TAPAS, Level.INTERMEDIATE)));

        assertThat(profile.hobbies()).extracting(Hobby::activity).containsExactly(Activity.YOGA, Activity.TAPAS);
    }
}

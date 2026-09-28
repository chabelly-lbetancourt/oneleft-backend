package es.upm.miw.oneleft.users.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileTest {

    private static final UUID ID = UUID.randomUUID();

    @Test
    void initialProfileUsesTheUserName() {
        var profile = Profile.initial(new User(ID, "Ana Test", "ana@oneleft.dev", Set.of()));
        assertThat(profile.userId()).isEqualTo(ID);
        assertThat(profile.displayName()).isEqualTo("Ana Test");
        assertThat(profile.zone()).isNull();
        assertThat(profile.hobbies()).isEmpty();
    }

    @Test
    void updateReplacesNameZoneAndHobbies() {
        var zone = new ApproximateZone("Vallecas", 40.39, -3.62);
        var updated = new Profile(ID, "Ana", null, null)
                .update("  Anita ", zone, List.of(new Hobby(Activity.PADEL, Level.INTERMEDIATE)));
        assertThat(updated.displayName()).isEqualTo("Anita");
        assertThat(updated.zone()).isEqualTo(zone);
        assertThat(updated.hobbies()).containsExactly(new Hobby(Activity.PADEL, Level.INTERMEDIATE));
        assertThat(updated.update("Ana", null, null).hobbies()).isEmpty();
    }

    @Test
    void displayNameIsMandatoryAndLimited() {
        assertThatThrownBy(() -> new Profile(ID, " ", null, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Profile(ID, null, null, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Profile(ID, "x".repeat(51), null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userIsMandatory() {
        assertThatThrownBy(() -> new Profile(null, "Ana", null, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activitiesCannotBeRepeated() {
        var hobbies = List.of(new Hobby(Activity.PADEL, Level.BEGINNER), new Hobby(Activity.PADEL, Level.ADVANCED));
        assertThatThrownBy(() -> new Profile(ID, "Ana", null, hobbies)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atMostTenHobbies() {
        var all = Arrays.stream(Activity.values()).map(a -> new Hobby(a, Level.INTERMEDIATE)).toList();
        assertThat(new Profile(ID, "Ana", null, all).hobbies()).hasSize(Profile.MAX_HOBBIES);
        var eleven = new java.util.ArrayList<>(all);
        eleven.add(new Hobby(Activity.PADEL, Level.ADVANCED));
        assertThatThrownBy(() -> new Profile(ID, "Ana", null, eleven)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hobbyNeedsActivityAndLevel() {
        assertThatThrownBy(() -> new Hobby(null, Level.ADVANCED)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Hobby(Activity.CINEMA, null)).isInstanceOf(IllegalArgumentException.class);
    }
}

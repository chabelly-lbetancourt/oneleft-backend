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
        var profile = Profile.initial(new User(ID, "Ana Pruebas", "ana@oneleft.dev", Set.of()));
        assertThat(profile.userId()).isEqualTo(ID);
        assertThat(profile.displayName()).isEqualTo("Ana Pruebas");
        assertThat(profile.zone()).isNull();
        assertThat(profile.hobbies()).isEmpty();
    }

    @Test
    void updateReplacesNameZoneAndHobbies() {
        var zone = new ApproximateZone("Vallecas", 40.39, -3.62);
        var updated = new Profile(ID, "Ana", null, null)
                .update("  Anita ", zone, List.of(new Hobby(Activity.PADEL, Level.INTERMEDIO)));
        assertThat(updated.displayName()).isEqualTo("Anita");
        assertThat(updated.zone()).isEqualTo(zone);
        assertThat(updated.hobbies()).containsExactly(new Hobby(Activity.PADEL, Level.INTERMEDIO));
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
        var hobbies = List.of(new Hobby(Activity.PADEL, Level.PRINCIPIANTE), new Hobby(Activity.PADEL, Level.AVANZADO));
        assertThatThrownBy(() -> new Profile(ID, "Ana", null, hobbies)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atMostTenHobbies() {
        var all = Arrays.stream(Activity.values()).map(a -> new Hobby(a, Level.INTERMEDIO)).toList();
        assertThat(new Profile(ID, "Ana", null, all).hobbies()).hasSize(Profile.MAX_HOBBIES);
        var eleven = new java.util.ArrayList<>(all);
        eleven.add(new Hobby(Activity.PADEL, Level.AVANZADO));
        assertThatThrownBy(() -> new Profile(ID, "Ana", null, eleven)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hobbyNeedsActivityAndLevel() {
        assertThatThrownBy(() -> new Hobby(null, Level.AVANZADO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Hobby(Activity.CINE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Activity.JUEGOS_DE_MESA.displayName()).isEqualTo("Juegos de mesa");
    }
}

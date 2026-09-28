package es.upm.miw.oneleft.users.infrastructure.seed;

import es.upm.miw.oneleft.users.domain.model.Activity;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.Profile;
import es.upm.miw.oneleft.users.domain.port.in.ManageProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seed runs on startup with the {@code seed} profile, against a real PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("seed")
class DemoProfilesSeederTest {

    private static final UUID ANA = UUID.fromString("a624d063-bd1e-442d-b52d-8de2df356c13");
    private static final UUID ADMIN = UUID.fromString("9e51a057-ce42-4177-b020-ac2fc7ec6ffa");

    @Autowired
    private DemoProfilesSeeder seeder;

    @Autowired
    private ManageProfileUseCase profiles;

    @Test
    void createsTheProfilesOfTheTestUsers() {
        Profile ana = profiles.profileOf(ANA);
        assertThat(ana.displayName()).isEqualTo("Ana");
        assertThat(ana.zone().name()).isEqualTo("Vallecas");
        assertThat(ana.hobbies()).extracting(hobby -> hobby.activity())
                .containsExactly(Activity.PADEL, Activity.RUNNING, Activity.BOARD_GAMES);
        assertThat(profiles.profileOf(ADMIN).hobbies()).hasSize(2);
    }

    @Test
    void keepsTheChangesMadeInTheApp() {
        var identity = new Identity(ADMIN.toString(), "Admin Pruebas", "admin@oneleft.dev", Set.of("admin"));
        profiles.updateMyProfile(identity, "Admin del club", null, List.of());

        seeder.run(new DefaultApplicationArguments());

        assertThat(profiles.profileOf(ADMIN).displayName()).isEqualTo("Admin del club");
    }

    @Test
    void theSeedIsNeverActiveInProduction() {
        var expression = Profiles.of(DemoProfilesSeeder.PROFILE);
        assertThat(expression.matches(Set.of("seed")::contains)).isTrue();
        assertThat(expression.matches(Set.of("seed", "pro")::contains)).isFalse();
    }
}

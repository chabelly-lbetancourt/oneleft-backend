package es.upm.miw.oneleft.plans.infrastructure.seed;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.port.in.QueryPlansUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seed runs on startup with the {@code seed} profile, against a real PostGIS and RabbitMQ.
 */
@SpringBootTest
@ActiveProfiles("seed")
@Import(TestcontainersConfiguration.class)
class DemoPlansSeederTest {

    @Autowired
    private DemoPlansSeeder seeder;

    @Autowired
    private QueryPlansUseCase queryPlans;

    @Test
    void publishesTheDemoPlansOnStartupOnlyOnce() {
        assertThat(queryPlans.upcomingPlansOrganizedBy(DemoPlansSeeder.LUCIA.id())).hasSize(2);
        assertThat(queryPlans.upcomingPlansOrganizedBy(DemoPlansSeeder.ANA.id()))
                .singleElement().satisfies(plan -> assertThat(plan.title()).isEqualTo("Rodaje suave por el parque"));

        seeder.run(new DefaultApplicationArguments());

        assertThat(queryPlans.upcomingPlansOrganizedBy(DemoPlansSeeder.LUCIA.id())).hasSize(2);
    }

    @Test
    void theDemoPlansAreFoundAroundVallecas() {
        var search = new NearbySearch(40.391, -3.629, 5_000, null, null, UUID.randomUUID());

        var nearby = queryPlans.nearbyPlans(search);

        // Cinema and concert are more than 5 km away, in the city centre
        assertThat(nearby).hasSize(6);
        assertThat(nearby.getFirst().plan().title()).isEqualTo("Partido de pádel, falta uno");
    }

    @Test
    void theSeedIsNeverActiveInProduction() {
        var profiles = Profiles.of(DemoPlansSeeder.PROFILE);
        assertThat(profiles.matches(Set.of("seed")::contains)).isTrue();
        assertThat(profiles.matches(Set.of("seed", "pro")::contains)).isFalse();
        assertThat(profiles.matches(Set.of()::contains)).isFalse();
    }
}

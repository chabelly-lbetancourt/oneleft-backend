package es.upm.miw.oneleft.users.infrastructure.persistence;

import es.upm.miw.oneleft.users.domain.model.Activity;
import es.upm.miw.oneleft.users.domain.model.ApproximateZone;
import es.upm.miw.oneleft.users.domain.model.Hobby;
import es.upm.miw.oneleft.users.domain.model.Level;
import es.upm.miw.oneleft.users.domain.model.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración del adaptador con PostgreSQL real (Testcontainers) y el esquema de Flyway.
 */
@SpringBootTest
class JpaProfileRepositoryTest {

    @Autowired
    private JpaProfileRepository repository;

    @Test
    void savesAndLoadsAProfileWithZoneAndHobbies() {
        var id = UUID.randomUUID();
        var profile = new Profile(id, "Ana", new ApproximateZone("Vallecas", 40.3912, -3.6287),
                List.of(new Hobby(Activity.PADEL, Level.INTERMEDIO), new Hobby(Activity.RUNNING, Level.AVANZADO)));

        repository.save(profile);
        var loaded = repository.findById(id).orElseThrow();

        assertThat(loaded.displayName()).isEqualTo("Ana");
        assertThat(loaded.zone()).isEqualTo(new ApproximateZone("Vallecas", 40.39, -3.63));
        assertThat(loaded.hobbies()).containsExactlyInAnyOrderElementsOf(profile.hobbies());
    }

    @Test
    void updatingReplacesHobbiesAndCanRemoveTheZone() {
        var id = UUID.randomUUID();
        repository.save(new Profile(id, "Ana", new ApproximateZone("Vallecas", 40.39, -3.62),
                List.of(new Hobby(Activity.PADEL, Level.INTERMEDIO))));

        repository.save(new Profile(id, "Ana", null, List.of(new Hobby(Activity.CINE, Level.PRINCIPIANTE))));
        var loaded = repository.findById(id).orElseThrow();

        assertThat(loaded.zone()).isNull();
        assertThat(loaded.hobbies()).containsExactly(new Hobby(Activity.CINE, Level.PRINCIPIANTE));
    }

    @Test
    void unknownProfileIsEmpty() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }
}

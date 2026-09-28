package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.Activity;
import es.upm.miw.oneleft.users.domain.model.ApproximateZone;
import es.upm.miw.oneleft.users.domain.model.Hobby;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.Level;
import es.upm.miw.oneleft.users.domain.model.Profile;
import es.upm.miw.oneleft.users.domain.model.ProfileNotFoundException;
import es.upm.miw.oneleft.users.domain.port.out.ProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileServiceTest {

    /** Repositorio en memoria: el servicio se prueba sin base de datos gracias al puerto de salida. */
    static class InMemoryProfiles implements ProfileRepository {
        final Map<UUID, Profile> data = new HashMap<>();

        @Override
        public Optional<Profile> findById(UUID userId) {
            return Optional.ofNullable(data.get(userId));
        }

        @Override
        public Profile save(Profile profile) {
            data.put(profile.userId(), profile);
            return profile;
        }
    }

    private final InMemoryProfiles repository = new InMemoryProfiles();
    private final ProfileService service = new ProfileService(new CurrentUserService(), repository);
    private final String subject = UUID.randomUUID().toString();
    private final Identity ana = new Identity(subject, "Ana Pruebas", "ana@oneleft.dev", Set.of("user"));

    @Test
    void firstAccessCreatesTheInitialProfile() {
        var profile = service.myProfile(ana);
        assertThat(profile.displayName()).isEqualTo("Ana Pruebas");
        assertThat(repository.data).containsKey(UUID.fromString(subject));
    }

    @Test
    void laterAccessesReturnTheStoredProfile() {
        service.updateMyProfile(ana, "Anita", null, List.of());
        assertThat(service.myProfile(ana).displayName()).isEqualTo("Anita");
    }

    @Test
    void updatesAreStored() {
        var zone = new ApproximateZone("Vallecas", 40.3912, -3.6287);
        var hobbies = List.of(new Hobby(Activity.PADEL, Level.INTERMEDIO), new Hobby(Activity.CINE, Level.PRINCIPIANTE));

        var updated = service.updateMyProfile(ana, "Ana", zone, hobbies);

        assertThat(updated.zone().latitude()).isEqualTo(40.39);
        assertThat(service.profileOf(UUID.fromString(subject)).hobbies()).hasSize(2);
    }

    @Test
    void unknownProfileIsNotFound() {
        assertThatThrownBy(() -> service.profileOf(UUID.randomUUID())).isInstanceOf(ProfileNotFoundException.class);
    }
}

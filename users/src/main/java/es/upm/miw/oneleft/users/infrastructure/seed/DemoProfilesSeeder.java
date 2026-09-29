package es.upm.miw.oneleft.users.infrastructure.seed;

import es.upm.miw.oneleft.users.domain.model.Activity;
import es.upm.miw.oneleft.users.domain.model.ApproximateZone;
import es.upm.miw.oneleft.users.domain.model.Hobby;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.Level;
import es.upm.miw.oneleft.users.domain.model.ProfileNotFoundException;
import es.upm.miw.oneleft.users.domain.port.in.ManageProfileUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Profiles of the test users of the development realm (fixed ids in oneleft-realm.json) for the dev and pre
 * environments. A profile that already exists is left untouched, so changes made in the app survive restarts.
 */
@Component
@Profile(DemoProfilesSeeder.PROFILE)
public class DemoProfilesSeeder implements ApplicationRunner {

    /** Explicit opt-in, and never together with production. */
    public static final String PROFILE = "seed & !pro";

    private static final Logger log = LoggerFactory.getLogger(DemoProfilesSeeder.class);

    private record DemoProfile(Identity identity, String displayName, ApproximateZone zone, List<Hobby> hobbies) {
    }

    static final List<DemoProfile> PROFILES = List.of(
            new DemoProfile(new Identity("a624d063-bd1e-442d-b52d-8de2df356c13", "Ana Pruebas", "ana@oneleft.dev",
                    Set.of("user")), "Ana", new ApproximateZone("Vallecas", 40.39, -3.63),
                    List.of(new Hobby(Activity.PADEL, Level.INTERMEDIATE), new Hobby(Activity.RUNNING, Level.BEGINNER),
                            new Hobby(Activity.BOARD_GAMES, Level.ADVANCED))),
            new DemoProfile(new Identity("9e51a057-ce42-4177-b020-ac2fc7ec6ffa", "Admin Pruebas", "admin@oneleft.dev",
                    Set.of("user", "admin")), "Admin", new ApproximateZone("Moratalaz", 40.41, -3.64),
                    List.of(new Hobby(Activity.TENNIS, Level.ADVANCED), new Hobby(Activity.CYCLING, Level.INTERMEDIATE))));

    private final ManageProfileUseCase profiles;

    public DemoProfilesSeeder(ManageProfileUseCase profiles) {
        this.profiles = profiles;
    }

    @Override
    public void run(ApplicationArguments args) {
        var created = PROFILES.stream().filter(this::createIfMissing).count();
        log.info("Seed: {} demo profiles created, {} already existed", created, PROFILES.size() - created);
    }

    private boolean createIfMissing(DemoProfile demo) {
        try {
            profiles.profileOf(UUID.fromString(demo.identity().subject()));
            return false;
        } catch (ProfileNotFoundException missing) {
            profiles.updateMyProfile(demo.identity(), demo.displayName(), demo.zone(), demo.hobbies());
            return true;
        }
    }
}

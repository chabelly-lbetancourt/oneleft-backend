package es.upm.miw.oneleft.notifications.infrastructure.seed;

import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.port.in.ManagePreferencesUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.PreferencesRepository;
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
 * Demo preferences for the dev and pre environments: Lucía and Admin, test users of the realm, want to hear about any
 * plan around Vallecas at any time, so a plan Ana publishes there in a demo (or an end-to-end test) reaches them.
 * Existing preferences are never overwritten.
 */
@Component
@Profile(DemoPreferencesSeeder.PROFILE)
public class DemoPreferencesSeeder implements ApplicationRunner {

    /** Explicit opt-in, and never together with production. */
    public static final String PROFILE = "seed & !pro";

    /** Test users of the development realm who sign in (fixed ids in oneleft-realm.json). */
    public static final UUID LUCIA = UUID.fromString("5b0f7c2e-8d4a-4f3b-9c61-2e7a9d4b8f10");
    public static final UUID ADMIN = UUID.fromString("9e51a057-ce42-4177-b020-ac2fc7ec6ffa");
    static final double VALLECAS_LATITUDE = 40.39;
    static final double VALLECAS_LONGITUDE = -3.63;

    private static final Logger log = LoggerFactory.getLogger(DemoPreferencesSeeder.class);

    private final PreferencesRepository repository;
    private final ManagePreferencesUseCase preferences;

    public DemoPreferencesSeeder(PreferencesRepository repository, ManagePreferencesUseCase preferences) {
        this.repository = repository;
        this.preferences = preferences;
    }

    @Override
    public void run(ApplicationArguments args) {
        var created = 0;
        for (var userId : List.of(LUCIA, ADMIN)) {
            if (repository.findByUser(userId).isEmpty()) {
                preferences.update(new NotificationPreferences(userId, true, VALLECAS_LATITUDE, VALLECAS_LONGITUDE,
                        5_000, Set.of(), null, NotificationPreferences.MAX_PER_DAY));
                created++;
            }
        }
        log.info("Seed: {} demo notification preferences created", created);
    }
}

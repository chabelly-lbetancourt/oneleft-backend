package es.upm.miw.oneleft.notifications.infrastructure.seed;

import es.upm.miw.oneleft.notifications.TestcontainersConfiguration;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.port.in.ManagePreferencesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seed runs on startup with the {@code seed} profile: Lucía and Diego hear about plans around Vallecas.
 */
@SpringBootTest
@ActiveProfiles("seed")
@Import(TestcontainersConfiguration.class)
class DemoPreferencesSeederTest {

    @Autowired
    private ManagePreferencesUseCase preferences;
    @Autowired
    private DemoPreferencesSeeder seeder;

    @Test
    void luciaAndDiegoHearAboutPlansAroundVallecas() {
        var lucia = preferences.preferencesOf(DemoPreferencesSeeder.LUCIA);
        assertThat(lucia.enabled()).isTrue();
        assertThat(lucia.latitude()).isEqualTo(40.39);
        assertThat(lucia.quietHours()).isNull();
        assertThat(preferences.preferencesOf(DemoPreferencesSeeder.DIEGO).latitude()).isEqualTo(40.39);
    }

    @Test
    void neverOverwritesWhatThePersonChose() {
        preferences.update(new NotificationPreferences(DemoPreferencesSeeder.DIEGO, false, 40.39, -3.63, 1_000,
                Set.of(), null, 3));

        seeder.run(new DefaultApplicationArguments());

        assertThat(preferences.preferencesOf(DemoPreferencesSeeder.DIEGO).enabled()).isFalse();
    }
}

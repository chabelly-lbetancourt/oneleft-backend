package es.upm.miw.oneleft.notifications.infrastructure;

import es.upm.miw.oneleft.notifications.TestcontainersConfiguration;
import es.upm.miw.oneleft.notifications.domain.port.in.PersonalDataUseCase;
import es.upm.miw.oneleft.notifications.infrastructure.messaging.AccountDeletedListener;
import es.upm.miw.oneleft.notifications.infrastructure.rest.MyDataController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-045 in this service with a real PostgreSQL: what a person can download, and what the deletion of their account
 * removes or makes anonymous, reached the way production reaches it (the event of the users service). What the test
 * creates is removed at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PersonalDataIntegrationTest {

    private record Check(String sql, String expected, String meaning) {
    }

    private static final String[] SEED = {
            "INSERT INTO notification_preferences (user_id, enabled, latitude, longitude, radius_meters, max_per_day, updated_at) VALUES ('$user', true, 40.39, -3.63, 3000, 5, now())",
            "INSERT INTO notification_preference_activity (user_id, activity) VALUES ('$user', 'PADEL')",
            "INSERT INTO push_subscription (endpoint, user_id, p256dh, auth, language, created_at) VALUES ('https://push.example/$user', '$user', 'key', 'auth', 'es', now())",
            "INSERT INTO sent_notice (user_id, plan_id, sent_at) VALUES ('$user', '$a', now())",
            "INSERT INTO saved_alert (id, user_id, name, latitude, longitude, radius_meters, created_at) VALUES ('$b', '$user', 'Padel', 40.39, -3.63, 2000, now())",
            "INSERT INTO saved_alert_activity (alert_id, activity) VALUES ('$b', 'PADEL')",
            "INSERT INTO block (blocker_id, blocked_id, blocked_at) VALUES ('$user', '$other', now())"};
    private static final Check[] AFTER = {
            new Check("SELECT count(*) FROM notification_preferences WHERE user_id = '$user'", "0", "her preferences"),
            new Check("SELECT count(*) FROM notification_preference_activity WHERE user_id = '$user'", "0", "with their activities"),
            new Check("SELECT count(*) FROM push_subscription WHERE user_id = '$user'", "0", "her subscriptions"),
            new Check("SELECT count(*) FROM sent_notice WHERE user_id = '$user'", "0", "her sent notices"),
            new Check("SELECT count(*) FROM saved_alert WHERE user_id = '$user'", "0", "her saved alerts"),
            new Check("SELECT count(*) FROM saved_alert_activity WHERE alert_id = '$b'", "0", "with their activities"),
            new Check("SELECT count(*) FROM block WHERE '$user' IN (blocker_id, blocked_id)", "0", "and her blocks")};
    private static final String[] CLEANUP = {
            };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AccountDeletedListener listener;

    @Autowired
    private PersonalDataUseCase personalData;

    private final UUID user = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID planA = UUID.randomUUID();
    private final UUID planB = UUID.randomUUID();
    private final UUID planC = UUID.randomUUID();

    private String sql(String template) {
        return template.replace("$user", user.toString()).replace("$other", other.toString())
                .replace("$a", planA.toString()).replace("$b", planB.toString()).replace("$c", planC.toString());
    }

    @BeforeEach
    void seed() {
        for (var statement : SEED) {
            jdbc.execute(sql(statement));
        }
    }

    /** Erasing is repeatable, so it also cleans what the test left if it failed half way. */
    @AfterEach
    void clean() {
        personalData.erase(user);
        personalData.erase(other);
        for (var statement : CLEANUP) {
            jdbc.execute(sql(statement));
        }
    }

    @Test
    void thePersonDownloadsOnlyWhatIsTheirs() throws Exception {
        mockMvc.perform(get(MyDataController.MY_DATA).with(jwt().jwt(token -> token.subject(user.toString()))))
                .andExpect(status().isOk())
        .andExpect(jsonPath("$.preferences", hasSize(1)))
        .andExpect(jsonPath("$.preferenceActivities", hasSize(1)))
        .andExpect(jsonPath("$.pushSubscriptions", hasSize(1)))
        .andExpect(jsonPath("$.sentNotices", hasSize(1)))
        .andExpect(jsonPath("$.savedAlerts", hasSize(1)));
    }

    @Test
    void whenTheAccountIsDeletedTheServiceErasesItsPartAndRepeatingItChangesNothing() {
        listener.onDeleted(new AccountDeletedListener.AccountDeleted(user, Instant.now()));
        // The event can arrive twice
        listener.onDeleted(new AccountDeletedListener.AccountDeleted(user, Instant.now()));

        for (var check : AFTER) {
            var found = jdbc.queryForObject(sql(check.sql()), Object.class);
            assertThat(String.valueOf(found)).as(check.meaning()).isEqualTo(check.expected());
        }
    }
}

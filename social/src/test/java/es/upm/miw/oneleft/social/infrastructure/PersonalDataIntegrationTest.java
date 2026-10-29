package es.upm.miw.oneleft.social.infrastructure;

import es.upm.miw.oneleft.social.TestcontainersConfiguration;
import es.upm.miw.oneleft.social.domain.port.in.PersonalDataUseCase;
import es.upm.miw.oneleft.social.infrastructure.messaging.AccountDeletedListener;
import es.upm.miw.oneleft.social.infrastructure.rest.MyDataController;
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
            "INSERT INTO person (user_id, name, visibility) VALUES ('$user', 'Lucía', 'PUBLIC'), ('$other', 'Ana', 'PUBLIC')",
            "INSERT INTO follow (follower_id, followee_id, status, requested_at, accepted_at) VALUES ('$other', '$user', 'ACCEPTED', now(), now())",
            "INSERT INTO hidden_plan (user_id, plan_id, hidden_at) VALUES ('$user', '$a', now())",
            "INSERT INTO activity (actor_id, type, plan_id, plan_title, plan_activity, starts_at, occurred_at) VALUES ('$user', 'JOINED', '$a', 'Test', 'PADEL', now(), now())",
            "INSERT INTO report (id, reporter_id, target_type, target_id, reported_user_id, reason, status, reported_at) VALUES ('$b', '$user', 'PERSON', '$other', '$other', 'SPAM', 'OPEN', now())"};
    private static final Check[] AFTER = {
            new Check("SELECT count(*) FROM person WHERE user_id = '$user'", "0", "her profile is gone"),
            new Check("SELECT count(*) FROM follow WHERE '$user' IN (follower_id, followee_id)", "0", "with her follows"),
            new Check("SELECT count(*) FROM hidden_plan WHERE user_id = '$user'", "0", "her hidden plans"),
            new Check("SELECT count(*) FROM activity WHERE actor_id = '$user'", "0", "her activity"),
            new Check("SELECT count(*) FROM report WHERE reporter_id = '$user'", "0", "and her reports"),
            new Check("SELECT count(*) FROM person WHERE user_id = '$other'", "1", "what is about someone else stays")};
    private static final String[] CLEANUP = {
            "DELETE FROM person WHERE user_id IN ('$user', '$other')",
            "DELETE FROM activity WHERE actor_id = '$user'",
            "DELETE FROM hidden_plan WHERE user_id = '$user'"};

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
        .andExpect(jsonPath("$.profile", hasSize(1)))
        .andExpect(jsonPath("$.follows", hasSize(1)))
        .andExpect(jsonPath("$.hiddenPlans", hasSize(1)))
        .andExpect(jsonPath("$.activity", hasSize(1)))
        .andExpect(jsonPath("$.reports", hasSize(1)));
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

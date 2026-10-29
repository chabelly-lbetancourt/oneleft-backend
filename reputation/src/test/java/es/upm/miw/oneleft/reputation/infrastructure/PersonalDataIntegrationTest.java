package es.upm.miw.oneleft.reputation.infrastructure;

import es.upm.miw.oneleft.reputation.TestcontainersConfiguration;
import es.upm.miw.oneleft.reputation.domain.port.in.PersonalDataUseCase;
import es.upm.miw.oneleft.reputation.infrastructure.messaging.AccountDeletedListener;
import es.upm.miw.oneleft.reputation.infrastructure.rest.MyDataController;
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
            "INSERT INTO reputation_entry (user_id, reason, points, source_id, plan_title, occurred_at) VALUES ('$user', 'ATTENDED', 10, '$a', 'Test', now())",
            "INSERT INTO reputation_score (user_id, score) VALUES ('$user', 10)",
            "INSERT INTO track_record (user_id, plan_id, activity, organizer, arrived, held, closed_at) VALUES ('$user', '$a', 'PADEL', false, true, true, now())",
            "INSERT INTO kudos (giver_id, receiver_id, plan_id, kind, given_at) VALUES ('$user', '$other', '$a', 'PUNCTUAL', now()), ('$other', '$user', '$b', 'GOOD_VIBES', now())"};
    private static final Check[] AFTER = {
            new Check("SELECT count(*) FROM reputation_entry WHERE user_id = '$user'", "0", "her ledger, although it is append-only"),
            new Check("SELECT count(*) FROM reputation_score WHERE user_id = '$user'", "0", "her score"),
            new Check("SELECT count(*) FROM track_record WHERE user_id = '$user'", "0", "her track record"),
            new Check("SELECT count(*) FROM kudos WHERE receiver_id = '$user'", "0", "the kudos she received"),
            new Check("SELECT count(*) FROM kudos WHERE giver_id = '$user'", "0", "and none says she gave one"),
            new Check("SELECT count(*) FROM kudos WHERE receiver_id = '$other' AND plan_id = '$a'", "1", "but what she gave stays for who received it")};
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
        .andExpect(jsonPath("$.ledger", hasSize(1)))
        .andExpect(jsonPath("$.score", hasSize(1)))
        .andExpect(jsonPath("$.trackRecord", hasSize(1)))
        .andExpect(jsonPath("$.kudosGiven", hasSize(1)))
        .andExpect(jsonPath("$.kudosReceived", hasSize(1)));
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

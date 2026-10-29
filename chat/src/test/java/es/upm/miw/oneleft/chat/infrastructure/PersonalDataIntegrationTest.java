package es.upm.miw.oneleft.chat.infrastructure;

import es.upm.miw.oneleft.chat.TestcontainersConfiguration;
import es.upm.miw.oneleft.chat.domain.port.in.PersonalDataUseCase;
import es.upm.miw.oneleft.chat.infrastructure.messaging.AccountDeletedListener;
import es.upm.miw.oneleft.chat.infrastructure.rest.MyDataController;
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
            "INSERT INTO chat_message (id, plan_id, author_id, author_name, text, sent_at, plan_ends_at) VALUES (gen_random_uuid(), '$a', '$user', 'Lucía', 'hola', now(), now() + interval '1 day'), (gen_random_uuid(), '$a', '$other', 'Ana', 'buenas', now(), now() + interval '1 day')"};
    private static final Check[] AFTER = {
            new Check("SELECT count(*) FROM chat_message WHERE author_id = '$user'", "0", "nothing is from her any more"),
            new Check("SELECT count(*) FROM chat_message WHERE plan_id = '$a'", "2", "the conversation stays complete"),
            new Check("SELECT count(*) FROM chat_message WHERE plan_id = '$a' AND author_name = 'Persona eliminada'", "1", "her message without her name"),
            new Check("SELECT count(*) FROM chat_message WHERE plan_id = '$a' AND author_name = 'Ana'", "1", "and the other message as it was")};
    private static final String[] CLEANUP = {
            "DELETE FROM chat_message WHERE plan_id = '$a'"};

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
        .andExpect(jsonPath("$.messages", hasSize(1)));
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

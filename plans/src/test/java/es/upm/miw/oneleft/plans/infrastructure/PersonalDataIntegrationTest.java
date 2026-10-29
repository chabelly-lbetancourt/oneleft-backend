package es.upm.miw.oneleft.plans.infrastructure;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.port.in.PersonalDataUseCase;
import es.upm.miw.oneleft.plans.infrastructure.messaging.AccountDeletedListener;
import es.upm.miw.oneleft.plans.infrastructure.rest.MyDataController;
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
            "INSERT INTO plan (id, organizer_id, organizer_name, activity, title, meeting_point, location, starts_at, spots, occupied, status, published_at) VALUES ('$a', '$other', 'Ana', 'PADEL', 'Test A', 'Pistas', ST_SetSRID(ST_MakePoint(-3.6, 40.4), 4326), now() + interval '2 hours', 3, 2, 'OPEN', now())",
            "INSERT INTO plan (id, organizer_id, organizer_name, activity, title, meeting_point, location, starts_at, spots, occupied, status, published_at) VALUES ('$b', '$user', 'Lucía', 'PADEL', 'Test B', 'Pistas', ST_SetSRID(ST_MakePoint(-3.6, 40.4), 4326), now() + interval '2 hours', 2, 1, 'OPEN', now())",
            "INSERT INTO plan (id, organizer_id, organizer_name, activity, title, meeting_point, location, starts_at, spots, occupied, status, published_at) VALUES ('$c', '$other', 'Ana', 'PADEL', 'Test C', 'Pistas', ST_SetSRID(ST_MakePoint(-3.6, 40.4), 4326), now() - interval '5 hours', 2, 2, 'FINISHED', now() - interval '6 hours')",
            "INSERT INTO plan_participant (plan_id, user_id, name, joined_at) VALUES ('$a', '$user', 'Lucía', now()), ('$c', '$user', 'Lucía', now())",
            "INSERT INTO block (blocker_id, blocked_id, blocked_at) VALUES ('$user', '$other', now())"};
    private static final Check[] AFTER = {
            new Check("SELECT occupied FROM plan WHERE id = '$a'", "1", "a plan she was in has her spot free again"),
            new Check("SELECT count(*) FROM plan_participant WHERE plan_id = '$a'", "0", "and she is no longer a participant"),
            new Check("SELECT status FROM plan WHERE id = '$b'", "CANCELLED", "the upcoming plan she organized is cancelled"),
            new Check("SELECT organizer_name FROM plan WHERE id = '$b'", "Persona eliminada", "without her name"),
            new Check("SELECT name FROM plan_participant WHERE plan_id = '$c'", "Persona eliminada", "a plan already held keeps her participation, anonymous"),
            new Check("SELECT occupied FROM plan WHERE id = '$c'", "2", "and its spots"),
            new Check("SELECT count(*) FROM block WHERE '$user' IN (blocker_id, blocked_id)", "0", "her blocks are gone")};
    private static final String[] CLEANUP = {
            "DELETE FROM plan WHERE id IN ('$a', '$b', '$c')",
            "DELETE FROM block WHERE '$user' IN (blocker_id, blocked_id)"};

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
        .andExpect(jsonPath("$.participations", hasSize(2)))
        .andExpect(jsonPath("$.plansOrganized", hasSize(1)))
        .andExpect(jsonPath("$.blocks", hasSize(1)));
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

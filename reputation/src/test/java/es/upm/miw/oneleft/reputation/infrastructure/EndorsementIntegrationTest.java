package es.upm.miw.oneleft.reputation.infrastructure;

import es.upm.miw.oneleft.reputation.TestcontainersConfiguration;
import es.upm.miw.oneleft.reputation.infrastructure.rest.ReputationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-061 with a real PostgreSQL: who played with whom is read from the track record, and the endorsements and the
 * ledger are kept there. Everything created is erased at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EndorsementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transaction;

    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();

    private static RequestPostProcessor user(UUID id) {
        return jwt().jwt(token -> token.subject(id.toString()));
    }

    private static String endorsementsOf(UUID userId) {
        return ReputationController.REPUTATION + "/people/" + userId + "/endorsements";
    }

    /** Ana and Lucía arrived at a plan of padel and at one of running; Diego was in the padel one and did not arrive */
    @BeforeEach
    void plans() {
        var padel = UUID.randomUUID();
        var running = UUID.randomUUID();
        attended(ana, padel, "PADEL", true);
        attended(lucia, padel, "PADEL", true);
        attended(diego, padel, "PADEL", false);
        attended(ana, running, "RUNNING", true);
        attended(lucia, running, "RUNNING", true);
    }

    /** Erases what the test wrote: the ledger only allows it with the erasure setting on */
    @AfterEach
    void erase() {
        transaction.executeWithoutResult(status -> {
            jdbc.execute("SET LOCAL oneleft.ledger_erasure = 'on'");
            jdbc.update("DELETE FROM reputation_entry WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
        });
        jdbc.update("DELETE FROM endorsement WHERE receiver_id IN (?, ?, ?)", ana, lucia, diego);
        jdbc.update("DELETE FROM reputation_score WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
        jdbc.update("DELETE FROM track_record WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
    }

    private void attended(UUID userId, UUID planId, String activity, boolean arrived) {
        jdbc.update("INSERT INTO track_record (user_id, plan_id, activity, organizer, arrived, held, closed_at) "
                + "VALUES (?, ?, ?, false, ?, true, ?)", userId, planId, activity, arrived,
                Timestamp.from(Instant.now()));
    }

    private ResultActions endorse(UUID endorser, UUID receiver, String activity, String level) throws Exception {
        return mockMvc.perform(post(endorsementsOf(receiver)).with(user(endorser))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activity\": \"%s\", \"level\": \"%s\"}".formatted(activity, level)));
    }

    private Integer score(UUID userId) {
        return jdbc.query("SELECT score FROM reputation_score WHERE user_id = ?",
                rs -> rs.next() ? rs.getInt(1) : null, userId);
    }

    @Test
    void anEndorsementAddsEightOnceAndShowsInTheirProfile() throws Exception {
        endorse(lucia, ana, "PADEL", "INTERMEDIATE").andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiverId").value(ana.toString()))
                .andExpect(jsonPath("$.activity").value("PADEL"))
                .andExpect(jsonPath("$.level").value("INTERMEDIATE"));
        // One per person and activity, whatever the level
        endorse(lucia, ana, "PADEL", "ADVANCED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("endorsement.alreadyGiven"));

        assertThat(score(ana)).isEqualTo(8);
        assertThat(score(lucia)).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reputation_entry WHERE user_id = ? AND reason = "
                + "'ENDORSEMENT' AND points = 8", Integer.class, ana)).isEqualTo(1);
        mockMvc.perform(get(endorsementsOf(ana)).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ana.toString()))
                .andExpect(jsonPath("$.endorsements.length()").value(1))
                .andExpect(jsonPath("$.endorsements[0].activity").value("PADEL"))
                .andExpect(jsonPath("$.endorsements[0].level").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.endorsements[0].count").value(1))
                .andExpect(jsonPath("$.mine.length()").value(1))
                .andExpect(jsonPath("$.mine[0].activity").value("PADEL"))
                .andExpect(jsonPath("$.endorsable.length()").value(1))
                .andExpect(jsonPath("$.endorsable[0]").value("RUNNING"));
        // Someone else sees how many, never who, and what they gave themselves: nothing
        mockMvc.perform(get(endorsementsOf(ana)).with(user(diego)))
                .andExpect(jsonPath("$.endorsements[0].count").value(1))
                .andExpect(jsonPath("$.mine.length()").value(0))
                .andExpect(jsonPath("$.endorsable.length()").value(0));
        // In my own profile there is nothing to give
        mockMvc.perform(get(endorsementsOf(ana)).with(user(ana)))
                .andExpect(jsonPath("$.endorsements[0].count").value(1))
                .andExpect(jsonPath("$.endorsable.length()").value(0));
    }

    @Test
    void onlyWhoeverPlayedWithThemEndorsesAndNobodyThemselves() throws Exception {
        endorse(ana, ana, "PADEL", "ADVANCED").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("endorsement.self"));
        // Diego did not arrive at the plan, in either direction
        endorse(diego, ana, "PADEL", "INTERMEDIATE").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("endorsement.notPlayedTogether"));
        endorse(ana, diego, "PADEL", "INTERMEDIATE").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("endorsement.notPlayedTogether"));
        // They played together, but not that
        endorse(lucia, ana, "CHESS", "INTERMEDIATE").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("endorsement.notPlayedTogether"));
        // Someone who was never in a plan with them
        endorse(UUID.randomUUID(), ana, "PADEL", "INTERMEDIATE").andExpect(status().isForbidden());
        endorse(lucia, ana, "PADEL", "MASTER").andExpect(status().isBadRequest());
        endorse(lucia, ana, " ", "BEGINNER").andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM endorsement WHERE receiver_id IN (?, ?)",
                Integer.class, ana, diego)).isZero();
        assertThat(score(ana)).isNull();
    }

    @Test
    void endorsementsNeedAToken() throws Exception {
        mockMvc.perform(post(endorsementsOf(ana)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activity\": \"PADEL\", \"level\": \"BEGINNER\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(endorsementsOf(ana))).andExpect(status().isUnauthorized());
    }

    /** HU-066 with the real store: the same person is worth half, and a pair that endorses each other, nothing */
    @Test
    void aPairThatKeepsEndorsingEachOtherGetsNothingFromIt() throws Exception {
        endorse(lucia, ana, "PADEL", "INTERMEDIATE").andExpect(status().isCreated());
        endorse(ana, lucia, "PADEL", "INTERMEDIATE").andExpect(status().isCreated());
        endorse(lucia, ana, "RUNNING", "BEGINNER").andExpect(status().isCreated());
        endorse(ana, lucia, "RUNNING", "BEGINNER").andExpect(status().isCreated());

        assertThat(jdbc.queryForList("SELECT points FROM reputation_entry WHERE user_id = ? AND reason = "
                + "'ENDORSEMENT' ORDER BY id", Integer.class, ana)).containsExactly(8, 4);
        assertThat(jdbc.queryForList("SELECT points FROM reputation_entry WHERE user_id = ? AND reason = "
                + "'ENDORSEMENT' ORDER BY id", Integer.class, lucia)).containsExactly(8, 0);
        assertThat(jdbc.queryForList("SELECT limited_by FROM reputation_entry WHERE user_id IN (?, ?) AND reason = "
                + "'ENDORSEMENT' AND limited_by IS NOT NULL ORDER BY id", String.class, ana, lucia))
                .containsExactly("SAME_PERSON", "MUTUAL_PAIR");
        assertThat(score(ana)).isEqualTo(12);
        assertThat(score(lucia)).isEqualTo(8);
        // The endorsements themselves are kept
        mockMvc.perform(get(endorsementsOf(lucia)).with(user(ana)))
                .andExpect(jsonPath("$.endorsements.length()").value(2))
                .andExpect(jsonPath("$.endorsable.length()").value(0));
    }
}

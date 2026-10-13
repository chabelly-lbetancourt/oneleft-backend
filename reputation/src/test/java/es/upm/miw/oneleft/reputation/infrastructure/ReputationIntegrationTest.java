package es.upm.miw.oneleft.reputation.infrastructure;

import es.upm.miw.oneleft.reputation.TestcontainersConfiguration;
import es.upm.miw.oneleft.reputation.infrastructure.messaging.RabbitConfig;
import es.upm.miw.oneleft.reputation.infrastructure.rest.ReputationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-009 end to end inside the service: the events arrive through a real RabbitMQ as the plans service publishes them
 * (plain JSON), the ledger and the scores are in a real PostgreSQL, and the API reads them. The entries and scores
 * created are erased at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReputationIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final String ME = ReputationController.REPUTATION + ReputationController.ME;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private MockMvc mockMvc;

    private final UUID plan = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();

    private static RequestPostProcessor user(UUID id, String... roles) {
        return jwt().jwt(token -> token.subject(id.toString())
                .claim("realm_access", Map.of("roles", List.of(roles))));
    }

    /** Erases what the test wrote: the ledger only allows it with the erasure setting on */
    @AfterEach
    void erase() {
        transaction.executeWithoutResult(status -> {
            jdbc.execute("SET LOCAL oneleft.ledger_erasure = 'on'");
            jdbc.update("DELETE FROM reputation_entry WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
        });
        jdbc.update("DELETE FROM reputation_score WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
    }

    /** An event as another service sends it: JSON, with nothing of the classes of this one */
    private void publish(String exchange, String key, Map<String, ?> event) {
        var properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbit.send(exchange, key, MessageBuilder.withBody(json.writeValueAsBytes(event))
                .andProperties(properties).build());
    }

    private void waitUntil(Callable<Boolean> condition) throws Exception {
        var deadline = Instant.now().plus(WAIT);
        while (!condition.call()) {
            assertThat(Instant.now()).as("waited for the reputation").isBefore(deadline);
            Thread.sleep(100);
        }
    }

    private Integer score(UUID userId) {
        return jdbc.query("SELECT score FROM reputation_score WHERE user_id = ?",
                rs -> rs.next() ? rs.getInt(1) : null, userId);
    }

    @Test
    void theEventsOfPlansBecomeTheLedgerAndTheScoreOnceEach() throws Exception {
        var checkIn = Map.of("planId", plan, "title", "Padel 2 vs 2", "userId", lucia, "name", "Lucía",
                "recipientIds", List.of(ana), "occurredAt", "2026-10-13T16:05:00Z");
        publish(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_CHECKED_IN, checkIn);
        publish(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_CHECKED_IN, checkIn);
        publish(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_ATTENDANCE_CLOSED, Map.of("planId", plan,
                "title", "Padel 2 vs 2", "organizerId", ana, "attendeeIds", List.of(lucia),
                "absenteeIds", List.of(ana, diego), "full", true, "startsAt", "2026-10-13T16:00:00Z",
                "occurredAt", "2026-10-13T16:30:00Z"));
        publish(RabbitConfig.REPORTS_EXCHANGE, RabbitConfig.REPORT_CONFIRMED, Map.of("reportId", UUID.randomUUID(),
                "userId", diego, "occurredAt", "2026-10-13T17:00:00Z"));

        waitUntil(() -> Integer.valueOf(-65).equals(score(diego)) && score(ana) != null);

        assertThat(score(lucia)).isEqualTo(10);
        assertThat(score(ana)).isEqualTo(-15 + 20);
        mockMvc.perform(get(ME + "/ledger").with(user(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].planTitle").value("Padel 2 vs 2"))
                .andExpect(jsonPath("$.items[0].planId").value(plan.toString()));
        mockMvc.perform(get(ME).with(user(lucia))).andExpect(jsonPath("$.score").value(10));
        mockMvc.perform(get(ReputationController.REPUTATION + "/scores").param("ids", diego + "," + lucia)
                        .with(user(ana)))
                .andExpect(jsonPath("$[0].score").value(-65))
                .andExpect(jsonPath("$[1].score").value(10));
        mockMvc.perform(get(ReputationController.REPUTATION + "/people/" + UUID.randomUUID()).with(user(ana)))
                .andExpect(jsonPath("$.score").value(0));
    }

    @Test
    void aCancellationByTheOrganizerCloseToTheStartCostsAndOneForTheMinimumDoesNot() throws Exception {
        publish(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_CANCELLED, Map.of("planId", plan, "organizerId", ana,
                "title", "Padel", "startsAt", "2026-10-13T16:20:00Z", "reason", "MINIMUM_NOT_REACHED",
                "recipientIds", List.of(ana), "occurredAt", "2026-10-13T16:00:00Z"));
        publish(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_CANCELLED, Map.of("planId", UUID.randomUUID(),
                "organizerId", lucia, "title", "Running", "startsAt", "2026-10-13T16:20:00Z", "reason", "ORGANIZER",
                "occurredAt", "2026-10-13T16:00:00Z"));

        waitUntil(() -> score(lucia) != null);

        assertThat(score(lucia)).isEqualTo(-10);
        assertThat(score(ana)).isNull();
    }

    @Test
    void anEventThatCannotBeReadEndsInTheDeadLetters() {
        var properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbit.send(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_CHECKED_IN,
                MessageBuilder.withBody("not json".getBytes(StandardCharsets.UTF_8)).andProperties(properties).build());

        var dead = rabbit.receive(RabbitConfig.DEAD_LETTERS_QUEUE, WAIT.toMillis());

        assertThat(dead).isNotNull();
        assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).isEqualTo("not json");
    }

    @Test
    void theLedgerCannotBeChangedAndTheScoresAreRebuiltFromItByAnAdministrator() throws Exception {
        publish(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_CHECKED_IN, Map.of("planId", plan, "title", "Padel",
                "userId", lucia, "occurredAt", "2026-10-13T16:05:00Z"));
        waitUntil(() -> score(lucia) != null);

        assertThatThrownBy(() -> jdbc.update("UPDATE reputation_entry SET points = 100 WHERE user_id = ?", lucia))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM reputation_entry WHERE user_id = ?", lucia))
                .isInstanceOf(DataAccessException.class);

        jdbc.update("UPDATE reputation_score SET score = 999 WHERE user_id = ?", lucia);
        mockMvc.perform(post(ReputationController.REPUTATION + ReputationController.REBUILD).with(user(ana)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ReputationController.REPUTATION + ReputationController.REBUILD)
                        .with(user(ana, "admin")))
                .andExpect(status().isOk());
        assertThat(score(lucia)).isEqualTo(10);
    }

    @Test
    void theApiAnswersWithStableCodes() throws Exception {
        mockMvc.perform(get(ReputationController.REPUTATION + "/scores").with(user(ana)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("validation"));
        mockMvc.perform(get(ME + "/ledger").param("size", "51").with(user(ana)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("page.invalid"));
        mockMvc.perform(get(ME)).andExpect(status().isUnauthorized());
    }
}

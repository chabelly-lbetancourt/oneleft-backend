package es.upm.miw.oneleft.reputation.infrastructure;

import es.upm.miw.oneleft.reputation.TestcontainersConfiguration;
import es.upm.miw.oneleft.reputation.infrastructure.messaging.RabbitConfig;
import es.upm.miw.oneleft.reputation.infrastructure.rest.KudosController;
import es.upm.miw.oneleft.reputation.infrastructure.rest.ReputationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-060 end to end inside the service: who arrived comes from the attendance event of the plans service through a
 * real RabbitMQ, the kudos and the ledger are in a real PostgreSQL, and the notice leaves through RabbitMQ. Everything
 * created is erased at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class KudosIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(10);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private TopicExchange notificationsExchange;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transaction;

    private final UUID plan = UUID.randomUUID();
    private final UUID old = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();
    private final Queue notices = new Queue("test.kudos." + UUID.randomUUID(), true, false, false);

    @BeforeEach
    void notices() {
        admin.declareQueue(notices);
        admin.declareBinding(BindingBuilder.bind(notices).to(notificationsExchange).with(RabbitConfig.KUDOS_RECEIVED));
    }

    /** Erases what the test wrote: the ledger only allows it with the erasure setting on */
    @AfterEach
    void erase() {
        admin.deleteQueue(notices.getName());
        transaction.executeWithoutResult(status -> {
            jdbc.execute("SET LOCAL oneleft.ledger_erasure = 'on'");
            jdbc.update("DELETE FROM reputation_entry WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
        });
        jdbc.update("DELETE FROM kudos WHERE plan_id IN (?, ?)", plan, old);
        jdbc.update("DELETE FROM reputation_score WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
        jdbc.update("DELETE FROM track_record WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
        jdbc.update("DELETE FROM badge WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
    }

    /** The attendance of a plan closes, as the plans service publishes it: Ana organized it, Diego did not arrive */
    private void attendanceClosed(UUID planId, Instant closedAt) throws Exception {
        var properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbit.send(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_ATTENDANCE_CLOSED, MessageBuilder.withBody(
                json.writeValueAsBytes(Map.of("planId", planId, "title", "Padel 2 vs 2", "activity", "PADEL",
                        "organizerId", ana, "attendeeIds", List.of(ana, lucia), "absenteeIds", List.of(diego),
                        "full", true, "startsAt", closedAt.minus(Duration.ofHours(2)).toString(),
                        "occurredAt", closedAt.toString()))).andProperties(properties).build());
        waitUntil(() -> jdbc.queryForObject("SELECT COUNT(*) FROM track_record WHERE plan_id = ?", Integer.class,
                planId) == 3);
    }

    private void waitUntil(Callable<Boolean> condition) throws Exception {
        var deadline = Instant.now().plus(WAIT);
        while (!condition.call()) {
            assertThat(Instant.now()).as("waited for the reputation").isBefore(deadline);
            Thread.sleep(100);
        }
    }

    private ResultActions give(UUID giver, UUID planId, UUID receiver, String kind) throws Exception {
        return mockMvc.perform(post(ReputationController.REPUTATION + KudosController.PLAN_KUDOS, planId)
                .with(jwt().jwt(token -> token.subject(giver.toString()).claim("name", "Lucía")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiverId\": \"%s\", \"kind\": \"%s\"}".formatted(receiver, kind)));
    }

    private Integer score(UUID userId) {
        return jdbc.query("SELECT score FROM reputation_score WHERE user_id = ?",
                rs -> rs.next() ? rs.getInt(1) : null, userId);
    }

    @Test
    void aKudosAddsToTheLedgerOnceAndIsToldAfterItIsSaved() throws Exception {
        attendanceClosed(plan, Instant.now());
        var before = score(ana);

        give(lucia, plan, ana, "GREAT_ORGANIZER").andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("GREAT_ORGANIZER"));
        give(lucia, plan, ana, "PUNCTUAL").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("kudos.alreadyGiven"));

        assertThat(score(ana)).isEqualTo(before + 5);
        assertThat(jdbc.queryForObject("SELECT plan_title FROM reputation_entry WHERE user_id = ? AND reason = "
                + "'KUDOS'", String.class, ana)).isEqualTo("Padel 2 vs 2");
        Message notice = rabbit.receive(notices.getName(), WAIT.toMillis());
        assertThat(notice).isNotNull();
        var body = json.readTree(notice.getBody());
        assertThat(body.path("userId").asString()).isEqualTo(ana.toString());
        assertThat(body.path("giverName").asString()).isEqualTo("Lucía");
        assertThat(body.path("kind").asString()).isEqualTo("GREAT_ORGANIZER");
        assertThat(rabbit.receive(notices.getName(), 1000)).isNull();

        mockMvc.perform(get(ReputationController.REPUTATION + "/people/" + ana + "/kudos")
                        .with(jwt().jwt(token -> token.subject(diego.toString()))))
                .andExpect(jsonPath("$[?(@.kind == 'GREAT_ORGANIZER')].count").value(1))
                .andExpect(jsonPath("$[?(@.kind == 'PUNCTUAL')].count").value(0));
    }

    @Test
    void theKudosOfAPlanAreForWhoeverArrived() throws Exception {
        attendanceClosed(plan, Instant.now());
        give(lucia, plan, ana, "GOOD_VIBES").andExpect(status().isCreated());

        mockMvc.perform(get(ReputationController.REPUTATION + KudosController.PLAN_KUDOS, plan)
                        .with(jwt().jwt(token -> token.subject(lucia.toString()))))
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.people.length()").value(1))
                .andExpect(jsonPath("$.people[0].userId").value(ana.toString()))
                .andExpect(jsonPath("$.people[0].organizer").value(true))
                .andExpect(jsonPath("$.people[0].given").value("GOOD_VIBES"));
        mockMvc.perform(get(ReputationController.REPUTATION + KudosController.PLAN_KUDOS, plan)
                        .with(jwt().jwt(token -> token.subject(diego.toString()))))
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.people.length()").value(0));
        give(diego, plan, ana, "PUNCTUAL").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("kudos.notAttended"));
        give(lucia, plan, diego, "PUNCTUAL").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("kudos.receiverNotAttended"));
        give(ana, plan, lucia, "GREAT_ORGANIZER").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("kudos.notOrganizer"));
        give(lucia, plan, ana, "BEST_FRIEND").andExpect(status().isBadRequest());
    }

    @Test
    void aWeekAfterThePlanNoMoreKudos() throws Exception {
        attendanceClosed(old, Instant.now().minus(Duration.ofDays(8)));

        give(lucia, old, ana, "PUNCTUAL").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("kudos.closed"));
        mockMvc.perform(get(ReputationController.REPUTATION + KudosController.PLAN_KUDOS, old)
                        .with(jwt().jwt(token -> token.subject(lucia.toString()))))
                .andExpect(jsonPath("$.open").value(false));
    }
}

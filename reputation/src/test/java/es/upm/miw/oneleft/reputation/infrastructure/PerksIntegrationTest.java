package es.upm.miw.oneleft.reputation.infrastructure;

import es.upm.miw.oneleft.reputation.TestcontainersConfiguration;
import es.upm.miw.oneleft.reputation.infrastructure.messaging.RabbitConfig;
import es.upm.miw.oneleft.reputation.infrastructure.rest.ReputationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-063 inside the service, with a real PostgreSQL and a real RabbitMQ: the perks of my level, the seal of trusted
 * organizer and the event that tells the other services what a level is worth. What the test writes is erased at the
 * end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PerksIntegrationTest {

    private static final String REPUTATION = ReputationController.REPUTATION;
    private static final String MY_PERKS = REPUTATION + ReputationController.MY_PERKS;

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

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private TopicExchange reputationExchange;

    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    private static RequestPostProcessor user(UUID id) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("realm_access", Map.of("roles", List.of())));
    }

    /** Erases what the test wrote: the ledger only allows it with the erasure setting on */
    @AfterEach
    void erase() {
        transaction.executeWithoutResult(status -> {
            jdbc.execute("SET LOCAL oneleft.ledger_erasure = 'on'");
            jdbc.update("DELETE FROM reputation_entry WHERE user_id IN (?, ?)", ana, lucia);
        });
        jdbc.update("DELETE FROM reputation_score WHERE user_id IN (?, ?)", ana, lucia);
        jdbc.update("DELETE FROM track_record WHERE user_id IN (?, ?)", ana, lucia);
    }

    private void score(UUID userId, int score) {
        jdbc.update("INSERT INTO reputation_score (user_id, score) VALUES (?, ?)", userId, score);
    }

    private void organizedHeld(UUID organizer, int plans) {
        for (var plan = 0; plan < plans; plan++) {
            jdbc.update("INSERT INTO track_record (user_id, plan_id, activity, organizer, arrived, held, closed_at) "
                    + "VALUES (?, ?, 'PADEL', true, true, true, ?)", organizer, UUID.randomUUID(),
                    Timestamp.from(Instant.now()));
        }
    }

    @Test
    void myPerksAreTheOnesOfMyLevelAndOnlyWithAToken() throws Exception {
        score(lucia, 620);

        mockMvc.perform(get(MY_PERKS).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("NEIGHBOURHOOD_REFERENCE"))
                .andExpect(jsonPath("$.waitlistPriority").value(2))
                .andExpect(jsonPath("$.noticeWeight").value(1.25))
                .andExpect(jsonPath("$.maxActivePlans").value(8))
                .andExpect(jsonPath("$.maxSavedAlerts").value(8))
                .andExpect(jsonPath("$.trustedOrganizer").value(false));
        // Someone else gets theirs, never mine: the person is the one of the token
        mockMvc.perform(get(MY_PERKS).with(user(ana)))
                .andExpect(jsonPath("$.level").value("NEW"))
                .andExpect(jsonPath("$.waitlistPriority").value(0))
                .andExpect(jsonPath("$.maxActivePlans").value(5))
                .andExpect(jsonPath("$.maxSavedAlerts").value(5));
        mockMvc.perform(get(MY_PERKS)).andExpect(status().isUnauthorized());
    }

    @Test
    void theSealOfTrustedOrganizerComesWithTheReputationOfEachPerson() throws Exception {
        score(ana, 150);
        score(lucia, 150);
        organizedHeld(ana, 5);
        organizedHeld(lucia, 4);

        mockMvc.perform(get(REPUTATION + "/scores").param("ids", ana.toString(), lucia.toString())
                        .with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trustedOrganizer").value(true))
                .andExpect(jsonPath("$[1].trustedOrganizer").value(false));
        mockMvc.perform(get(REPUTATION + "/people/" + ana).with(user(lucia)))
                .andExpect(jsonPath("$.trustedOrganizer").value(true));
        mockMvc.perform(get(MY_PERKS).with(user(ana))).andExpect(jsonPath("$.trustedOrganizer").value(true));
        mockMvc.perform(get(REPUTATION + ReputationController.ME).with(user(lucia)))
                .andExpect(jsonPath("$.trustedOrganizer").value(false));
    }

    @Test
    void reachingALevelTellsTheOtherServicesWhatItIsWorth() throws Exception {
        var copies = new Queue("test.perks." + UUID.randomUUID(), true, false, false);
        admin.declareQueue(copies);
        admin.declareBinding(BindingBuilder.bind(copies).to(reputationExchange).with(RabbitConfig.PERKS_CHANGED));
        try {
            score(lucia, 95);
            var properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            rabbit.send(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_CHECKED_IN,
                    MessageBuilder.withBody(json.writeValueAsBytes(Map.of("planId", UUID.randomUUID(), "title",
                            "Padel", "userId", lucia, "occurredAt", "2026-11-24T16:05:00Z")))
                            .andProperties(properties).build());

            var notice = rabbit.receive(copies.getName(), 10_000);

            assertThat(notice).isNotNull();
            var perks = json.readTree(notice.getBody());
            assertThat(perks.path("userId").asString()).isEqualTo(lucia.toString());
            assertThat(perks.path("level").asString()).isEqualTo("REGULAR");
            assertThat(perks.path("waitlistPriority").asInt()).isEqualTo(1);
            assertThat(perks.path("noticeWeight").asDouble()).isEqualTo(1.1);
            assertThat(perks.path("maxActivePlans").asInt()).isEqualTo(6);
            assertThat(perks.path("maxSavedAlerts").asInt()).isEqualTo(6);
            assertThat(perks.path("occurredAt").asString()).isNotBlank();
            // The bonus of the streak comes after, in the same transaction, and leaves the level as it was: it
            // is counted on top of the points of the check-in, and nothing else is told
            assertThat(rabbit.receive(copies.getName(), 1000)).isNull();
            assertThat(jdbc.queryForObject("SELECT score FROM reputation_score WHERE user_id = ?", Integer.class,
                    lucia)).isEqualTo(95 + 10 + 5);
        } finally {
            admin.deleteQueue(copies.getName());
        }
    }
}

package es.upm.miw.oneleft.notifications.infrastructure;

import es.upm.miw.oneleft.notifications.TestcontainersConfiguration;
import es.upm.miw.oneleft.notifications.domain.model.MemberPerks;
import es.upm.miw.oneleft.notifications.domain.port.out.PerkCopies;
import es.upm.miw.oneleft.notifications.infrastructure.messaging.RabbitConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-063: the copy of the perks with a real PostgreSQL, filled by the event of the reputation service as it sends it
 * (plain JSON) through a real RabbitMQ. What the test creates is removed at the end.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PerkCopiesIntegrationTest {

    @Autowired
    private PerkCopies perks;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM member_perks WHERE user_id IN (?, ?)", lucia, diego);
    }

    @Test
    void theLatestPerksOfEachPersonAreKept() {
        perks.save(new MemberPerks(lucia, "REGULAR", 1.1, 6, now));
        perks.save(new MemberPerks(lucia, "NEW", 1.0, 5, now.minusSeconds(60)));

        assertThat(perks.findByUsers(List.of(lucia, diego)))
                .containsOnly(Map.entry(lucia, new MemberPerks(lucia, "REGULAR", 1.1, 6, now)));

        perks.save(new MemberPerks(lucia, "LEGEND", 1.5, 10, now.plusSeconds(60)));

        assertThat(perks.findByUsers(List.of(lucia)).get(lucia).level()).isEqualTo("LEGEND");
        assertThat(perks.findByUsers(List.of())).isEmpty();
    }

    @Test
    void theEventOfTheReputationServiceFillsTheCopy() throws Exception {
        var properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbit.send(RabbitConfig.REPUTATION_EXCHANGE, RabbitConfig.PERKS_CHANGED,
                MessageBuilder.withBody(json.writeValueAsBytes(Map.of("userId", diego, "level",
                        "NEIGHBOURHOOD_REFERENCE", "waitlistPriority", 2, "noticeWeight", 1.25, "maxActivePlans", 8,
                        "maxSavedAlerts", 8, "occurredAt", now.toString()))).andProperties(properties).build());

        var deadline = Instant.now().plusSeconds(10);
        while (perks.findByUsers(List.of(diego)).isEmpty()) {
            assertThat(Instant.now()).as("waited for the copy").isBefore(deadline);
            Thread.sleep(100);
        }

        assertThat(perks.findByUsers(List.of(diego)).get(diego))
                .isEqualTo(new MemberPerks(diego, "NEIGHBOURHOOD_REFERENCE", 1.25, 8, now));
    }
}

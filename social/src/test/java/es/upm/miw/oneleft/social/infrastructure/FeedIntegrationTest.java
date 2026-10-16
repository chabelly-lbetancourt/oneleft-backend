package es.upm.miw.oneleft.social.infrastructure;

import es.upm.miw.oneleft.social.TestcontainersConfiguration;
import es.upm.miw.oneleft.social.infrastructure.messaging.RabbitConfig;
import es.upm.miw.oneleft.social.infrastructure.rest.FeedController;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialController;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-051 end to end inside the service: the events arrive through a real RabbitMQ as the plans service publishes them
 * (plain JSON), the activity and the follows are in a real PostgreSQL, the first page of each feed in a real Redis, and
 * the notices leave through RabbitMQ. The people, activity, cached pages and queues created are deleted at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FeedIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final String FEED = SocialController.SOCIAL + SocialController.ME + FeedController.FEED;
    private static final String STARTS_AT = "2026-10-16T18:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private TopicExchange notificationsExchange;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private JsonMapper json;

    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();
    private final UUID plan = UUID.randomUUID();
    private final Queue notices = new Queue("test.feed-activity." + UUID.randomUUID(), true, false, false);

    private void person(UUID id, String name, String visibility) {
        jdbc.update("INSERT INTO person (user_id, name, visibility) VALUES (?, ?, ?)", id, name, visibility);
    }

    private void follow(UUID follower, UUID followee, String status) {
        jdbc.update("""
                INSERT INTO follow (follower_id, followee_id, status, requested_at, accepted_at)
                VALUES (?, ?, ?, now(), CASE WHEN ? = 'ACCEPTED' THEN now() END)""",
                follower, followee, status, status);
    }

    @BeforeEach
    void people() {
        person(ana, "Ana", "PRIVATE");
        person(lucia, "Lucía", "PRIVATE");
        person(diego, "Diego", "PRIVATE");
        follow(lucia, ana, "ACCEPTED");
        // Diego has not accepted Lucía yet: his activity is not for her
        follow(lucia, diego, "PENDING");
        admin.declareQueue(notices);
        admin.declareBinding(BindingBuilder.bind(notices).to(notificationsExchange).with(RabbitConfig.FEED_ACTIVITY));
    }

    @AfterEach
    void deleteWhatWasCreated() {
        admin.deleteQueue(notices.getName());
        jdbc.update("DELETE FROM activity WHERE actor_id IN (?, ?, ?) OR plan_id = ?", ana, lucia, diego, plan);
        jdbc.update("DELETE FROM person WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
        redis.delete(List.of(key(ana), key(lucia), key(diego)));
    }

    private static String key(UUID viewer) {
        return "oneleft:social:feed:" + viewer;
    }

    /** An event as the plans service sends it: JSON, with nothing of the classes of this one */
    private void publish(String key, Map<String, ?> event) {
        var properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbit.send(RabbitConfig.PLANS_EXCHANGE, key, MessageBuilder.withBody(json.writeValueAsBytes(event))
                .andProperties(properties).build());
    }

    private Map<String, Object> published(UUID organizer) {
        return Map.of("planId", plan, "organizerId", organizer, "activity", "PADEL", "title", "Padel 2 vs 2",
                "placeName", "Club", "latitude", 40.39, "longitude", -3.65, "startsAt", STARTS_AT, "freeSpots", 3,
                "occurredAt", "2026-10-16T10:00:00Z");
    }

    private Map<String, Object> joined(UUID participant) {
        return Map.of("planId", plan, "organizerId", ana, "title", "Padel 2 vs 2", "activity", "PADEL",
                "startsAt", STARTS_AT, "participantId", participant, "participantName", "Diego", "freeSpots", 2,
                "full", false, "occurredAt", "2026-10-16T10:05:00Z");
    }

    private int activities() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM activity WHERE plan_id = ?", Integer.class, plan);
    }

    private void waitUntil(Callable<Boolean> condition) throws Exception {
        var deadline = Instant.now().plus(WAIT);
        while (!condition.call()) {
            assertThat(Instant.now()).as("waited for the feed").isBefore(deadline);
            Thread.sleep(100);
        }
    }

    private ResultActions feedOf(UUID viewer) throws Exception {
        return mockMvc.perform(get(FEED).with(jwt().jwt(token -> token.subject(viewer.toString()))));
    }

    private List<JsonNode> receivedNotices() {
        var received = new ArrayList<JsonNode>();
        Message message;
        while ((message = rabbit.receive(notices.getName(), 1000)) != null) {
            received.add(json.readTree(message.getBody()));
        }
        return received;
    }

    @Test
    void theFeedShowsWhatThePeopleFollowedDidOnceTheyAccepted() throws Exception {
        publish(RabbitConfig.PLAN_PUBLISHED, published(ana));
        publish(RabbitConfig.PLAN_PUBLISHED, published(ana));
        publish(RabbitConfig.PLAN_JOINED, joined(diego));
        publish(RabbitConfig.PLAN_ATTENDANCE_CLOSED, Map.of("planId", plan, "title", "Padel 2 vs 2",
                "activity", "PADEL", "organizerId", ana, "attendeeIds", List.of(ana, diego),
                "absenteeIds", List.of(), "full", true, "startsAt", STARTS_AT,
                "occurredAt", "2026-10-16T19:30:00Z"));

        // Published once although it came twice, joined, and completed by two
        waitUntil(() -> activities() == 4);

        feedOf(lucia).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].actorName").value("Ana"))
                .andExpect(jsonPath("$.items[0].type").value("COMPLETED"))
                .andExpect(jsonPath("$.items[1].type").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[1].planTitle").value("Padel 2 vs 2"))
                .andExpect(jsonPath("$.items[1].planActivity").value("PADEL"))
                .andExpect(jsonPath("$.items[1].startsAt").value(STARTS_AT));
        // Lucía was told twice, once per new activity of Ana; nobody follows Diego yet
        assertThat(receivedNotices()).allSatisfy(notice -> {
            assertThat(notice.path("userId").asString()).isEqualTo(lucia.toString());
            assertThat(notice.path("actorName").asString()).isEqualTo("Ana");
        }).extracting(notice -> notice.path("type").asString()).containsExactlyInAnyOrder("PUBLISHED", "COMPLETED");

        jdbc.update("UPDATE follow SET status = 'ACCEPTED', accepted_at = now() WHERE follower_id = ? "
                + "AND followee_id = ?", lucia, diego);
        redis.delete(key(lucia));
        feedOf(lucia).andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void theFirstPageIsCachedUntilWhatItShowsChanges() throws Exception {
        publish(RabbitConfig.PLAN_PUBLISHED, published(ana));
        waitUntil(() -> activities() == 1);

        feedOf(lucia).andExpect(jsonPath("$.items", hasSize(1)));
        assertThat(redis.hasKey(key(lucia))).isTrue();
        // Other pages are not cached
        mockMvc.perform(get(FEED).param("page", "1").with(jwt().jwt(token -> token.subject(ana.toString()))))
                .andExpect(status().isOk());
        assertThat(redis.hasKey(key(ana))).isFalse();

        publish(RabbitConfig.PLAN_CANCELLED, Map.of("planId", plan, "organizerId", ana, "title", "Padel 2 vs 2",
                "placeName", "Club", "startsAt", STARTS_AT, "recipientIds", List.of(), "occurredAt",
                "2026-10-16T11:00:00Z"));
        waitUntil(() -> activities() == 0 && !redis.hasKey(key(lucia)));

        feedOf(lucia).andExpect(jsonPath("$.items", hasSize(0))).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void whoeverLeavesIsReplacedByWhoeverWasWaiting() throws Exception {
        publish(RabbitConfig.PLAN_JOINED, joined(lucia));
        waitUntil(() -> activities() == 1);

        publish(RabbitConfig.PLAN_LEFT, Map.ofEntries(Map.entry("planId", plan), Map.entry("organizerId", diego),
                Map.entry("title", "Padel 2 vs 2"), Map.entry("activity", "PADEL"), Map.entry("startsAt", STARTS_AT),
                Map.entry("participantId", lucia), Map.entry("participantName", "Lucía"),
                Map.entry("promotedId", ana), Map.entry("promotedName", "Ana"), Map.entry("freeSpots", 0),
                Map.entry("full", true), Map.entry("occurredAt", "2026-10-16T12:00:00Z")));
        waitUntil(() -> jdbc.queryForObject("SELECT COUNT(*) FROM activity WHERE plan_id = ? AND actor_id = ?",
                Integer.class, plan, ana) == 1);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity WHERE plan_id = ? AND actor_id = ?",
                Integer.class, plan, lucia)).isZero();
        feedOf(lucia).andExpect(jsonPath("$.items[0].actorName").value("Ana"))
                .andExpect(jsonPath("$.items[0].type").value("JOINED"));
    }

    @Test
    void theApiAnswersWithStableCodes() throws Exception {
        mockMvc.perform(get(FEED).param("size", "51").with(jwt().jwt(token -> token.subject(lucia.toString()))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("page.invalid"));
        mockMvc.perform(get(FEED)).andExpect(status().isUnauthorized());
    }
}

package es.upm.miw.oneleft.notifications;

import es.upm.miw.oneleft.notifications.infrastructure.messaging.RabbitConfig;
import es.upm.miw.oneleft.notifications.infrastructure.rest.NotificationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The service end to end with a real PostgreSQL and RabbitMQ: preferences through the API, a plan published by the
 * plans service and the notice that comes out for the app.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationsIntegrationTest {

    private static final String BASE = NotificationController.NOTIFICATIONS;
    private static final String NEAR_VALLECAS = """
            {"enabled":true,"latitude":40.3912,"longitude":-3.6287,"radiusMeters":3000,"activities":["PADEL"],
             "quietHours":null,"maxPerDay":5}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RabbitTemplate rabbit;
    @Autowired
    private AmqpAdmin admin;

    private Queue notices;

    @BeforeEach
    void listenToTheNoticesForTheApp() {
        // Exclusive to this connection, but not auto-deleted: each receive adds and removes a consumer
        var queue = QueueBuilder.nonDurable("test.notices." + UUID.randomUUID()).exclusive().build();
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(new TopicExchange(RabbitConfig.NOTIFICATIONS_EXCHANGE))
                .with(RabbitConfig.NEARBY_PLAN));
        notices = queue;
    }

    private static RequestPostProcessor user(UUID id) {
        return jwt().jwt(token -> token.subject(id.toString()));
    }

    @Test
    void noticesAreOffUntilThePersonTurnsThemOn() throws Exception {
        var lucia = UUID.randomUUID();
        mockMvc.perform(get(BASE + "/preferences").with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.radiusMeters").value(3000))
                .andExpect(jsonPath("$.quietHours.start").value("23:00:00"))
                .andExpect(jsonPath("$.maxPerDay").value(5));

        mockMvc.perform(put(BASE + "/preferences").with(user(lucia)).contentType(MediaType.APPLICATION_JSON)
                        .content(NEAR_VALLECAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.latitude").value(40.39))
                .andExpect(jsonPath("$.longitude").value(-3.63))
                .andExpect(jsonPath("$.activities[0]").value("PADEL"));

        mockMvc.perform(get(BASE + "/preferences").with(user(lucia)))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.quietHours").isEmpty());
    }

    @Test
    void invalidPreferencesAreRejectedWithACode() throws Exception {
        mockMvc.perform(put(BASE + "/preferences").with(user(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                        .content(NEAR_VALLECAS.replace("3000", "50000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("notifications.radius"));
        mockMvc.perform(put(BASE + "/preferences").with(user(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                        .content(NEAR_VALLECAS.replace("\"quietHours\":null", "\"quietHours\":{\"start\":\"08:00\",\"end\":\"08:00\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("notifications.quietHours"));
    }

    @Test
    void theApiNeedsASession() throws Exception {
        mockMvc.perform(get(BASE + "/preferences")).andExpect(status().isUnauthorized());
    }

    @Test
    void browsersSubscribeAndUnsubscribe() throws Exception {
        var lucia = UUID.randomUUID();
        mockMvc.perform(get(BASE + "/push/public-key").with(user(lucia))).andExpect(status().isNotFound());
        mockMvc.perform(put(BASE + "/push/subscriptions").with(user(lucia)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endpoint":"https://push.example.org/send/%s","keys":{"p256dh":"BExample","auth":"secret"},
                                 "language":"en"}""".formatted(lucia)))
                .andExpect(status().isNoContent());
        mockMvc.perform(put(BASE + "/push/subscriptions").with(user(lucia)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"http://insecure.example.org\",\"keys\":{\"p256dh\":\"k\",\"auth\":\"a\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("notifications.pushEndpoint"));
        mockMvc.perform(delete(BASE + "/push/subscriptions").with(user(lucia))
                        .param("endpoint", "https://push.example.org/send/" + lucia))
                .andExpect(status().isNoContent());
    }

    @Test
    void aPlanPublishedNearbyReachesThePeopleWhoWantItOnce() throws Exception {
        var lucia = UUID.randomUUID();
        mockMvc.perform(put(BASE + "/preferences").with(user(lucia)).contentType(MediaType.APPLICATION_JSON)
                .content(NEAR_VALLECAS)).andExpect(status().isOk());
        var planId = UUID.randomUUID();
        var startsAt = Instant.now().plus(90, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);

        publishPlan(planId, startsAt, "PADEL");
        publishPlan(planId, startsAt, "PADEL");
        publishPlan(UUID.randomUUID(), startsAt, "CINEMA");

        // Other tests share the database: keep only the notices for this person
        var forLucia = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> notice;
        while ((notice = rabbit.receiveAndConvert(notices.getName(), forLucia.isEmpty() ? 20_000 : 3_000,
                new ParameterizedTypeReference<>() {
                })) != null) {
            if (lucia.toString().equals(notice.get("userId"))) {
                forLucia.add(notice);
            }
        }
        assertThat(forLucia).as("once per plan and only padel").singleElement().satisfies(received ->
                assertThat(received).containsEntry("planId", planId.toString())
                        .containsEntry("title", "Partido de pádel, falta uno").containsEntry("distanceMeters", 200)
                        .containsEntry("startsAt", startsAt.toString()));
    }

    /** As the plans service sends it: JSON with the type header of its own class, which this service does not have. */
    private void publishPlan(UUID planId, Instant startsAt, String activity) {
        var json = """
                {"planId":"%s","organizerId":"%s","activity":"%s","title":"Partido de pádel, falta uno",
                 "placeName":"Pistas del polideportivo de Vallecas","latitude":40.3912,"longitude":-3.6287,
                 "startsAt":"%s","freeSpots":1,"level":"INTERMEDIATE","occurredAt":"%s"}"""
                .formatted(planId, UUID.randomUUID(), activity, startsAt, Instant.now());
        var properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("__TypeId__", "es.upm.miw.oneleft.plans.domain.model.PlanPublished");
        rabbit.send(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_PUBLISHED,
                MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).andProperties(properties).build());
    }
}

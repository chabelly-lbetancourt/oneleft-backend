package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.infrastructure.realtime.PlanNearbyNotice;
import es.upm.miw.oneleft.plans.infrastructure.realtime.UserEventSubscriptions;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;

import java.time.Clock;
import java.time.Duration;

import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration with a real RabbitMQ: the event reaches, as JSON, whoever subscribes to plan.published, and the notices
 * of the notifications service reach the person's real-time stream.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RabbitPlanEventPublisherTest {

    @Autowired
    private RabbitPlanEventPublisher publisher;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private TopicExchange plansExchange;

    @MockitoSpyBean
    private UserEventSubscriptions subscriptions;

    @Test
    void publishedEventReachesTheSubscribers() {
        var queue = new AnonymousQueue();
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(plansExchange).with(RabbitConfig.PLAN_PUBLISHED));
        var event = padelPlan(ana(), Duration.ofHours(1), Clock.systemUTC()).publishedEvent();

        publisher.publish(event);

        PlanPublished received = rabbit.receiveAndConvert(queue.getName(), 5000,
                new ParameterizedTypeReference<PlanPublished>() {
                });
        assertThat(received).isEqualTo(event);
    }

    @Test
    void aReminderReachesTheStreamsOfEveryoneInThePlan() {
        var event = new PlanReminder(UUID.randomUUID(), "Padel", "Courts", Instant.parse("2026-11-16T17:20:00Z"),
                List.of(UUID.randomUUID(), UUID.randomUUID()), Instant.parse("2026-11-16T16:50:00Z"));

        publisher.publish(event);

        verify(subscriptions, timeout(10_000)).dispatch(event);
    }

    @Test
    void theNoticesOfNearbyPlansReachThePersonsStream() {
        var lucia = UUID.randomUUID();
        var planId = UUID.randomUUID();
        var json = """
                {"userId":"%s","planId":"%s","activity":"PADEL","title":"Pádel 2 contra 2, falta uno",
                 "placeName":"Pistas de la Albufera","startsAt":"2026-11-16T17:20:00Z","freeSpots":1,
                 "distanceMeters":700}""".formatted(lucia, planId);
        var properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // The class of the notifications service, which this service does not have
        properties.setHeader("__TypeId__", "es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice");

        rabbit.send(RabbitConfig.NOTIFICATIONS_EXCHANGE, RabbitConfig.NEARBY_PLAN,
                MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).andProperties(properties).build());

        verify(subscriptions, timeout(10_000)).dispatch(new PlanNearbyNotice.Message(lucia, planId, "PADEL",
                "Pádel 2 contra 2, falta uno", "Pistas de la Albufera", Instant.parse("2026-11-16T17:20:00Z"), 1, 700));
    }

    @Test
    void aCancellationReachesTheStreamsOfEveryoneInThePlan() {
        var event = new PlanCancelled(UUID.randomUUID(), "Padel", "Courts", Instant.parse("2026-11-16T17:20:00Z"),
                PlanCancelled.Reason.MINIMUM_NOT_REACHED, List.of(UUID.randomUUID(), UUID.randomUUID()),
                Instant.parse("2026-11-16T16:50:00Z"));

        publisher.publish(event);

        verify(subscriptions, timeout(10_000)).dispatch(event);
    }
}

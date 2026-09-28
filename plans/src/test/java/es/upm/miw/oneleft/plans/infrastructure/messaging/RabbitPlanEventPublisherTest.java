package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
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
 * Integración con RabbitMQ real: el evento llega en JSON a quien se suscribe a plan.published.
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
}

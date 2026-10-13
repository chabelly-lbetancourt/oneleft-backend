package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.domain.model.PlanArrival;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Output adapter: publishes plan events to the {@code oneleft.plans} RabbitMQ exchange.
 */
@Component
public class RabbitPlanEventPublisher implements PlanEventPublisher {

    private final RabbitTemplate rabbit;

    public RabbitPlanEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void publish(PlanPublished event) {
        rabbit.convertAndSend(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_PUBLISHED, event);
    }

    @Override
    public void publish(PlanJoined event) {
        rabbit.convertAndSend(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_JOINED, event);
    }

    @Override
    public void publish(PlanLeftEvent event) {
        rabbit.convertAndSend(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_LEFT, event);
    }

    @Override
    public void publish(PlanReminder event) {
        rabbit.convertAndSend(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_REMINDER, event);
    }

    @Override
    public void publish(PlanCancelled event) {
        rabbit.convertAndSend(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_CANCELLED, event);
    }

    @Override
    public void publish(PlanArrival event) {
        rabbit.convertAndSend(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_ARRIVAL, event);
    }
}

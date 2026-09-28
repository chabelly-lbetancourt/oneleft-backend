package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: publica los eventos de planes en el exchange {@code oneleft.plans} de RabbitMQ.
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
}

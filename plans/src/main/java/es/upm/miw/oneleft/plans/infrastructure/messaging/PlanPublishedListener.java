package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.infrastructure.realtime.NearbyPlanSubscriptions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: receives published plans and forwards them to the clients watching nearby plans.
 *
 * <p>The queue is anonymous (exclusive and temporary), so <b>each replica has its own</b> and receives every event:
 * it is a fan-out, not a work queue. This way a plan published through one replica also reaches the clients
 * connected to the others.
 */
@Component
public class PlanPublishedListener {

    private final NearbyPlanSubscriptions subscriptions;

    public PlanPublishedListener(NearbyPlanSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.PLANS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.PLAN_PUBLISHED))
    public void onPlanPublished(PlanPublished event) {
        subscriptions.dispatch(event);
    }
}

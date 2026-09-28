package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.infrastructure.realtime.UserEventSubscriptions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: forwards each join to the organizer's event stream. Like {@link PlanPublishedListener}, the queue is
 * anonymous, so every replica receives every event and notifies the organizer wherever they are connected.
 */
@Component
public class PlanJoinedListener {

    private final UserEventSubscriptions subscriptions;

    public PlanJoinedListener(UserEventSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.PLANS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.PLAN_JOINED))
    public void onPlanJoined(PlanJoined event) {
        subscriptions.dispatch(event);
    }
}

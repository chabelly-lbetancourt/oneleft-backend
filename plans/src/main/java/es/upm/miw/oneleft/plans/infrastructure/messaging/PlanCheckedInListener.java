package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.domain.model.PlanCheckedIn;
import es.upm.miw.oneleft.plans.infrastructure.realtime.UserEventSubscriptions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: forwards each check-in at the meeting point (HU-025) to the event stream of the rest of the group,
 * so the organizer sees who arrived. The queue is anonymous, so every replica receives it.
 */
@Component
public class PlanCheckedInListener {

    private final UserEventSubscriptions subscriptions;

    public PlanCheckedInListener(UserEventSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.PLANS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.PLAN_CHECKED_IN))
    public void onPlanCheckedIn(PlanCheckedIn event) {
        subscriptions.dispatch(event);
    }
}

package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import es.upm.miw.oneleft.plans.infrastructure.realtime.UserEventSubscriptions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: forwards each reminder (HU-007) to the event stream of everyone in the plan. The queue is
 * anonymous, so every replica receives it and reaches each person wherever they are connected.
 */
@Component
public class PlanReminderListener {

    private final UserEventSubscriptions subscriptions;

    public PlanReminderListener(UserEventSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.PLANS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.PLAN_REMINDER))
    public void onPlanReminder(PlanReminder event) {
        subscriptions.dispatch(event);
    }
}

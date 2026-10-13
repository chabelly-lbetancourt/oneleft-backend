package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.infrastructure.realtime.BadgeEarnedNotice;
import es.upm.miw.oneleft.plans.infrastructure.realtime.UserEventSubscriptions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: the reputation service tells who earned a badge (HU-044) and this service delivers the notice
 * through the person's real-time stream, the one the app already keeps open. The queue is anonymous, so every replica
 * receives every notice and delivers it wherever the person is connected.
 */
@Component
public class BadgeEarnedListener {

    private final UserEventSubscriptions subscriptions;

    public BadgeEarnedListener(UserEventSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.NOTIFICATIONS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.BADGE_EARNED))
    public void onBadgeEarned(BadgeEarnedNotice.Message notice) {
        subscriptions.dispatch(notice);
    }
}

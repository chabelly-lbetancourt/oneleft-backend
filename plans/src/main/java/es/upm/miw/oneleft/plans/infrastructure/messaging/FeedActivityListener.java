package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.infrastructure.realtime.FeedActivityNotice;
import es.upm.miw.oneleft.plans.infrastructure.realtime.UserEventSubscriptions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: the social service tells each follower that someone they follow did something new (HU-051), and this
 * service delivers it through their real-time stream, as with the badges. The queue is anonymous, so every replica
 * receives every notice.
 */
@Component
public class FeedActivityListener {

    private final UserEventSubscriptions subscriptions;

    public FeedActivityListener(UserEventSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.NOTIFICATIONS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.FEED_ACTIVITY))
    public void onFeedActivity(FeedActivityNotice.Message notice) {
        subscriptions.dispatch(notice);
    }
}

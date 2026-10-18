package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.infrastructure.realtime.KudosNotice;
import es.upm.miw.oneleft.plans.infrastructure.realtime.UserEventSubscriptions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: the reputation service tells someone that they received a kudos (HU-060), and this service delivers
 * it through their real-time stream, as with the badges. The queue is anonymous, so every replica receives every
 * notice.
 */
@Component
public class KudosListener {

    private final UserEventSubscriptions subscriptions;

    public KudosListener(UserEventSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.NOTIFICATIONS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.KUDOS))
    public void onKudos(KudosNotice.Message notice) {
        subscriptions.dispatch(notice);
    }
}

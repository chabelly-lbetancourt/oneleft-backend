package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.domain.model.JoinRequested;
import es.upm.miw.oneleft.plans.domain.model.RequestDecided;
import es.upm.miw.oneleft.plans.infrastructure.realtime.UserEventSubscriptions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: the requests for a spot (HU-069) and their decisions reach whoever is connected, on any replica. The
 * queues are anonymous, so every replica receives every event and notifies the person wherever they are connected.
 */
@Component
public class JoinRequestListener {

    private final UserEventSubscriptions subscriptions;

    public JoinRequestListener(UserEventSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.PLANS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.JOIN_REQUESTED))
    public void onJoinRequested(JoinRequested event) {
        subscriptions.dispatch(event);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.PLANS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.REQUEST_DECIDED))
    public void onRequestDecided(RequestDecided event) {
        subscriptions.dispatch(event);
    }
}

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
 * Adaptador de entrada: recibe los planes publicados y los reenvía a los clientes que miran planes cercanos.
 *
 * <p>La cola es anónima (exclusiva y temporal), así que <b>cada réplica tiene la suya</b> y recibe todos los
 * eventos: es un reparto en abanico, no un reparto de trabajo. Así, un plan publicado en una réplica llega también a
 * los clientes conectados a las demás.
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

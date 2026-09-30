package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import es.upm.miw.oneleft.notifications.domain.port.in.NotifyNearbyPlanUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: every published plan may interest people nearby.
 */
@Component
class PlanPublishedListener {

    private final NotifyNearbyPlanUseCase notifier;

    PlanPublishedListener(NotifyNearbyPlanUseCase notifier) {
        this.notifier = notifier;
    }

    @RabbitListener(queues = RabbitConfig.PLAN_PUBLISHED_QUEUE)
    void onPlanPublished(PlanPublishedMessage event) {
        notifier.notifyNearby(event.toDomain());
    }
}

package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import es.upm.miw.oneleft.notifications.domain.port.in.NotifyCancellationUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: a plan has been cancelled (HU-039), so everyone who was in it gets a system notification.
 */
@Component
class PlanCancelledListener {

    private final NotifyCancellationUseCase cancellations;

    PlanCancelledListener(NotifyCancellationUseCase cancellations) {
        this.cancellations = cancellations;
    }

    @RabbitListener(queues = RabbitConfig.PLAN_CANCELLED_QUEUE)
    void onPlanCancelled(PlanCancelledMessage event) {
        cancellations.notifyCancellation(event.toDomain());
    }
}

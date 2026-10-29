package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import es.upm.miw.oneleft.notifications.domain.port.in.NotifyCancellationUseCase;
import es.upm.miw.oneleft.notifications.domain.port.in.TrackAcceptanceUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: a plan has been cancelled (HU-039), so everyone who was in it gets a system notification and
 * nobody else is notified of it (HU-012).
 */
@Component
class PlanCancelledListener {

    private final NotifyCancellationUseCase cancellations;
    private final TrackAcceptanceUseCase tracker;

    PlanCancelledListener(NotifyCancellationUseCase cancellations, TrackAcceptanceUseCase tracker) {
        this.cancellations = cancellations;
        this.tracker = tracker;
    }

    @RabbitListener(queues = RabbitConfig.PLAN_CANCELLED_QUEUE)
    void onPlanCancelled(PlanCancelledMessage event) {
        var cancellation = event.toDomain();
        tracker.closed(cancellation.planId());
        cancellations.notifyCancellation(cancellation);
    }
}

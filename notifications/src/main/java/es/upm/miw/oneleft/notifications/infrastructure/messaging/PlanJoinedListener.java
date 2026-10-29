package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import es.upm.miw.oneleft.notifications.domain.port.in.TrackAcceptanceUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Input adapter: someone took a spot in a plan (HU-012). If they had been notified, the notice worked; and a full plan
 * needs no more batches.
 */
@Component
class PlanJoinedListener {

    private final TrackAcceptanceUseCase tracker;

    PlanJoinedListener(TrackAcceptanceUseCase tracker) {
        this.tracker = tracker;
    }

    @RabbitListener(queues = RabbitConfig.PLAN_JOINED_QUEUE)
    void onPlanJoined(PlanJoinedMessage event) {
        tracker.joined(event.planId(), event.participantId(), event.full(),
                event.occurredAt() == null ? Instant.now() : event.occurredAt());
    }
}

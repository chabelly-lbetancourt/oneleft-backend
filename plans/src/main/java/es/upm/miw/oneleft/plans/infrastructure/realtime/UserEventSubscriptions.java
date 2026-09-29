package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Personal event stream of each signed-in user, open while the app is in use: the organizer learns at once that
 * someone has joined (HU-005) or left (HU-023) their plan, and the first person of a waiting list that a spot is now
 * theirs. Push notifications for a closed app arrive with HU-006.
 */
@Component
public class UserEventSubscriptions extends SseSubscriptions<UUID> {

    static final String PLAN_JOINED = "plan-joined";
    static final String PLAN_LEFT = "plan-left";
    static final String SPOT_FREED = "plan-spot";

    public UserEventSubscriptions(MeterRegistry meterRegistry) {
        super(meterRegistry, "oneleft.plans.user.subscriptions", "Users connected to their personal event stream");
    }

    public SseEmitter subscribe(UUID userId) {
        return register(userId);
    }

    public void dispatch(PlanJoined event) {
        forEach((userId, emitter) -> {
            if (userId.equals(event.organizerId())) {
                send(emitter, SseEmitter.event().name(PLAN_JOINED).id(event.planId() + ":" + event.participantId())
                        .data(PlanJoinedNotice.of(event), MediaType.APPLICATION_JSON));
            }
        });
    }

    public void dispatch(PlanLeftEvent event) {
        var id = event.planId() + ":" + event.participantId() + ":" + event.occurredAt().toEpochMilli();
        forEach((userId, emitter) -> {
            if (userId.equals(event.organizerId())) {
                send(emitter, SseEmitter.event().name(PLAN_LEFT).id(id)
                        .data(PlanLeftNotice.of(event), MediaType.APPLICATION_JSON));
            } else if (userId.equals(event.promotedId())) {
                send(emitter, SseEmitter.event().name(SPOT_FREED).id(id)
                        .data(SpotFreedNotice.of(event), MediaType.APPLICATION_JSON));
            }
        });
    }
}

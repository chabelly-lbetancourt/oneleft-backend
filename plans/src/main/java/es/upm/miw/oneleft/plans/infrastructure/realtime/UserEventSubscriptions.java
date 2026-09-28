package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Personal event stream of each signed-in user, open while the app is in use: the organizer learns at once that
 * someone has joined their plan (HU-005). Push notifications for a closed app arrive with HU-006.
 */
@Component
public class UserEventSubscriptions extends SseSubscriptions<UUID> {

    static final String PLAN_JOINED = "plan-joined";

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
}

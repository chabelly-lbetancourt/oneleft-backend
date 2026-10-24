package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.port.out.Blocks;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Clock;

/**
 * Clients watching nearby plans (HU-004), each one with its search. When a plan is published, only the clients it
 * matches are notified.
 */
@Component
public class NearbyPlanSubscriptions extends SseSubscriptions<NearbySearch> {

    static final String PLAN_PUBLISHED = "plan-published";

    private final Clock clock;
    private final Blocks blocks;

    public NearbyPlanSubscriptions(Clock clock, Blocks blocks, MeterRegistry meterRegistry) {
        super(meterRegistry, "oneleft.plans.nearby.subscriptions",
                "Clients connected to the real-time list of nearby plans");
        this.clock = clock;
        this.blocks = blocks;
    }

    public SseEmitter subscribe(NearbySearch search) {
        return register(search);
    }

    public void dispatch(PlanPublished event) {
        var now = clock.instant();
        forEach((search, emitter) -> {
            // The same as the search: nothing of someone blocked by the requester, or who blocked them (HU-033)
            if (search.matches(event, now) && !blocks.between(search.requesterId(), event.organizerId())) {
                send(emitter, SseEmitter.event().name(PLAN_PUBLISHED).id(event.planId().toString())
                        .data(NearbyPlanEvent.of(event, search), MediaType.APPLICATION_JSON));
            }
        });
    }
}

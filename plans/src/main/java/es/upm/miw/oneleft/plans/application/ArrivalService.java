package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.ArrivalAnnounced;
import es.upm.miw.oneleft.plans.domain.model.ArrivalStatus;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.in.ArrivalUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

/**
 * «On my way» and «running late» (HU-040), saved with optimistic locking like the rest of the changes of a plan. The
 * event is published after the change is saved, so the group never hears about a status that was not kept.
 */
@Service
public class ArrivalService implements ArrivalUseCase {

    private final OptimisticPlanUpdates updates;
    private final PlanEventPublisher events;
    private final Clock clock;

    public ArrivalService(OptimisticPlanUpdates updates, PlanEventPublisher events, Clock clock) {
        this.updates = updates;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Plan announce(UUID planId, UUID userId, ArrivalStatus status, Integer minutesLate) {
        var announced = updates.update(planId, plan -> plan.announce(userId, status, minutesLate, clock),
                ArrivalAnnounced::plan);
        events.publish(announced.event());
        return announced.plan();
    }

    @Override
    public Plan clear(UUID planId, UUID userId) {
        return updates.update(planId, plan -> plan.clearArrival(userId, clock));
    }
}

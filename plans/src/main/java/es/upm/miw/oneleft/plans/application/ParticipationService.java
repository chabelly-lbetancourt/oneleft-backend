package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanLeft;
import es.upm.miw.oneleft.plans.domain.port.in.ParticipationUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

/**
 * Leaving and waiting list with the same optimistic locking as joining: if someone leaves while another person
 * joins, one of them is retried with the fresh plan and the spots never go out of sync.
 */
@Service
public class ParticipationService implements ParticipationUseCase {

    private final OptimisticPlanUpdates updates;
    private final PlanEventPublisher events;
    private final Clock clock;

    public ParticipationService(OptimisticPlanUpdates updates, PlanEventPublisher events, Clock clock) {
        this.updates = updates;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Plan leave(UUID planId, UUID userId) {
        var left = updates.update(planId, plan -> plan.leave(userId, clock), PlanLeft::plan);
        events.publish(left.event());
        return left.plan();
    }

    @Override
    public Plan joinWaitlist(UUID planId, UUID userId, String name) {
        return updates.update(planId, plan -> plan.joinWaitlist(userId, name, clock));
    }

    @Override
    public Plan leaveWaitlist(UUID planId, UUID userId) {
        return updates.update(planId, plan -> plan.leaveWaitlist(userId));
    }
}

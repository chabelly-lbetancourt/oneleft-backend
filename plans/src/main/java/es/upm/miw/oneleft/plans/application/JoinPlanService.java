package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.in.JoinPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.AvailabilityRepository;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

/**
 * Takes a spot with optimistic locking (see {@link OptimisticPlanUpdates}): when two people ask for the last spot at
 * the same time, only one gets it. The event is published after the commit, so a join that is rolled back is never
 * announced.
 */
@Service
public class JoinPlanService implements JoinPlanUseCase {

    private final OptimisticPlanUpdates updates;
    private final PlanEventPublisher events;
    private final AvailabilityRepository availabilities;
    private final Clock clock;

    public JoinPlanService(OptimisticPlanUpdates updates, PlanEventPublisher events,
                           AvailabilityRepository availabilities, Clock clock) {
        this.updates = updates;
        this.events = events;
        this.availabilities = availabilities;
        this.clock = clock;
    }

    /** Whoever joins a plan is no longer free (HU-035). */
    @Override
    public Plan join(UUID planId, UUID userId, String name) {
        var joined = updates.update(planId, plan -> plan.join(userId, name, clock));
        availabilities.delete(userId);
        events.publish(joined.joinedEvent());
        return joined;
    }
}

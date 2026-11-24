package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.CancelledPlan;
import es.upm.miw.oneleft.plans.domain.port.in.CancelPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

/**
 * The organizer cancels their plan, with the same optimistic locking as joining (see {@link OptimisticPlanUpdates}):
 * if someone joins at the same time, the cancellation is retried with the fresh plan and that person is told too. The
 * event is published after the change is saved, and only by the request that really cancelled the plan.
 */
@Service
public class PlanCancellationService implements CancelPlanUseCase {

    private final OptimisticPlanUpdates updates;
    private final PlanEventPublisher events;
    private final Clock clock;

    public PlanCancellationService(OptimisticPlanUpdates updates, PlanEventPublisher events, Clock clock) {
        this.updates = updates;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void cancel(UUID planId, UUID organizerId) {
        var cancelled = updates.update(planId, plan -> plan.cancel(organizerId, clock.instant()),
                CancelledPlan::plan);
        cancelled.event().ifPresent(events::publish);
    }
}

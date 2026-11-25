package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.CancelledPlan;
import es.upm.miw.oneleft.plans.domain.port.in.CancelPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

/**
 * The organizer cancels their plan, with the same optimistic locking as joining (see {@link OptimisticPlanUpdates}):
 * if someone joins at the same time, the cancellation is retried with the fresh plan and that person is told too. The
 * event is published after the change is saved, and only by the request that really cancelled the plan. The plans of
 * an organizer who deleted their account (HU-045) are cancelled the same way.
 */
@Service
public class PlanCancellationService implements CancelPlanUseCase {

    private final PlanRepository plans;
    private final OptimisticPlanUpdates updates;
    private final PlanEventPublisher events;
    private final Clock clock;

    public PlanCancellationService(PlanRepository plans, OptimisticPlanUpdates updates, PlanEventPublisher events,
                                   Clock clock) {
        this.plans = plans;
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

    /**
     * Each plan is cancelled in its own transaction and announced once saved. If it stops half way, the plans already
     * cancelled are no longer upcoming, so the next attempt goes on with the rest and tells nobody twice.
     */
    @Override
    public void cancelPlansOfDeletedAccount(UUID organizerId) {
        var now = clock.instant();
        for (var upcoming : plans.findByOrganizerStartingAfter(organizerId, now)) {
            var cancelled = updates.update(upcoming.id(), plan -> plan.cancelForDeletedAccount(now),
                    CancelledPlan::plan);
            cancelled.event().ifPresent(events::publish);
        }
    }
}

package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import es.upm.miw.oneleft.plans.domain.port.in.AdvancePlansUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Automatic expiry and reminders (HU-007), and the minimum of participants at its deadline (HU-039). Each due plan
 * changes in its own transaction with optimistic locking: if several replicas run at the same time, only one saves the
 * change and publishes the reminder or the cancellation; the others read the plan again, find nothing left to do and
 * stop.
 */
@Service
public class PlanLifecycleService implements AdvancePlansUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlanLifecycleService.class);

    private final PlanRepository plans;
    private final OptimisticPlanUpdates updates;
    private final PlanEventPublisher events;
    private final Clock clock;

    public PlanLifecycleService(PlanRepository plans, OptimisticPlanUpdates updates, PlanEventPublisher events,
                                Clock clock) {
        this.plans = plans;
        this.updates = updates;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public int advance() {
        var now = clock.instant();
        var changed = 0;
        for (var planId : plans.findDueForLifecycle(now)) {
            try {
                var step = updates.update(planId, plan -> step(plan, now), Step::plan);
                if (step.changed()) {
                    step.reminder().ifPresent(events::publish);
                    step.cancellation().ifPresent(events::publish);
                    changed++;
                }
            } catch (PlanNotFoundException | JoinRejectedException skipped) {
                // Deleted or busy: the next run sees it again
                log.debug("Plan {} skipped in this run: {}", planId, skipped.getMessage());
            }
        }
        if (changed > 0) {
            log.info("Lifecycle: {} plans changed", changed);
        }
        return changed;
    }

    /**
     * One step per run, in order: the minimum at its deadline (a plan is confirmed or cancelled before anyone is
     * reminded of it), then the reminder of a plan that is about to start, then the start and the end.
     */
    private static Step step(Plan plan, Instant now) {
        if (plan.needsMinimumCheck(now)) {
            var checked = plan.checkMinimum(now);
            return new Step(checked.plan(), Optional.empty(), checked.cancellation(), true);
        }
        if (plan.needsReminder(now)) {
            var reminded = plan.remind(now);
            return new Step(reminded.plan(), Optional.of(reminded.event()), Optional.empty(), true);
        }
        var advanced = plan.advance(now);
        return new Step(advanced, Optional.empty(), Optional.empty(), advanced != plan);
    }

    private record Step(Plan plan, Optional<PlanReminder> reminder, Optional<PlanCancelled> cancellation,
                        boolean changed) {
    }
}

package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.ConcurrentPlanUpdateException;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.port.in.JoinPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.util.UUID;

/**
 * Takes a spot with optimistic locking. When two people ask for the last spot at the same time, the second save
 * finds a newer version of the plan; it is retried with fresh data, where the domain sees the plan full and rejects
 * it. The event is published after the commit, so a join that is rolled back is never announced.
 */
@Service
public class JoinPlanService implements JoinPlanUseCase {

    static final int MAX_ATTEMPTS = 3;

    private final PlanRepository plans;
    private final PlanEventPublisher events;
    private final Clock clock;
    private final TransactionOperations transactions;

    public JoinPlanService(PlanRepository plans, PlanEventPublisher events, Clock clock,
                           TransactionOperations transactions) {
        this.plans = plans;
        this.events = events;
        this.clock = clock;
        this.transactions = transactions;
    }

    @Override
    public Plan join(UUID planId, UUID userId, String name) {
        for (var attempt = 1; ; attempt++) {
            try {
                var joined = transactions.execute(status -> {
                    var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
                    return plans.save(plan.join(userId, name, clock));
                });
                events.publish(joined.joinedEvent());
                return joined;
            } catch (ConcurrentPlanUpdateException conflict) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new JoinRejectedException("plan.busy", "The plan is changing right now, try again");
                }
            }
        }
    }
}

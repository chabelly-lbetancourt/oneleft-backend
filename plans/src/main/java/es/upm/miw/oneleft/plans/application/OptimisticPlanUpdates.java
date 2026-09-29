package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.ConcurrentPlanUpdateException;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Changes a plan with optimistic locking: reads it, applies the domain change and saves it in one transaction. If
 * someone else saved a newer version in the meantime, the change is retried with fresh data, where the domain sees
 * the real state (for example, that the last spot is already taken). After {@link #MAX_ATTEMPTS} the plan is busy.
 */
@Component
class OptimisticPlanUpdates {

    static final int MAX_ATTEMPTS = 3;

    private final PlanRepository plans;
    private final TransactionOperations transactions;

    OptimisticPlanUpdates(PlanRepository plans, TransactionOperations transactions) {
        this.plans = plans;
        this.transactions = transactions;
    }

    /** A change that returns the new plan. */
    Plan update(UUID planId, UnaryOperator<Plan> change) {
        return update(planId, change, Function.identity());
    }

    /**
     * A change that returns a richer result (for example, who took the spot that was freed).
     *
     * @param planOf the new plan inside the result, which is the one saved
     */
    <R> R update(UUID planId, Function<Plan, R> change, Function<R, Plan> planOf) {
        for (var attempt = 1; ; attempt++) {
            try {
                return transactions.execute(status -> {
                    var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
                    var result = change.apply(plan);
                    plans.save(planOf.apply(result));
                    return result;
                });
            } catch (ConcurrentPlanUpdateException conflict) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new JoinRejectedException("plan.busy", "The plan is changing right now, try again");
                }
            }
        }
    }
}

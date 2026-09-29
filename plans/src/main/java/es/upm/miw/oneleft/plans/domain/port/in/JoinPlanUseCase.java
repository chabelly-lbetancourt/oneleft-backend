package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.util.UUID;

/**
 * HU-005: take a free spot in a plan. If two people ask for the last spot at the same time, only one gets it.
 */
public interface JoinPlanUseCase {

    /**
     * @throws es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException if the plan does not exist
     * @throws es.upm.miw.oneleft.plans.domain.model.JoinRejectedException if the spot cannot be taken
     */
    Plan join(UUID planId, UUID userId, String name);
}

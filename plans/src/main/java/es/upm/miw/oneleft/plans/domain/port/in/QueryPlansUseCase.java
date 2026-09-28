package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.NearbyPlan;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.util.List;
import java.util.UUID;

public interface QueryPlansUseCase {

    /** @throws es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException if it does not exist */
    Plan plan(UUID planId);

    /** Organizer's plans that have not started yet, from the soonest to the latest. */
    List<Plan> upcomingPlansOrganizedBy(UUID organizerId);

    /**
     * Open plans with free spots near a position (HU-004), from the nearest to the farthest. At most
     * {@link NearbySearch#MAX_RESULTS}.
     */
    List<NearbyPlan> nearbyPlans(NearbySearch search);
}

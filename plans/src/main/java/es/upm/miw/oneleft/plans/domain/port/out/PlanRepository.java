package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {

    /**
     * @throws es.upm.miw.oneleft.plans.domain.model.ConcurrentPlanUpdateException if the stored plan changed since
     *     it was read (its version is different)
     */
    Plan save(Plan plan);

    Optional<Plan> findById(UUID planId);

    List<Plan> findByOrganizerStartingAfter(UUID organizerId, Instant from);

    /**
     * Open plans with free spots that start within the search window, sorted by distance.
     */
    List<Plan> findOpenNearby(NearbySearch search, Instant now, int limit);
}

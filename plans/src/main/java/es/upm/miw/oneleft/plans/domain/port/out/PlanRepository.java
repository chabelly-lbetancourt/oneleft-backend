package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {

    Plan save(Plan plan);

    Optional<Plan> findById(UUID planId);

    List<Plan> findByOrganizerStartingAfter(UUID organizerId, Instant from);

    /**
     * Planes abiertos, con plazas libres y que empiezan en la ventana de la búsqueda, ordenados por distancia.
     */
    List<Plan> findOpenNearby(NearbySearch search, Instant now, int limit);
}

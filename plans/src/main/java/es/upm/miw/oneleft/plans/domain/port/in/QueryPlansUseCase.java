package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.util.List;
import java.util.UUID;

public interface QueryPlansUseCase {

    /** @throws es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException si no existe */
    Plan plan(UUID planId);

    /** Planes del organizador que todavía no han empezado, del más próximo al más lejano. */
    List<Plan> upcomingPlansOrganizedBy(UUID organizerId);
}

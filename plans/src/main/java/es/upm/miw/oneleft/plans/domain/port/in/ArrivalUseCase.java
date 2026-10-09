package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.ArrivalStatus;
import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.util.UUID;

/** «On my way» and «running late» (HU-040): only the group of a plan, until it starts. */
public interface ArrivalUseCase {

    Plan announce(UUID planId, UUID userId, ArrivalStatus status, Integer minutesLate);

    Plan clear(UUID planId, UUID userId);
}

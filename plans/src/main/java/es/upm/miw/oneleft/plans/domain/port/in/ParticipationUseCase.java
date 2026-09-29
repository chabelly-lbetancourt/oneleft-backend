package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.util.UUID;

/**
 * HU-023: leave a plan and wait for a spot of a full plan. When someone leaves, the first person of the waiting
 * list takes the spot and both they and the organizer are told.
 *
 * @see es.upm.miw.oneleft.plans.domain.model.JoinRejectedException for the rejections of each operation
 */
public interface ParticipationUseCase {

    Plan leave(UUID planId, UUID userId);

    Plan joinWaitlist(UUID planId, UUID userId, String name);

    Plan leaveWaitlist(UUID planId, UUID userId);
}

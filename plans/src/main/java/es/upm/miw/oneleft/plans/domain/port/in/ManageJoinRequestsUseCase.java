package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.RequestDecision;

import java.util.UUID;

/**
 * Plans whose organizer chooses who comes (HU-069): people ask for a spot, the organizer accepts or rejects and each
 * person can withdraw their own request.
 */
public interface ManageJoinRequestsUseCase {

    Plan request(UUID planId, UUID userId, String name, String message);

    /** The plan with its requests, for its organizer only. */
    Plan requestsOf(UUID planId, UUID organizerId);

    RequestDecision accept(UUID planId, UUID organizerId, UUID requesterId);

    RequestDecision reject(UUID planId, UUID organizerId, UUID requesterId);

    Plan withdraw(UUID planId, UUID userId);
}

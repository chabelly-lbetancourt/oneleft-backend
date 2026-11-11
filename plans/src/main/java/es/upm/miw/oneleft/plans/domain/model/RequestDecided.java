package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Domain event: the organizer decided on a request, or the plan filled up first (HU-069); the requester is told. */
public record RequestDecided(UUID planId, String title, UUID requesterId, RequestStatus decision, boolean planFull,
                             Instant occurredAt) {
}

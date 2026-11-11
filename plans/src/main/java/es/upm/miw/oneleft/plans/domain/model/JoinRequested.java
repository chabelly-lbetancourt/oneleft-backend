package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Domain event: someone asked for a spot in a plan (HU-069); the organizer sees it in real time. */
public record JoinRequested(UUID planId, UUID organizerId, String title, UUID requesterId, String requesterName,
                            Instant occurredAt) {
}

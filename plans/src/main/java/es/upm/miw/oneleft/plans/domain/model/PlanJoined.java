package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: someone has taken a spot in a plan. The organizer is notified, and when {@code full} is true the
 * plan disappears from the nearby searches.
 */
public record PlanJoined(UUID planId, UUID organizerId, String title, UUID participantId, String participantName,
                         int freeSpots, boolean full, Instant occurredAt) {
}

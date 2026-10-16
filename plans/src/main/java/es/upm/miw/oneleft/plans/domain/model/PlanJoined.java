package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: someone has taken a spot in a plan. The organizer is notified, and when {@code full} is true the
 * plan disappears from the nearby searches. The activity and the start go to the feed of their followers (HU-051).
 */
public record PlanJoined(UUID planId, UUID organizerId, String title, Activity activity, Instant startsAt,
                         UUID participantId, String participantName, int freeSpots, boolean full, Instant occurredAt) {
}

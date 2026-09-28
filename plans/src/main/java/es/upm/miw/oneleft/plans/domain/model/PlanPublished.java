package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: a plan has been published. The real-time nearby search and the notifications service consume it
 * to let people nearby know.
 */
public record PlanPublished(UUID planId, UUID organizerId, Activity activity, double latitude, double longitude,
                            Instant startsAt, int freeSpots, Level level, Instant occurredAt) {
}

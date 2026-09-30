package es.upm.miw.oneleft.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A plan that has just been published, as the plans service announces it (event {@code plan.published}).
 */
public record PublishedPlan(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                            double latitude, double longitude, Instant startsAt, int freeSpots) {
}

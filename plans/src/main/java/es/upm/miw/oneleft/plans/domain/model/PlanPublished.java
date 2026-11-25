package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Domain event: a plan has been published. The real-time nearby search and the notifications service consume it
 * to let people nearby know. It carries for whom the plan is (HU-070), so the real-time search can leave it out for
 * who does not fit; events from before that have none and are for everyone. The same goes for what makes its place
 * accessible (HU-046): events from before that have none, as a place nobody said anything about.
 */
public record PlanPublished(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                            double latitude, double longitude,
                            Instant startsAt, int freeSpots, Level level, Instant occurredAt, Audience audience,
                            Set<AccessibilityFeature> accessibility) {

    public PlanPublished {
        audience = audience == null ? Audience.EVERYONE : audience;
        accessibility = accessibility == null ? Set.of() : Set.copyOf(accessibility);
    }

    /** A plan in a place nobody said anything about its accessibility. */
    @SuppressWarnings("java:S107")
    public PlanPublished(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                         double latitude, double longitude, Instant startsAt, int freeSpots, Level level,
                         Instant occurredAt, Audience audience) {
        this(planId, organizerId, activity, title, placeName, latitude, longitude, startsAt, freeSpots, level,
                occurredAt, audience, Set.of());
    }

    /** A plan for everyone. */
    public PlanPublished(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                         double latitude, double longitude, Instant startsAt, int freeSpots, Level level,
                         Instant occurredAt) {
        this(planId, organizerId, activity, title, placeName, latitude, longitude, startsAt, freeSpots, level,
                occurredAt, Audience.EVERYONE, Set.of());
    }
}

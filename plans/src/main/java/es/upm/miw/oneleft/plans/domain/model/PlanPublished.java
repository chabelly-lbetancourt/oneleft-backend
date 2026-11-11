package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: a plan has been published. The real-time nearby search and the notifications service consume it
 * to let people nearby know. It carries for whom the plan is (HU-070), so the real-time search can leave it out for
 * who does not fit; events from before that have none and are for everyone.
 */
public record PlanPublished(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                            double latitude, double longitude,
                            Instant startsAt, int freeSpots, Level level, Instant occurredAt, Audience audience) {

    public PlanPublished {
        audience = audience == null ? Audience.EVERYONE : audience;
    }

    /** A plan for everyone. */
    public PlanPublished(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                         double latitude, double longitude, Instant startsAt, int freeSpots, Level level,
                         Instant occurredAt) {
        this(planId, organizerId, activity, title, placeName, latitude, longitude, startsAt, freeSpots, level,
                occurredAt, Audience.EVERYONE);
    }
}

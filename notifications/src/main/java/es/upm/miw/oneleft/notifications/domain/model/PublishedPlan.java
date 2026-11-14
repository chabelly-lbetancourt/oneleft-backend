package es.upm.miw.oneleft.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A plan that has just been published, as the plans service announces it (event {@code plan.published}). The level is
 * {@code null} for plans open to any level; the audience (HU-070) is everyone if the event does not say.
 */
public record PublishedPlan(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                            double latitude, double longitude, Instant startsAt, int freeSpots, Level level,
                            Audience audience) {

    public PublishedPlan {
        audience = audience == null ? Audience.EVERYONE : audience;
    }

    public PublishedPlan(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                         double latitude, double longitude, Instant startsAt, int freeSpots, Level level) {
        this(planId, organizerId, activity, title, placeName, latitude, longitude, startsAt, freeSpots, level, null);
    }

    public PublishedPlan(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                         double latitude, double longitude, Instant startsAt, int freeSpots) {
        this(planId, organizerId, activity, title, placeName, latitude, longitude, startsAt, freeSpots, null, null);
    }
}

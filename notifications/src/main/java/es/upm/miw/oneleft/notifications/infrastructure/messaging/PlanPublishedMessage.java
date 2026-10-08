package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.Level;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;

import java.time.Instant;
import java.util.UUID;

/** The {@code plan.published} event as the plans service sends it; only the fields notices need are read. */
@JsonIgnoreProperties(ignoreUnknown = true)
record PlanPublishedMessage(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                            double latitude, double longitude, Instant startsAt, int freeSpots, Level level) {

    PublishedPlan toDomain() {
        return new PublishedPlan(planId, organizerId, activity, title, placeName, latitude, longitude, startsAt,
                freeSpots, level);
    }
}

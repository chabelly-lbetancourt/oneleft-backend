package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.Audience;
import es.upm.miw.oneleft.notifications.domain.model.AudienceGender;
import es.upm.miw.oneleft.notifications.domain.model.Level;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;

import java.time.Instant;
import java.util.UUID;

/** The {@code plan.published} event as the plans service sends it; only the fields notices need are read. */
@JsonIgnoreProperties(ignoreUnknown = true)
record PlanPublishedMessage(UUID planId, UUID organizerId, Activity activity, String title, String placeName,
                            double latitude, double longitude, Instant startsAt, int freeSpots, Level level,
                            AudienceMessage audience) {

    PublishedPlan toDomain() {
        return new PublishedPlan(planId, organizerId, activity, title, placeName, latitude, longitude, startsAt,
                freeSpots, level, audience == null ? null : audience.toDomain());
    }

    /** For whom the plan is (HU-070). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AudienceMessage(AudienceGender gender, Integer minAge, Integer maxAge) {

        Audience toDomain() {
            return new Audience(gender, minAge, maxAge);
        }
    }
}

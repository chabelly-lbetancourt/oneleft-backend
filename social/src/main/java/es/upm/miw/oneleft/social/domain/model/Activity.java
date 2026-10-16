package es.upm.miw.oneleft.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Something someone did in a plan (HU-051), from the events of the plans service.
 *
 * @param planActivity the activity of the plan (padel, running…), as the plans service names it
 */
public record Activity(UUID actorId, ActivityType type, UUID planId, String planTitle, String planActivity,
                       Instant startsAt, Instant occurredAt) {

    public static final int MAX_TITLE_LENGTH = 80;

    public Activity {
        if (actorId == null || type == null || planId == null || planActivity == null || startsAt == null
                || occurredAt == null) {
            throw new ValidationException("activity.missingData", "Required activity data is missing");
        }
        planTitle = planTitle == null || planTitle.isBlank() ? "" : planTitle.strip();
        if (planTitle.length() > MAX_TITLE_LENGTH) {
            planTitle = planTitle.substring(0, MAX_TITLE_LENGTH).strip();
        }
    }
}

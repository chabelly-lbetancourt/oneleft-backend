package es.upm.miw.oneleft.plans.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Someone of the group of a plan arrived at the meeting point (HU-025). It is kept as their attendance, for the
 * reputation (HU-009) and to reduce no-shows; where they were when they checked in is not.
 */
public record CheckIn(UUID planId, UUID userId, String name, Instant at) {

    /** The check-in opens this long before the start... */
    public static final Duration OPENS_BEFORE = Duration.ofMinutes(15);
    /** ...and closes this long after it. */
    public static final Duration CLOSES_AFTER = Duration.ofMinutes(30);
    /** Radius around the meeting point within which the device must be. */
    public static final int GEOFENCE_METERS = 150;

    public CheckIn {
        if (planId == null || userId == null || name == null || at == null) {
            throw new ValidationException("checkIn.missingData", "Required check-in data is missing");
        }
    }
}

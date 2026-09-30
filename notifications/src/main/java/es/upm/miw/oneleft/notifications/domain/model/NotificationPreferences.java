package es.upm.miw.oneleft.notifications.domain.model;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * What each person wants to hear about (HU-006). Notices are opt-in: nobody gets them until they turn them on.
 *
 * @param latitude       approximate zone (rounded to 2 decimals, about 1 km, like the profile zone)
 * @param radiusMeters   how far from the zone a plan may be
 * @param activities     activities of interest; empty means all of them
 * @param quietHours     hours without notices, or {@code null} for none
 * @param maxPerDay      maximum notices per local day, so that the app never becomes a nuisance
 */
public record NotificationPreferences(UUID userId, boolean enabled, Double latitude, Double longitude, int radiusMeters,
                                      Set<Activity> activities, QuietHours quietHours, int maxPerDay) {

    public static final int MIN_RADIUS = 500;
    public static final int MAX_RADIUS = 20_000;
    public static final int MAX_PER_DAY = 20;
    static final int DEFAULT_RADIUS = 3_000;
    static final int DEFAULT_PER_DAY = 5;
    static final QuietHours DEFAULT_QUIET = new QuietHours(LocalTime.of(23, 0), LocalTime.of(8, 0));

    public NotificationPreferences {
        activities = activities == null || activities.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(activities));
        if (enabled && (latitude == null || longitude == null)) {
            throw new ValidationException("notifications.zoneRequired", "Notices need a zone to look for plans around");
        }
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude == null || longitude < -180 || longitude > 180)) {
            throw new ValidationException("notifications.zone", "Invalid zone");
        }
        if (radiusMeters < MIN_RADIUS || radiusMeters > MAX_RADIUS) {
            throw new ValidationException("notifications.radius", "Radius between %d and %d metres".formatted(MIN_RADIUS, MAX_RADIUS));
        }
        if (maxPerDay < 1 || maxPerDay > MAX_PER_DAY) {
            throw new ValidationException("notifications.maxPerDay", "Between 1 and %d notices a day".formatted(MAX_PER_DAY));
        }
        latitude = round(latitude);
        longitude = round(longitude);
    }

    /** Until the person chooses: notices off, 3 km, every activity, silent at night and at most 5 a day. */
    public static NotificationPreferences defaults(UUID userId) {
        return new NotificationPreferences(userId, false, null, null, DEFAULT_RADIUS, Set.of(), DEFAULT_QUIET,
                DEFAULT_PER_DAY);
    }

    /** Whether the plan is one this person wants to hear about (the time of day is checked apart). */
    public boolean wants(PublishedPlan plan) {
        return enabled && !plan.organizerId().equals(userId)
                && (activities.isEmpty() || activities.contains(plan.activity()))
                && distanceTo(plan) <= radiusMeters;
    }

    public boolean isQuietAt(LocalTime time) {
        return quietHours != null && quietHours.includes(time);
    }

    public double distanceTo(PublishedPlan plan) {
        return GeoDistance.meters(latitude, longitude, plan.latitude(), plan.longitude());
    }

    /** The zone is kept approximate: no exact home address is ever stored. */
    private static Double round(Double coordinate) {
        return coordinate == null ? null : Math.round(coordinate * 100) / 100.0;
    }
}

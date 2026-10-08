package es.upm.miw.oneleft.notifications.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * A saved search (HU-036): «padel, intermediate, less than 2 km, weekday afternoons». When a plan like that is
 * published, its owner is told through the notices of HU-006.
 *
 * @param activities   activities of the search; empty means any
 * @param level        level of the plans; {@code null} means any. Plans open to any level always match
 * @param latitude     centre of the search, rounded to 2 decimals (about 1 km) like the zone of the notices
 * @param radiusMeters how far from the centre, like the nearby search (HU-004)
 * @param days         days of the week of the start; empty means any day
 * @param from         from this time of the day (inclusive), or {@code null} together with {@code to} for any time
 * @param to           until this time of the day (exclusive)
 */
public record SavedAlert(UUID id, UUID userId, String name, Set<Activity> activities, Level level, double latitude,
                         double longitude, int radiusMeters, Set<DayOfWeek> days, LocalTime from, LocalTime to) {

    public static final int MAX_NAME = 40;
    public static final int MIN_RADIUS = NotificationPreferences.MIN_RADIUS;
    public static final int MAX_RADIUS = NotificationPreferences.MAX_RADIUS;

    public SavedAlert {
        if (id == null || userId == null) {
            throw new ValidationException("alerts.missingData", "Required alert data is missing");
        }
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME) {
            throw new ValidationException("alerts.name", "The name of the alert has 1 to %d characters"
                    .formatted(MAX_NAME));
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("alerts.zone", "Invalid zone");
        }
        if (radiusMeters < MIN_RADIUS || radiusMeters > MAX_RADIUS) {
            throw new ValidationException("alerts.radius", "Radius between %d and %d metres"
                    .formatted(MIN_RADIUS, MAX_RADIUS));
        }
        if ((from == null) != (to == null) || from != null && !from.isBefore(to)) {
            throw new ValidationException("alerts.hours", "The hours of an alert go from an earlier to a later time "
                    + "of the same day");
        }
        name = name.strip();
        activities = activities == null || activities.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(activities));
        days = days == null || days.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(days));
        latitude = Math.round(latitude * 100) / 100.0;
        longitude = Math.round(longitude * 100) / 100.0;
    }

    /** Same alert with the data of an edit; the id and the owner stay. */
    public SavedAlert edit(SavedAlert changes) {
        return new SavedAlert(id, userId, changes.name(), changes.activities(), changes.level(), changes.latitude(),
                changes.longitude(), changes.radiusMeters(), changes.days(), changes.from(), changes.to());
    }

    /** Whether a newly published plan is one the alert looks for; its start is read in the local time zone. */
    public boolean matches(PublishedPlan plan, ZoneId zone) {
        if (plan.organizerId().equals(userId) || !activities.isEmpty() && !activities.contains(plan.activity())
                || level != null && plan.level() != null && level != plan.level()
                || GeoDistance.meters(latitude, longitude, plan.latitude(), plan.longitude()) > radiusMeters) {
            return false;
        }
        var start = plan.startsAt().atZone(zone);
        if (!days.isEmpty() && !days.contains(start.getDayOfWeek())) {
            return false;
        }
        var time = start.toLocalTime();
        return from == null || !time.isBefore(from) && time.isBefore(to);
    }
}

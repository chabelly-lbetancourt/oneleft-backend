package es.upm.miw.oneleft.plans.domain.model;

import java.util.Set;

/**
 * Someone free near a plan, as its organizer sees them (HU-035): no name and no place, only the distance rounded to
 * 500 m from their approximate zone, their level in the activity of the plan and what else they would do.
 */
public record FreePerson(long distanceMeters, Level level, Set<Activity> activities) {

    /** Distances are given in steps of half a kilometre. */
    public static final int DISTANCE_STEP = 500;

    public static FreePerson near(Availability availability, Plan plan) {
        var point = plan.meetingPoint();
        var meters = GeoDistance.meters(availability.latitude(), availability.longitude(), point.latitude(),
                point.longitude());
        var rounded = Math.max(DISTANCE_STEP, Math.round(meters / DISTANCE_STEP) * DISTANCE_STEP);
        var activities = availability.interests().stream().map(Interest::activity)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new FreePerson(rounded, availability.levelIn(plan.activity()), activities);
    }
}

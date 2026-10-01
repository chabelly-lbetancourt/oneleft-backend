package es.upm.miw.oneleft.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Notice for one person: a plan that fits their preferences has been published near their zone.
 *
 * @param distanceMeters from the person's approximate zone to the meeting point, rounded to 100 m
 */
public record NearbyPlanNotice(UUID userId, UUID planId, Activity activity, String title, String placeName,
                               Instant startsAt, int freeSpots, long distanceMeters) {

    public static NearbyPlanNotice of(UUID userId, PublishedPlan plan, double distanceMeters) {
        return new NearbyPlanNotice(userId, plan.planId(), plan.activity(), plan.title(), plan.placeName(),
                plan.startsAt(), plan.freeSpots(), Math.round(distanceMeters / 100) * 100);
    }
}

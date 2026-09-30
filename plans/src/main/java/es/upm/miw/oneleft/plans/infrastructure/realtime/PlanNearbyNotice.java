package es.upm.miw.oneleft.plans.infrastructure.realtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * What the app receives when a plan that fits the person's preferences is published nearby (HU-006).
 *
 * @param distanceMeters from the person's approximate zone, rounded to 100 m
 */
public record PlanNearbyNotice(UUID planId, String activity, String title, String placeName, Instant startsAt,
                               int freeSpots, long distanceMeters) {

    public static PlanNearbyNotice of(Message message) {
        return new PlanNearbyNotice(message.planId(), message.activity(), message.title(), message.placeName(),
                message.startsAt(), message.freeSpots(), message.distanceMeters());
    }

    /** The notice as the notifications service sends it, with the person it is for. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(UUID userId, UUID planId, String activity, String title, String placeName, Instant startsAt,
                          int freeSpots, long distanceMeters) {
    }
}

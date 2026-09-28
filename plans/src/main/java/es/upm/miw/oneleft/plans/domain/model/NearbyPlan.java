package es.upm.miw.oneleft.plans.domain.model;

/** Plan found by a nearby search, with the distance from the requester's position. */
public record NearbyPlan(Plan plan, double distanceMeters) {
}

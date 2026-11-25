package es.upm.miw.oneleft.plans.domain.model;

/**
 * Plan found by a nearby search, with the distance from the requester's position.
 *
 * @param compatibility how well it fits the requester (HU-031); {@code null} if their profile is not known
 */
public record NearbyPlan(Plan plan, double distanceMeters, Compatibility compatibility) {

    public NearbyPlan(Plan plan, double distanceMeters) {
        this(plan, distanceMeters, null);
    }
}

package es.upm.miw.oneleft.plans.domain.model;

/** The order of the nearby plans. */
public enum NearbySort {
    /** From the nearest to the farthest (HU-004). */
    DISTANCE,
    /** From the most compatible with the person to the least (HU-031) and, among equals, from the nearest. */
    COMPATIBILITY
}

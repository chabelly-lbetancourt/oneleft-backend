package es.upm.miw.oneleft.plans.domain.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * What makes the place of a plan accessible (HU-046), as its organizer declares it: a short closed list about the
 * place, never about the people who come.
 */
public enum AccessibilityFeature {
    /** Someone in a wheelchair can get in and move around the place. */
    WHEELCHAIR_ACCESSIBLE,
    /** No steps on the way in: level, with a ramp or with a lift. */
    STEP_FREE_ACCESS,
    ACCESSIBLE_TOILET,
    ACCESSIBLE_PARKING;

    public static final String CODE = "plan.accessibility";

    /**
     * The features with these names, without repeats and in the order of the list. A name that is not in the list is
     * rejected, so that nothing else can be stored.
     */
    public static Set<AccessibilityFeature> of(Collection<String> names) {
        var features = EnumSet.noneOf(AccessibilityFeature.class);
        if (names != null) {
            names.forEach(name -> features.add(named(name)));
        }
        return Collections.unmodifiableSet(features);
    }

    private static AccessibilityFeature named(String name) {
        for (var feature : values()) {
            if (feature.name().equals(name)) {
                return feature;
            }
        }
        throw new ValidationException(CODE, "Unknown accessibility feature: one of " + EnumSet.allOf(
                AccessibilityFeature.class) + " is expected");
    }
}

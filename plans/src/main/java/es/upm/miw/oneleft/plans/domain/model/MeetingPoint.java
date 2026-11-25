package es.upm.miw.oneleft.plans.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Where the plan meets (a court, a bar, a park gate...). Unlike the user's zone, it is a public place and is
 * stored precisely so that nearby plans can be found.
 *
 * @param accessibility what makes the place accessible, as the organizer declares it (HU-046); empty if they said
 *                      nothing
 */
public record MeetingPoint(String name, double latitude, double longitude, Set<AccessibilityFeature> accessibility) {

    public static final int MAX_NAME_LENGTH = 100;

    /** A place nobody has said anything about its accessibility. */
    public MeetingPoint(String name, double latitude, double longitude) {
        this(name, latitude, longitude, Set.of());
    }

    public MeetingPoint {
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH) {
            throw new ValidationException("meetingPoint.name",
                    "The meeting point needs a name of up to " + MAX_NAME_LENGTH + " characters");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("coordinates.outOfRange", "Meeting point coordinates out of range");
        }
        name = name.strip();
        // Always in the order of the list, whatever the order they came in
        accessibility = accessibility == null || accessibility.isEmpty() ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(accessibility));
    }
}

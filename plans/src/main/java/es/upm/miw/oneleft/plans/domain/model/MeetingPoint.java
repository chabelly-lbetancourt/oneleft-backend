package es.upm.miw.oneleft.plans.domain.model;

/**
 * Where the plan meets (a court, a bar, a park gate...). Unlike the user's zone, it is a public place and is
 * stored precisely so that nearby plans can be found.
 */
public record MeetingPoint(String name, double latitude, double longitude) {

    public static final int MAX_NAME_LENGTH = 100;

    public MeetingPoint {
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH) {
            throw new ValidationException("meetingPoint.name",
                    "The meeting point needs a name of up to " + MAX_NAME_LENGTH + " characters");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("coordinates.outOfRange", "Meeting point coordinates out of range");
        }
        name = name.strip();
    }
}

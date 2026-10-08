package es.upm.miw.oneleft.plans.domain.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * «I'm free now» (HU-035): someone is available for a plan around an approximate zone until a time, for some
 * activities (none means any). Organizers of nearby plans see how many people are free, never who or where exactly.
 *
 * @param latitude rounded to 2 decimals (about 1 km), like the zone of the profile
 */
public record Availability(UUID userId, double latitude, double longitude, Instant until, Set<Interest> interests) {

    /** Free mode lasts 1 to 3 hours: OneLeft is for plans right now. */
    public static final int MIN_HOURS = 1;
    public static final int MAX_HOURS = 3;

    public Availability {
        if (userId == null || until == null) {
            throw new ValidationException("availability.missingData", "Required availability data is missing");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("availability.zone", "Invalid zone");
        }
        latitude = Math.round(latitude * 100) / 100.0;
        longitude = Math.round(longitude * 100) / 100.0;
        interests = interests == null ? Set.of() : Set.copyOf(interests);
    }

    /** Free from now for {@code hours}. */
    public static Availability start(UUID userId, double latitude, double longitude, int hours,
                                     Set<Interest> interests, Clock clock) {
        if (hours < MIN_HOURS || hours > MAX_HOURS) {
            throw new ValidationException("availability.hours", "Free mode lasts between %d and %d hours"
                    .formatted(MIN_HOURS, MAX_HOURS));
        }
        return new Availability(userId, latitude, longitude, clock.instant().plus(Duration.ofHours(hours)),
                interests);
    }

    public boolean activeAt(Instant now) {
        return until.isAfter(now);
    }

    /** Whether this person would do the activity (no interests means any). */
    public boolean wants(Activity activity) {
        return interests.isEmpty() || interests.stream().anyMatch(interest -> interest.activity() == activity);
    }

    /** Level in the activity, if they said it. */
    public Level levelIn(Activity activity) {
        return interests.stream().filter(interest -> interest.activity() == activity).map(Interest::level)
                .findFirst().orElse(null);
    }
}

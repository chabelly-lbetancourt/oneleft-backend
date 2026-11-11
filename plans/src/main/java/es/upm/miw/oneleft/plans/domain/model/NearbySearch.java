package es.upm.miw.oneleft.plans.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Search for open plans near a position (HU-004): within a radius, of some activities and starting soon. The
 * requester does not see their own plans.
 *
 * @param activities activities of interest; empty means all of them
 * @param requesterId who is searching, or {@code null} if unknown
 * @param viewerGender gender of who searches, or {@code null} if unknown: without it only the plans for everyone
 * @param viewerAge age of who searches, or {@code null} if unknown: without it only the plans with no age range
 */
public record NearbySearch(double latitude, double longitude, int radiusMeters, Set<Activity> activities,
                           Duration startsWithin, UUID requesterId, String viewerGender, Integer viewerAge) {

    /** A search that does not know who the viewer is: only the plans for everyone (HU-070). */
    public NearbySearch(double latitude, double longitude, int radiusMeters, Set<Activity> activities,
                        Duration startsWithin, UUID requesterId) {
        this(latitude, longitude, radiusMeters, activities, startsWithin, requesterId, null, null);
    }

    /** The same search for a person with this gender and age: the plans that are not for them are left out (HU-070). */
    public NearbySearch forViewer(AudienceMember viewer) {
        return new NearbySearch(latitude, longitude, radiusMeters, activities, startsWithin, requesterId,
                viewer.gender().name(), viewer.age());
    }

    public static final int MIN_RADIUS_METERS = 500;
    public static final int MAX_RADIUS_METERS = 25_000;
    public static final int DEFAULT_RADIUS_METERS = 5_000;
    public static final Duration MIN_STARTS_WITHIN = Duration.ofHours(1);
    /** Maximum number of results: enough for a phone screen and a readable map. */
    public static final int MAX_RESULTS = 50;

    public NearbySearch {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("coordinates.outOfRange", "Search coordinates out of range");
        }
        if (radiusMeters < MIN_RADIUS_METERS || radiusMeters > MAX_RADIUS_METERS) {
            throw new ValidationException("search.radius", "The radius must be between " + MIN_RADIUS_METERS
                    + " m and " + MAX_RADIUS_METERS / 1000 + " km");
        }
        if (startsWithin == null) {
            startsWithin = Plan.MAX_HORIZON;
        }
        if (startsWithin.compareTo(MIN_STARTS_WITHIN) < 0 || startsWithin.compareTo(Plan.MAX_HORIZON) > 0) {
            throw new ValidationException("search.startsWithin", "Only plans starting in the next "
                    + MIN_STARTS_WITHIN.toHours() + " to " + Plan.MAX_HORIZON.toHours() + " hours can be searched");
        }
        activities = activities == null ? Set.of() : Set.copyOf(activities);
    }

    public double distanceTo(double otherLatitude, double otherLongitude) {
        return GeoDistance.meters(latitude, longitude, otherLatitude, otherLongitude);
    }

    public boolean includes(Activity activity) {
        return activities.isEmpty() || activities.contains(activity);
    }

    /** End of the search window: plans must start between {@code now} and this instant. */
    public Instant until(Instant now) {
        return now.plus(startsWithin);
    }

    /**
     * Decides whether a newly published plan belongs to this search. It applies the same criteria as the database
     * query, so that the real-time list and a reload agree.
     */
    public boolean matches(PlanPublished event, Instant now) {
        return includes(event.activity())
                && !event.organizerId().equals(requesterId)
                && event.freeSpots() > 0
                && event.startsAt().isAfter(now)
                && !event.startsAt().isAfter(until(now))
                && distanceTo(event.latitude(), event.longitude()) <= radiusMeters
                && isForViewer(event.audience());
    }

    /**
     * Whether the plan is for who searches (HU-070), as the database query decides: a plan for everyone always is;
     * otherwise the gender must match and, for an age limit, the age must be known and inside it. Who is unknown
     * only sees the plans for everyone.
     */
    boolean isForViewer(Audience audience) {
        if (audience.isEveryone()) {
            return true;
        }
        var genderFits = audience.gender() == AudienceGender.ALL || audience.gender().name().equals(viewerGender);
        var minFits = audience.minAge() == null || viewerAge != null && viewerAge >= audience.minAge();
        var maxFits = audience.maxAge() == null || viewerAge != null && viewerAge <= audience.maxAge();
        return genderFits && minFits && maxFits;
    }
}

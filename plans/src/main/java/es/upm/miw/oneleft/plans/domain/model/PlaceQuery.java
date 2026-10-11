package es.upm.miw.oneleft.plans.domain.model;

/**
 * What the organizer is typing as the meeting point and where the device is (HU-071). The position comes rounded to
 * about 1 km from the device and only puts the closest places first.
 */
public record PlaceQuery(String text, double latitude, double longitude) {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 100;

    public PlaceQuery {
        text = text == null ? "" : text.strip();
        if (text.length() < MIN_LENGTH || text.length() > MAX_LENGTH) {
            throw new ValidationException("place.query", "The place to search must have between " + MIN_LENGTH
                    + " and " + MAX_LENGTH + " characters");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("coordinates.outOfRange", "Search coordinates out of range");
        }
    }

    public int distanceTo(Place place) {
        return (int) Math.round(GeoDistance.meters(latitude, longitude, place.latitude(), place.longitude()));
    }
}

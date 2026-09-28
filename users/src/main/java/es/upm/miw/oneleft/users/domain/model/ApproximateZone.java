package es.upm.miw.oneleft.users.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The user's usual area. For privacy, OneLeft never stores the exact location:
 * coordinates are rounded to two decimals (a grid of roughly 1.1 km).
 */
public record ApproximateZone(String name, double latitude, double longitude) {

    public static final int DECIMALS = 2;
    public static final int MAX_NAME_LENGTH = 60;

    public ApproximateZone {
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH) {
            throw new ValidationException("zone.name",
                    "The zone needs a name of up to " + MAX_NAME_LENGTH + " characters");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("coordinates.outOfRange", "Coordinates out of range");
        }
        name = name.strip();
        latitude = round(latitude);
        longitude = round(longitude);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).doubleValue();
    }
}

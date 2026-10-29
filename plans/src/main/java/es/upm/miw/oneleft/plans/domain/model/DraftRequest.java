package es.upm.miw.oneleft.plans.domain.model;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * The sentence a person wrote or dictated to create a plan (HU-010), where they are (to find the meeting point near
 * them) and their time zone (to resolve «tomorrow at 7»). The sentence is never stored.
 */
public record DraftRequest(String text, double latitude, double longitude, ZoneId zone) {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 300;
    /** Used when the browser sends no valid time zone. */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Madrid");

    public DraftRequest {
        text = text == null ? "" : text.strip();
        if (text.length() < MIN_LENGTH || text.length() > MAX_LENGTH) {
            throw new ValidationException("draft.text", "The plan must be described in " + MIN_LENGTH + " to "
                    + MAX_LENGTH + " characters");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("coordinates.outOfRange", "Search coordinates out of range");
        }
        zone = zone == null ? DEFAULT_ZONE : zone;
    }

    /** The request with the zone as the browser names it, or the default one if it is not a valid one. */
    public static DraftRequest of(String text, double latitude, double longitude, String zoneId) {
        try {
            return new DraftRequest(text, latitude, longitude, zoneId == null || zoneId.isBlank() ? null
                    : ZoneId.of(zoneId));
        } catch (DateTimeException e) {
            return new DraftRequest(text, latitude, longitude, null);
        }
    }
}

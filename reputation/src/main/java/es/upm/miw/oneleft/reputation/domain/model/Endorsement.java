package es.upm.miw.oneleft.reputation.domain.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Someone vouches for the level of someone else in an activity (HU-061): one per endorser, receiver and activity.
 *
 * @param activity the code of the activity, as the plans have it
 * @param level    the level the endorser vouches for: it only backs that level, not another one declared later
 * @param givenAt  when it was given, to the second
 */
public record Endorsement(UUID endorserId, UUID receiverId, String activity, SkillLevel level, Instant givenAt) {

    public static final int MAX_ACTIVITY_LENGTH = 30;

    public Endorsement {
        if (endorserId == null || receiverId == null || activity == null || activity.isBlank() || level == null
                || givenAt == null) {
            throw new ValidationException("endorsement.missingData", "Required endorsement data is missing");
        }
        if (activity.length() > MAX_ACTIVITY_LENGTH) {
            throw new ValidationException("endorsement.activity", "Not an activity");
        }
        givenAt = givenAt.truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * The same for the same endorser, receiver and activity: the source of its entry in the ledger, so that an
     * endorsement counts once however many times it arrives.
     */
    public UUID id() {
        return UUID.nameUUIDFromBytes(("endorsement:" + endorserId + ":" + receiverId + ":" + activity)
                .getBytes(StandardCharsets.UTF_8));
    }
}

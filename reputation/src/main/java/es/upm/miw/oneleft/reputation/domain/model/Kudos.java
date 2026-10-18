package es.upm.miw.oneleft.reputation.domain.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * A kudos that someone gives to someone else of the same plan (HU-060): one per person and plan.
 *
 * @param givenAt when it was given, to the second
 */
public record Kudos(UUID giverId, UUID receiverId, UUID planId, KudosKind kind, Instant givenAt) {

    public Kudos {
        if (giverId == null || receiverId == null || planId == null || kind == null || givenAt == null) {
            throw new ValidationException("kudos.missingData", "Required kudos data is missing");
        }
        givenAt = givenAt.truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * The same for the same giver, receiver and plan: the source of its entry in the ledger, so that a kudos counts
     * once however many times it arrives.
     */
    public UUID id() {
        return UUID.nameUUIDFromBytes(("kudos:" + giverId + ":" + receiverId + ":" + planId)
                .getBytes(StandardCharsets.UTF_8));
    }
}

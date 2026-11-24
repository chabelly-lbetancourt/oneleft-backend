package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A photo of the album of a plan (HU-052): who of its group uploaded it and where it is in the storage.
 *
 * @param key  the object in the storage
 * @param size bytes
 */
public record Memory(UUID id, UUID planId, UUID userId, String key, long size, Instant uploadedAt) {

    /** Photos an album takes in total... */
    public static final int MAX_PER_PLAN = 100;
    /** ...and from each person. */
    public static final int MAX_PER_PERSON = 10;

    public Memory {
        if (id == null || planId == null || userId == null || key == null || uploadedAt == null) {
            throw new ValidationException("memory.missingData", "Required data of the photo is missing");
        }
    }

    public static Memory uploaded(UUID planId, UUID userId, String key, long size, Instant at) {
        return new Memory(UUID.randomUUID(), planId, userId, key, size, at);
    }

    public boolean isOf(UUID personId) {
        return userId.equals(personId);
    }
}

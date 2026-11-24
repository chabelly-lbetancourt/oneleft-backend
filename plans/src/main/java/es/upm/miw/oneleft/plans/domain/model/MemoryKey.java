package es.upm.miw.oneleft.plans.domain.model;

import java.util.UUID;

/**
 * Where a photo of an album is kept in the storage: {@code memories/<user>/<plan>/<random>.<extension>}. The name of
 * the file of the person is never used. The person goes first, so that everything of an account is under one folder
 * (HU-045), and nobody can guess the key of a photo.
 */
public final class MemoryKey {

    public static final int MAX_LENGTH = 160;
    private static final String ROOT = "memories/";

    private MemoryKey() {
    }

    public static String newFor(UUID userId, UUID planId, MemoryFormat format) {
        return prefixOf(userId, planId) + UUID.randomUUID() + "." + format.extension();
    }

    /** The folder of everything a person uploaded, to any album */
    public static String prefixOf(UUID userId) {
        return ROOT + userId + "/";
    }

    /** The folder of what a person uploaded to the album of a plan, confirmed or not */
    public static String prefixOf(UUID userId, UUID planId) {
        return prefixOf(userId) + planId + "/";
    }

    /** @throws ValidationException if the key is not one given to this person for this plan */
    public static String requireOwnedBy(String key, UUID userId, UUID planId) {
        var prefix = prefixOf(userId, planId);
        if (key == null || key.length() > MAX_LENGTH || !key.startsWith(prefix)
                || key.substring(prefix.length()).contains("/")) {
            throw new ValidationException("memory.key",
                    "The photo was not uploaded with a link given to this person for this plan");
        }
        return key;
    }
}

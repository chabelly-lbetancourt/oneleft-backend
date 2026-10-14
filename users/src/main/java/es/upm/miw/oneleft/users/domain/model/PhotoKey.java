package es.upm.miw.oneleft.users.domain.model;

import java.util.UUID;

/**
 * Where a profile photo is kept in the storage: {@code photos/<user>/<random>.<extension>}. Every upload gets a new
 * key, so a new photo never reuses the link of the previous one, and nobody can guess the key of somebody else's photo.
 */
public final class PhotoKey {

    public static final int MAX_LENGTH = 120;
    private static final String ROOT = "photos/";

    private PhotoKey() {
    }

    public static String newFor(UUID userId, PhotoFormat format) {
        return prefixOf(userId) + UUID.randomUUID() + "." + format.extension();
    }

    /** The folder of the photos of a person: the current one and any upload left unconfirmed */
    public static String prefixOf(UUID userId) {
        return ROOT + userId + "/";
    }

    /** @throws ValidationException if the key is not one given to this person */
    public static String requireOwnedBy(String key, UUID userId) {
        if (key == null || key.length() > MAX_LENGTH || !key.startsWith(prefixOf(userId))
                || key.substring(prefixOf(userId).length()).contains("/")) {
            throw new ValidationException("photo.key", "The photo was not uploaded with a link given to this user");
        }
        return key;
    }
}

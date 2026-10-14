package es.upm.miw.oneleft.users.domain.model;

/**
 * Size limit of a profile photo. The web sends a 512 px square of about 50 KB; the limit leaves room for a JPEG and
 * stops anything else.
 */
public final class PhotoSize {

    public static final long MAX_BYTES = 1024 * 1024;

    private PhotoSize() {
    }

    /** @throws ValidationException if the photo is empty or bigger than {@link #MAX_BYTES} */
    public static long require(long size) {
        if (size <= 0) {
            throw new ValidationException("photo.empty", "The photo is empty");
        }
        if (size > MAX_BYTES) {
            throw new ValidationException("photo.tooBig", "The photo allows up to " + MAX_BYTES + " bytes");
        }
        return size;
    }
}

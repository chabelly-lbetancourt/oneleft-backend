package es.upm.miw.oneleft.plans.domain.model;

/**
 * Size limit of a photo of an album (HU-052). The web resizes it before uploading; the limit leaves room for a JPEG
 * of a good phone and stops anything else.
 */
public final class MemorySize {

    public static final long MAX_BYTES = 2L * 1024 * 1024;

    private MemorySize() {
    }

    /** @throws ValidationException if the photo is empty or bigger than {@link #MAX_BYTES} */
    public static long require(long size) {
        if (size <= 0) {
            throw new ValidationException("memory.empty", "The photo is empty");
        }
        if (size > MAX_BYTES) {
            throw new ValidationException("memory.tooBig", "The photo allows up to " + MAX_BYTES + " bytes");
        }
        return size;
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formats of a photo of the album of a plan (HU-052), the same as the profile photos (HU-020): the web resizes the
 * photo before uploading it, as WebP or, where the browser cannot encode WebP, as JPEG. The declared type is not
 * trusted: the first bytes of what was uploaded say what it really is.
 */
public enum MemoryFormat {
    WEBP("image/webp", "webp"),
    JPEG("image/jpeg", "jpg");

    /** How many bytes of a file are enough to tell its format. */
    public static final int HEAD_BYTES = 12;

    private static final byte[] JPEG_START = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_MARK = {'W', 'E', 'B', 'P'};
    private static final int WEBP_MARK_AT = 8;

    private final String contentType;
    private final String extension;

    MemoryFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    /** @throws ValidationException if the photo is not WebP or JPEG */
    public static MemoryFormat of(String contentType) {
        return Arrays.stream(values())
                .filter(format -> format.contentType.equalsIgnoreCase(contentType == null ? "" : contentType.strip()))
                .findFirst()
                .orElseThrow(() -> new ValidationException("memory.format", "The photo must be WebP or JPEG"));
    }

    /** The format the first bytes of a file belong to, if it is one of these. */
    public static Optional<MemoryFormat> detect(byte[] head) {
        if (has(head, 0, JPEG_START)) {
            return Optional.of(JPEG);
        }
        if (has(head, 0, RIFF) && has(head, WEBP_MARK_AT, WEBP_MARK)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean has(byte[] head, int from, byte[] mark) {
        return head != null && head.length >= from + mark.length
                && Arrays.equals(head, from, from + mark.length, mark, 0, mark.length);
    }
}

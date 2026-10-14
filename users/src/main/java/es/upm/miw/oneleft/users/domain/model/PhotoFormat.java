package es.upm.miw.oneleft.users.domain.model;

import java.util.Arrays;

/**
 * Formats of a profile photo. The web crops and resizes the photo before uploading it, as WebP or, where the browser
 * cannot encode WebP, as JPEG.
 */
public enum PhotoFormat {
    WEBP("image/webp", "webp"),
    JPEG("image/jpeg", "jpg");

    private final String contentType;
    private final String extension;

    PhotoFormat(String contentType, String extension) {
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
    public static PhotoFormat of(String contentType) {
        return Arrays.stream(values())
                .filter(format -> format.contentType.equalsIgnoreCase(contentType == null ? "" : contentType.strip()))
                .findFirst()
                .orElseThrow(() -> new ValidationException("photo.format", "The photo must be WebP or JPEG"));
    }
}

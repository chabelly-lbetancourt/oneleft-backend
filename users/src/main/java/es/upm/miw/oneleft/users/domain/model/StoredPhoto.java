package es.upm.miw.oneleft.users.domain.model;

/**
 * What the storage says about an uploaded photo.
 *
 * @param size        bytes
 * @param contentType as uploaded (the signed link only accepts the declared one)
 */
public record StoredPhoto(long size, String contentType) {

    /** @throws ValidationException if what was uploaded is not a photo within the limits */
    public StoredPhoto requireValid() {
        PhotoFormat.of(contentType);
        PhotoSize.require(size);
        return this;
    }
}

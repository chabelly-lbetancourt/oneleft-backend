package es.upm.miw.oneleft.users.domain.model;

/** The storage of the photos did not answer: try again later. */
public class PhotoStorageUnavailableException extends RuntimeException {

    public static final String CODE = "photo.unavailable";

    public PhotoStorageUnavailableException(Throwable cause) {
        super("The storage of the photos is not available", cause);
    }
}

package es.upm.miw.oneleft.plans.domain.model;

/** The storage of the photos did not answer: try again later. */
public class MemoryStorageUnavailableException extends RuntimeException {

    public static final String CODE = "memory.unavailable";

    public MemoryStorageUnavailableException(Throwable cause) {
        super("The storage of the photos is not available", cause);
    }
}

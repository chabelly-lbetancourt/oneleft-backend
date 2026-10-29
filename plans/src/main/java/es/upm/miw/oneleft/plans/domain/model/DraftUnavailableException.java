package es.upm.miw.oneleft.plans.domain.model;

/** The language model cannot be used now (no key configured, the provider failed or answered nonsense). */
public class DraftUnavailableException extends RuntimeException {

    public static final String CODE = "draft.unavailable";

    public DraftUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DraftUnavailableException(String message) {
        super(message);
    }
}

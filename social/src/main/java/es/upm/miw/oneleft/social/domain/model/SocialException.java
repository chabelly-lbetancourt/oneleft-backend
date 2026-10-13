package es.upm.miw.oneleft.social.domain.model;

/**
 * Why a person or a request cannot be reached, with a stable code for the clients: {@code person.notFound},
 * {@code follow.requestNotFound} or {@code social.unavailable}.
 */
public class SocialException extends RuntimeException {

    public enum Reason {
        /** Nobody with that id has an account. */
        PERSON_NOT_FOUND("person.notFound"),
        /** There is no request of that person waiting for an answer. */
        REQUEST_NOT_FOUND("follow.requestNotFound"),
        /** The users service did not answer, so the name of someone new is not known. */
        UNAVAILABLE("social.unavailable");

        private final String code;

        Reason(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    private final Reason reason;

    public SocialException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public String code() {
        return reason.code();
    }
}

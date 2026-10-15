package es.upm.miw.oneleft.plans.domain.model;

/**
 * Why the plans of someone cannot be shown (HU-048), with the codes of the social service, which decides it:
 * {@code social.private}, {@code person.notFound} or {@code social.unavailable}.
 */
public class PersonActivityException extends RuntimeException {

    public enum Reason {
        /** Their profile is private and whoever asks does not follow them. */
        PRIVATE("social.private"),
        /** Nobody with that id has an account. */
        NOT_FOUND("person.notFound"),
        /** The social service did not answer: nothing is shown rather than too much. */
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

    public PersonActivityException(Reason reason, String message) {
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

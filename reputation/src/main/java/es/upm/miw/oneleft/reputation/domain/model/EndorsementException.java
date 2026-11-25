package es.upm.miw.oneleft.reputation.domain.model;

/** An endorsement that cannot be given (HU-061), with a stable code for the clients. */
public class EndorsementException extends RuntimeException {

    private final Reason reason;

    public EndorsementException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public String code() {
        return reason.code;
    }

    public enum Reason {
        /** The two never arrived at the same plan of that activity */
        NOT_PLAYED_TOGETHER("endorsement.notPlayedTogether"),
        /** Already given to that person in that activity */
        ALREADY_GIVEN("endorsement.alreadyGiven");

        private final String code;

        Reason(String code) {
            this.code = code;
        }
    }
}

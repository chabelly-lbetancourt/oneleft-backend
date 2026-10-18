package es.upm.miw.oneleft.reputation.domain.model;

/** A kudos that cannot be given (HU-060), with a stable code for the clients. */
public class KudosException extends RuntimeException {

    private final Reason reason;

    public KudosException(Reason reason, String message) {
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
        /** The giver did not arrive at the plan */
        NOT_ATTENDED("kudos.notAttended"),
        /** More than the days allowed since the attendance closed */
        CLOSED("kudos.closed"),
        /** Already given to that person in that plan */
        ALREADY_GIVEN("kudos.alreadyGiven");

        private final String code;

        Reason(String code) {
            this.code = code;
        }
    }
}

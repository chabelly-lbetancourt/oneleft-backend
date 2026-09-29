package es.upm.miw.oneleft.plans.domain.model;

/**
 * A spot cannot be taken or given back because of the current state of the plan (full, already started, already
 * joined, not on the waiting list...).
 * It is a conflict with the state, not invalid data: the API answers 409 with the {@code code}.
 */
public class JoinRejectedException extends RuntimeException {

    private final String code;

    public JoinRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

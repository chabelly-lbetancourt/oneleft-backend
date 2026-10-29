package es.upm.miw.oneleft.users.domain.model;

/**
 * An account operation cannot be done now (HU-045), with a stable code for the clients: {@code account.unavailable}
 * when the identity provider does not answer, {@code data.unavailable} when a service cannot give its data.
 */
public class AccountException extends RuntimeException {

    public enum Reason {
        IDENTITY_UNAVAILABLE("account.unavailable"),
        DATA_UNAVAILABLE("data.unavailable");

        private final String code;

        Reason(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    private final Reason reason;

    public AccountException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public String code() {
        return reason.code();
    }
}

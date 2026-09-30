package es.upm.miw.oneleft.notifications.domain.model;

/**
 * A business rule was broken. The {@code code} is stable so that clients can show the message in the user's
 * language; the English message is meant for logs and API consumers.
 */
public class ValidationException extends IllegalArgumentException {

    private final String code;

    public ValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

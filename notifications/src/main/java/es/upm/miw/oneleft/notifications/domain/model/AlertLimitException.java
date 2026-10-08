package es.upm.miw.oneleft.notifications.domain.model;

/** Each person can keep a limited number of saved alerts (HU-036). */
public class AlertLimitException extends RuntimeException {

    public AlertLimitException(int max) {
        super("You can keep up to " + max + " alerts: delete one to save another");
    }

    public String code() {
        return "alerts.limit";
    }
}

package es.upm.miw.oneleft.notifications.domain.model;

import java.util.UUID;

/** The alert does not exist or belongs to someone else: for its owner, both look the same. */
public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(UUID alertId) {
        super("Alert " + alertId + " not found");
    }

    public String code() {
        return "alerts.notFound";
    }
}

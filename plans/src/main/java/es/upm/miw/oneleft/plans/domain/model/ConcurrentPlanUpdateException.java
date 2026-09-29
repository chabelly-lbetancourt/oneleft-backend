package es.upm.miw.oneleft.plans.domain.model;

import java.util.UUID;

/** The plan was changed by someone else between reading and saving it (optimistic locking). */
public class ConcurrentPlanUpdateException extends RuntimeException {

    public ConcurrentPlanUpdateException(UUID planId) {
        super("The plan " + planId + " was changed concurrently");
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import java.util.UUID;

public class PlanNotFoundException extends RuntimeException {

    public static final String CODE = "plan.notFound";

    public PlanNotFoundException(UUID planId) {
        super("There is no plan " + planId);
    }
}

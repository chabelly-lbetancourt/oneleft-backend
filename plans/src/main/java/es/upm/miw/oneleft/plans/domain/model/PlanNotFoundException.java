package es.upm.miw.oneleft.plans.domain.model;

import java.util.UUID;

public class PlanNotFoundException extends RuntimeException {

    public PlanNotFoundException(UUID planId) {
        super("No existe el plan " + planId);
    }
}

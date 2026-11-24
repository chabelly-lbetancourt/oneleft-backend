package es.upm.miw.oneleft.plans.domain.model;

/**
 * Someone already organizes as many plans that have not started as their reputation allows (HU-063). It is a conflict
 * with the state, not invalid data: the API answers 409 with the {@code code}.
 */
public class PlanLimitException extends RuntimeException {

    public static final String CODE = "plan.tooManyActive";

    private final int max;

    public PlanLimitException(int max) {
        super("You can organize up to " + max + " plans at once: wait for one to start or cancel it");
        this.max = max;
    }

    public int max() {
        return max;
    }
}

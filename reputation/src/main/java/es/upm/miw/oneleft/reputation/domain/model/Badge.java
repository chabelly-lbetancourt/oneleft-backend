package es.upm.miw.oneleft.reputation.domain.model;

/**
 * The badges for milestones (HU-044), with the goal of each one. Only plans that were held count, and only with a
 * check-in: a plan nobody arrived at gives nothing.
 */
public enum Badge {

    /** Organized a plan that was held. */
    FIRST_PLAN_ORGANIZED(1),
    /** Arrived at ten plans. */
    TEN_PLANS_COMPLETED(10),
    /** Arrived at plans of ten different activities. */
    TEN_ACTIVITIES(10),
    /** Arrived at twenty plans in a row, without a no-show in between. */
    ALWAYS_PUNCTUAL(20),
    /** Received fifty kudos (HU-060). */
    NEIGHBOURHOOD_SOUL(50);

    private final int goal;

    Badge(int goal) {
        this.goal = goal;
    }

    public int goal() {
        return goal;
    }
}

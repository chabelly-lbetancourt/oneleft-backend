package es.upm.miw.oneleft.plans.domain.model;

/**
 * What the reputation of someone is worth in this service (HU-063), as the reputation service says: who goes first on
 * a waiting list and how many plans they may organize at once. The numbers come from there; here they are only applied.
 *
 * @param waitlistPriority the higher goes first; the order of arrival among equals
 * @param maxActivePlans   the plans someone may have published that have not started
 */
public record ReputationPerks(int waitlistPriority, int maxActivePlans) {

    /** The plans that everyone may organize at once */
    public static final int BASE_PLANS = 5;
    /** What everyone has, also when the reputation service cannot be asked */
    public static final ReputationPerks BASE = new ReputationPerks(0, BASE_PLANS);

    /** A perk only adds: never behind, nor fewer plans, than everyone */
    public ReputationPerks {
        waitlistPriority = Math.max(0, waitlistPriority);
        maxActivePlans = Math.max(BASE_PLANS, maxActivePlans);
    }
}

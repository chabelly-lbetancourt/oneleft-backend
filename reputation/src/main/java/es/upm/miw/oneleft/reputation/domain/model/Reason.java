package es.upm.miw.oneleft.reputation.domain.model;

/** Why the reputation of someone changes, and how much (HU-009). */
public enum Reason {

    /** Arrived at the meeting point: checked in (HU-025). */
    ATTENDED(10),
    /** Organized a plan that filled its spots and was held: someone of the participants arrived. */
    ORGANIZED_HELD(20),
    /** Did not check in to a plan that was held: someone of the group did. */
    NO_SHOW(-15),
    /** Cancelled their own plan less than an hour before its start. */
    LATE_CANCELLATION(-10),
    /** A report against them was confirmed (HU-033). */
    CONFIRMED_REPORT(-50),
    /** Someone of the same plan gave them a kudos (HU-060). */
    KUDOS(5);

    private final int points;

    Reason(int points) {
        this.points = points;
    }

    public int points() {
        return points;
    }
}

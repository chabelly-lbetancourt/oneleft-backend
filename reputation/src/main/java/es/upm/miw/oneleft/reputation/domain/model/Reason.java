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
    KUDOS(5),
    /** Someone who played with them vouched for their level in an activity (HU-061). */
    ENDORSEMENT(8),
    /**
     * The first plan of a week with a check-in (HU-062): {@link StreakRules#POINTS_PER_WEEK} for each week in a row,
     * up to {@link StreakRules#MAX_BONUS}; the points are those of the week, kept with its entry.
     */
    WEEKLY_STREAK(StreakRules.POINTS_PER_WEEK);

    private final int points;

    Reason(int points) {
        this.points = points;
    }

    public int points() {
        return points;
    }
}

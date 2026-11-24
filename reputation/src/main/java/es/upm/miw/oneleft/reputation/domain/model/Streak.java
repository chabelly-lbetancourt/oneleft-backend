package es.upm.miw.oneleft.reputation.domain.model;

/**
 * The weekly streak of someone (HU-062): the weeks in a row, up to this one, with at least one plan checked in.
 *
 * @param weeks    the weeks in a row; a streak that did not include this week yet is still alive until it ends
 * @param thisWeek whether this week already has a plan
 */
public record Streak(int weeks, boolean thisWeek) {

    public static final Streak NONE = new Streak(0, false);

    /** The points of the next week of the streak */
    public int nextBonus() {
        return StreakRules.bonusOf(weeks + 1);
    }
}

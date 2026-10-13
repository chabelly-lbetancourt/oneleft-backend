package es.upm.miw.oneleft.reputation.domain.model;

/**
 * What someone has done, for the badges (HU-044): plans organized that were held, plans arrived at, different
 * activities arrived at, plans arrived at in a row and kudos received.
 */
public record TrackRecord(int organizedHeld, int completed, int activities, int streak, int kudos) {

    public static final TrackRecord EMPTY = new TrackRecord(0, 0, 0, 0, 0);

    /** How far someone is towards a badge, never beyond its goal */
    public int progressOf(Badge badge) {
        var done = switch (badge) {
            case FIRST_PLAN_ORGANIZED -> organizedHeld;
            case TEN_PLANS_COMPLETED -> completed;
            case TEN_ACTIVITIES -> activities;
            case ALWAYS_PUNCTUAL -> streak;
            case NEIGHBOURHOOD_SOUL -> kudos;
        };
        return Math.min(done, badge.goal());
    }

    public boolean reached(Badge badge) {
        return progressOf(badge) >= badge.goal();
    }
}

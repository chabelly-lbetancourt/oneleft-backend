package es.upm.miw.oneleft.reputation.domain.model;

/**
 * What a level is worth (HU-063): the higher, the further ahead on the waiting lists, the more the notices of nearby
 * plans count on the person, and the more plans at once and saved alerts they may have. The numbers are only here: the
 * other services ask for them or copy them, they never work them out.
 *
 * @param waitlistPriority who goes first on a waiting list: the higher first, and the order of arrival among equals
 * @param noticeWeight     what multiplies how likely the person is to take a spot when choosing whom to notify first
 * @param maxActivePlans   the plans someone may organize at once, counting the ones that have not started
 * @param maxSavedAlerts   the alerts someone may keep
 */
public record Perks(Level level, int waitlistPriority, double noticeWeight, int maxActivePlans, int maxSavedAlerts) {

    /** A trusted organizer has reached this level... */
    public static final Level TRUSTED_ORGANIZER_LEVEL = Level.REGULAR;
    /** ...and has organized this many plans that were held */
    public static final int TRUSTED_ORGANIZER_PLANS = 5;

    public static Perks of(Level level) {
        return switch (level) {
            case NEW -> new Perks(level, 0, 1.0, 5, 5);
            case REGULAR -> new Perks(level, 1, 1.1, 6, 6);
            case NEIGHBOURHOOD_REFERENCE -> new Perks(level, 2, 1.25, 8, 8);
            case LEGEND -> new Perks(level, 3, 1.5, 10, 10);
        };
    }

    /** The seal «Organizador de confianza»: a good score is not enough, the plans must have been held */
    public static boolean trustedOrganizer(Level level, int organizedHeld) {
        return level.compareTo(TRUSTED_ORGANIZER_LEVEL) >= 0 && organizedHeld >= TRUSTED_ORGANIZER_PLANS;
    }
}

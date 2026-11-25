package es.upm.miw.oneleft.reputation.domain.model;

/**
 * How much of my statistics to bring (HU-043): the latest months, this one included, and the activities I have done
 * the most.
 */
public record StatsQuery(int months, int activities) {

    public static final int DEFAULT_MONTHS = 6;
    public static final int MAX_MONTHS = 12;
    public static final int DEFAULT_ACTIVITIES = 5;
    public static final int MAX_ACTIVITIES = 20;

    public StatsQuery {
        if (months < 1 || months > MAX_MONTHS || activities < 1 || activities > MAX_ACTIVITIES) {
            throw new ValidationException("stats.invalid", "Ask for 1 to " + MAX_MONTHS + " months and 1 to "
                    + MAX_ACTIVITIES + " activities");
        }
    }
}

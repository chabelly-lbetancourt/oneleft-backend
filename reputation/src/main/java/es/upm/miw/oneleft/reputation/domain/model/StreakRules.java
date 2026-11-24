package es.upm.miw.oneleft.reputation.domain.model;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The weekly streak (HU-062). The first check-in of a week gives a bonus that grows {@link #POINTS_PER_WEEK} for each
 * week in a row with at least one plan, up to {@link #MAX_BONUS}: 5, 10, 15, 20, 25, 25… A week without plans ends
 * the streak. Weeks go from Monday to Sunday in Spain, where the plans are. The bonus is one entry per person and week,
 * so a check-in delivered twice, or a second plan that week, gives nothing more.
 */
public final class StreakRules {

    public static final ZoneId ZONE = ZoneId.of("Europe/Madrid");
    public static final int POINTS_PER_WEEK = 5;
    /** The bonus of a week stops growing at the fifth week in a row */
    public static final int MAX_BONUS = 25;
    /** How far back a streak is looked for: a year */
    public static final int MAX_WEEKS = 52;

    private StreakRules() {
    }

    /** The bonus of the n-th week in a row */
    public static int bonusOf(int weeksInARow) {
        return Math.min(POINTS_PER_WEEK * weeksInARow, MAX_BONUS);
    }

    /** The Monday of the week of an instant, in {@link #ZONE} */
    public static LocalDate weekOf(Instant at) {
        return at.atZone(ZONE).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** From when the plans are looked at to know the streak of the week of an instant */
    public static Instant since(Instant at) {
        return weekOf(at).minusWeeks(MAX_WEEKS - 1L).atStartOfDay(ZONE).toInstant();
    }

    /** The weeks of some check-ins */
    public static Set<LocalDate> weeksOf(Collection<Instant> checkIns) {
        return checkIns.stream().map(StreakRules::weekOf).collect(Collectors.toSet());
    }

    /** The weeks in a row with plans that end at a week, that one included */
    public static int weeksInARow(Set<LocalDate> weeks, LocalDate until) {
        var count = 0;
        for (var week = until; weeks.contains(week) && count < MAX_WEEKS; week = week.minusWeeks(1)) {
            count++;
        }
        return count;
    }

    /** The streak at an instant: the one of this week, or the one that ended last week while this one lasts */
    public static Streak streakAt(Set<LocalDate> weeks, Instant now) {
        var thisWeek = weekOf(now);
        if (weeks.contains(thisWeek)) {
            return new Streak(weeksInARow(weeks, thisWeek), true);
        }
        return new Streak(weeksInARow(weeks, thisWeek.minusWeeks(1)), false);
    }

    /**
     * The bonus of the week of a check-in, for the weeks in a row up to it.
     *
     * @param weeks the weeks with plans of the person, the one of the check-in included
     */
    public static LedgerEntry bonus(UUID userId, Instant checkedInAt, Set<LocalDate> weeks) {
        var week = weekOf(checkedInAt);
        var points = bonusOf(Math.max(1, weeksInARow(weeks, week)));
        return new LedgerEntry(userId, Reason.WEEKLY_STREAK, points, sourceOf(userId, week), null, checkedInAt);
    }

    /** The same for the same person and week: the bonus counts once a week */
    static UUID sourceOf(UUID userId, LocalDate week) {
        return UUID.nameUUIDFromBytes(("streak:" + userId + ":" + week).getBytes(StandardCharsets.UTF_8));
    }
}

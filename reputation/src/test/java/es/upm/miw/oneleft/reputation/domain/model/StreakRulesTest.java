package es.upm.miw.oneleft.reputation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-062: the weeks of a streak and the bonus of each one. */
class StreakRulesTest {

    /** Tuesday 13 October 2026, in the week that starts on Monday the 12th */
    private static final Instant NOW = Instant.parse("2026-10-13T16:30:00Z");
    private static final LocalDate THIS_WEEK = LocalDate.parse("2026-10-12");
    private final UUID lucia = UUID.randomUUID();

    @Test
    void theBonusGrowsFiveAWeekUpToItsCap() {
        assertThat(IntStream.rangeClosed(1, 7).map(StreakRules::bonusOf))
                .containsExactly(5, 10, 15, 20, 25, 25, 25);
        assertThat(StreakRules.bonusOf(1000)).isEqualTo(StreakRules.MAX_BONUS);
    }

    @Test
    void aWeekGoesFromMondayToSundayInSpain() {
        // Sunday 23:30 and Monday 00:30 in Madrid
        assertThat(StreakRules.weekOf(Instant.parse("2026-10-18T21:30:00Z"))).isEqualTo(THIS_WEEK);
        assertThat(StreakRules.weekOf(Instant.parse("2026-10-18T22:30:00Z"))).isEqualTo(THIS_WEEK.plusWeeks(1));
        assertThat(StreakRules.weeksOf(Set.of(NOW, NOW.plusSeconds(3600)))).containsExactly(THIS_WEEK);
        assertThat(StreakRules.since(NOW)).isEqualTo(THIS_WEEK.minusWeeks(StreakRules.MAX_WEEKS - 1L)
                .atStartOfDay(StreakRules.ZONE).toInstant());
    }

    @Test
    void aWeekWithoutPlansEndsTheStreak() {
        var weeks = Set.of(THIS_WEEK, THIS_WEEK.minusWeeks(1), THIS_WEEK.minusWeeks(3));

        assertThat(StreakRules.weeksInARow(weeks, THIS_WEEK)).isEqualTo(2);
        assertThat(StreakRules.weeksInARow(weeks, THIS_WEEK.minusWeeks(2))).isZero();
        assertThat(StreakRules.weeksInARow(weeks, THIS_WEEK.minusWeeks(3))).isEqualTo(1);
    }

    @Test
    void aStreakWithoutAPlanThisWeekIsAliveUntilTheWeekEnds() {
        var lastTwoWeeks = Set.of(THIS_WEEK.minusWeeks(1), THIS_WEEK.minusWeeks(2));

        assertThat(StreakRules.streakAt(lastTwoWeeks, NOW)).isEqualTo(new Streak(2, false));
        assertThat(StreakRules.streakAt(Set.of(THIS_WEEK, THIS_WEEK.minusWeeks(1)), NOW))
                .isEqualTo(new Streak(2, true));
        assertThat(StreakRules.streakAt(Set.of(THIS_WEEK.minusWeeks(2)), NOW)).isEqualTo(Streak.NONE);
        assertThat(new Streak(2, false).nextBonus()).isEqualTo(15);
        assertThat(new Streak(9, true).nextBonus()).isEqualTo(StreakRules.MAX_BONUS);
    }

    @Test
    void theBonusIsOneEntryPerPersonAndWeek() {
        var weeks = Set.of(THIS_WEEK, THIS_WEEK.minusWeeks(1));

        var bonus = StreakRules.bonus(lucia, NOW, weeks);

        assertThat(bonus).extracting(LedgerEntry::userId, LedgerEntry::reason, LedgerEntry::points,
                LedgerEntry::planTitle).containsExactly(lucia, Reason.WEEKLY_STREAK, 10, null);
        assertThat(StreakRules.bonus(lucia, NOW.plusSeconds(86_400), weeks).sourceId()).isEqualTo(bonus.sourceId());
        assertThat(StreakRules.bonus(lucia, NOW.minusSeconds(7 * 86_400), weeks).sourceId())
                .isNotEqualTo(bonus.sourceId());
        assertThat(StreakRules.bonus(UUID.randomUUID(), NOW, weeks).sourceId()).isNotEqualTo(bonus.sourceId());
        // Even if the week of the check-in is not known yet, it is the first one
        assertThat(StreakRules.bonus(lucia, NOW, Set.of()).points()).isEqualTo(StreakRules.POINTS_PER_WEEK);
    }
}

package es.upm.miw.oneleft.reputation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-043: the months of my statistics, in the time of Spain, and how much of them can be asked. */
class StatsRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    @Test
    void theMonthIsTheOneInSpain() {
        // 23:30 UTC of the last day of October is already November in Spain (UTC+1 in winter)
        assertThat(StatsRules.monthOf(Instant.parse("2026-10-31T23:30:00Z"))).isEqualTo(YearMonth.of(2026, 11));
        assertThat(StatsRules.monthOf(Instant.parse("2026-10-31T22:30:00Z"))).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void theLatestMonthsStartAtMidnightInSpainOfTheFirstDay() {
        assertThat(StatsRules.since(NOW, 1)).isEqualTo(Instant.parse("2026-10-31T23:00:00Z"));
        // September is in summer time (UTC+2), and the year is crossed backwards
        assertThat(StatsRules.since(NOW, 3)).isEqualTo(Instant.parse("2026-08-31T22:00:00Z"));
        assertThat(StatsRules.since(NOW, 12)).isEqualTo(Instant.parse("2025-11-30T23:00:00Z"));
    }

    @Test
    void theSeriesHasEveryMonthTheOldestFirstAndEmptyTheOnesWithoutPlans() {
        var october = new MonthPlans(YearMonth.of(2026, 10), 4, 1, 3);
        var november = new MonthPlans(YearMonth.of(2026, 11), 6, 2, 4);

        var series = StatsRules.series(List.of(november, october), NOW, 4);

        assertThat(series).containsExactly(MonthPlans.empty(YearMonth.of(2026, 8)),
                MonthPlans.empty(YearMonth.of(2026, 9)), october, november);
    }

    @Test
    void myStatsReadThisMonthAndTheFavouriteFromTheirSeries() {
        var stats = new MyStats(10, 3, 7, List.of(new ActivityPlans("PADEL", 6), new ActivityPlans("RUNNING", 4)),
                StatsRules.series(List.of(new MonthPlans(YearMonth.of(2026, 11), 6, 2, 4)), NOW, 2));

        assertThat(stats.plansThisMonth()).isEqualTo(6);
        assertThat(stats.favoriteActivity()).contains("PADEL");
        // Without plans there is no favourite, and the month is empty
        var empty = new MyStats(0, 0, 0, List.of(), List.of());
        assertThat(empty.plansThisMonth()).isZero();
        assertThat(empty.favoriteActivity()).isEmpty();
    }

    @Test
    void onlyAFewMonthsAndActivitiesCanBeAsked() {
        assertThat(new StatsQuery(StatsQuery.MAX_MONTHS, StatsQuery.MAX_ACTIVITIES).months()).isEqualTo(12);
        for (var query : List.of(new int[]{0, 5}, new int[]{13, 5}, new int[]{6, 0}, new int[]{6, 21})) {
            assertThatThrownBy(() -> new StatsQuery(query[0], query[1]))
                    .isInstanceOfSatisfying(ValidationException.class,
                            error -> assertThat(error.code()).isEqualTo("stats.invalid"));
        }
    }
}

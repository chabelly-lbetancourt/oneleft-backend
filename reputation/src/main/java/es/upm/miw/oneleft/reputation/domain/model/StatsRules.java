package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The months of my statistics (HU-043). A month starts and ends at midnight in {@link AntiCheatRules#ZONE}, as the
 * days of the daily caps and the weeks of the streak do.
 */
public final class StatsRules {

    private StatsRules() {
    }

    public static YearMonth monthOf(Instant at) {
        return YearMonth.from(at.atZone(AntiCheatRules.ZONE));
    }

    /** When the oldest of the latest months starts, this month included */
    public static Instant since(Instant now, int months) {
        return monthOf(now).minusMonths(months - 1L).atDay(1).atStartOfDay(AntiCheatRules.ZONE).toInstant();
    }

    /**
     * @param known the months with plans, in any order
     * @return every one of the latest months, the oldest first and this one the last, empty those without plans
     */
    public static List<MonthPlans> series(List<MonthPlans> known, Instant now, int months) {
        var byMonth = known.stream().collect(Collectors.toMap(MonthPlans::month, Function.identity()));
        var first = monthOf(now).minusMonths(months - 1L);
        return IntStream.range(0, months).mapToObj(first::plusMonths)
                .map(month -> byMonth.getOrDefault(month, MonthPlans.empty(month))).toList();
    }
}

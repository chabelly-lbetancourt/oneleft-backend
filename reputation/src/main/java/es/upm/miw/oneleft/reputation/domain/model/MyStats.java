package es.upm.miw.oneleft.reputation.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * My statistics (HU-043), read from the track record that the attendance events already fill: nothing is kept twice.
 * Only plans that were held count, with a check-in, as for the badges (HU-044) and the statistics of a person
 * (HU-048), so the totals are the same numbers.
 *
 * @param plansCompleted the plans I arrived at, ever
 * @param plansOrganized the plans I organized that were held, ever
 * @param plansJoined    the plans of someone else I arrived at, ever
 * @param activities     the activities I have done the most, the first one the favourite
 * @param months         the latest months, the oldest first and this one the last, also those without plans
 */
public record MyStats(int plansCompleted, int plansOrganized, int plansJoined, List<ActivityPlans> activities,
                      List<MonthPlans> months) {

    public MyStats {
        activities = List.copyOf(activities);
        months = List.copyOf(months);
    }

    /** The plans I arrived at this month */
    public int plansThisMonth() {
        return months.isEmpty() ? 0 : months.getLast().completed();
    }

    /** The activity with the most plans; among equals, the one done the latest. None without plans */
    public Optional<String> favoriteActivity() {
        return activities.stream().findFirst().map(ActivityPlans::activity);
    }
}

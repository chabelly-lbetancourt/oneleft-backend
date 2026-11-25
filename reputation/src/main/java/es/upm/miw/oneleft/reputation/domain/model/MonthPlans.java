package es.upm.miw.oneleft.reputation.domain.model;

import java.time.YearMonth;

/**
 * The plans of someone in one month (HU-043), in the time of {@link AntiCheatRules#ZONE}.
 *
 * @param completed the plans they arrived at (check-in)
 * @param organized the plans they organized that were held
 * @param joined    the plans of someone else they arrived at
 */
public record MonthPlans(YearMonth month, int completed, int organized, int joined) {

    public static MonthPlans empty(YearMonth month) {
        return new MonthPlans(month, 0, 0, 0);
    }
}

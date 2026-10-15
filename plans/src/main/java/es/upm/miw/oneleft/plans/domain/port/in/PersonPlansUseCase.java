package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.RecentPlan;

import java.util.List;
import java.util.UUID;

/** The plans of someone, for their social profile (HU-048). */
public interface PersonPlansUseCase {

    int RECENT_PLANS = 5;

    /**
     * The last {@link #RECENT_PLANS} plans held that someone organized or arrived at, the latest first.
     *
     * @throws es.upm.miw.oneleft.plans.domain.model.PersonActivityException if their profile is private and
     *                                                                        {@code viewer} does not follow them,
     *                                                                        or it cannot be known
     */
    List<RecentPlan> recentPlansOf(UUID viewer, UUID userId);
}

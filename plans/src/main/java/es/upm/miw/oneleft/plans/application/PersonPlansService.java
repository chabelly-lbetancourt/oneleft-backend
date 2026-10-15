package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.PersonActivityException;
import es.upm.miw.oneleft.plans.domain.model.RecentPlan;
import es.upm.miw.oneleft.plans.domain.port.in.PersonPlansUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.ActivityAccess;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PersonPlansService implements PersonPlansUseCase {

    private final PlanRepository plans;
    private final ActivityAccess access;

    public PersonPlansService(PlanRepository plans, ActivityAccess access) {
        this.plans = plans;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentPlan> recentPlansOf(UUID viewer, UUID userId) {
        // My own plans need no question to the social service
        if (!viewer.equals(userId) && !access.visible(userId)) {
            throw new PersonActivityException(PersonActivityException.Reason.PRIVATE,
                    "Only their followers see their plans");
        }
        return plans.findRecentlyHeldWith(userId, RECENT_PLANS);
    }
}

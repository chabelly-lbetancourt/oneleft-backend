package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.port.out.HiddenPlans;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/** Output adapter: the plans each person keeps out of their activity, in PostgreSQL (HU-059). */
@Repository
public class JpaHiddenPlanRepository implements HiddenPlans {

    private final SpringDataHiddenPlanRepository hidden;

    public JpaHiddenPlanRepository(SpringDataHiddenPlanRepository hidden) {
        this.hidden = hidden;
    }

    @Override
    public boolean hide(UUID userId, UUID planId, Instant at) {
        return hidden.hide(userId, planId, at) == 1;
    }

    @Override
    public boolean show(UUID userId, UUID planId) {
        return hidden.show(userId, planId) > 0;
    }

    @Override
    public boolean isHidden(UUID userId, UUID planId) {
        return hidden.existsById(new HiddenPlanEntity.Key(userId, planId));
    }
}

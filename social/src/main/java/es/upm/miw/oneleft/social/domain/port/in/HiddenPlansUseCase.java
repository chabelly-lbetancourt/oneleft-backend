package es.upm.miw.oneleft.social.domain.port.in;

import java.util.UUID;

/**
 * Keeping a plan out of my activity (HU-059): my followers do not see that I published, joined or completed it, and
 * nothing of it is kept from then on. Showing it again brings back what was kept before.
 */
public interface HiddenPlansUseCase {

    void hidePlan(UUID me, UUID planId);

    void showPlan(UUID me, UUID planId);

    boolean isHidden(UUID me, UUID planId);
}

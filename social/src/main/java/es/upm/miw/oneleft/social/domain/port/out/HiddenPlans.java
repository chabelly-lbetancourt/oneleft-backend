package es.upm.miw.oneleft.social.domain.port.out;

import java.time.Instant;
import java.util.UUID;

/** Plans someone keeps out of their activity (HU-059). */
public interface HiddenPlans {

    /** @return whether it was not hidden yet */
    boolean hide(UUID userId, UUID planId, Instant at);

    /** @return whether it was hidden */
    boolean show(UUID userId, UUID planId);

    boolean isHidden(UUID userId, UUID planId);
}

package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.Badge;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** The badges earned: once each, for good. */
public interface Badges {

    /** @return false if the person already had it */
    boolean award(UUID userId, Badge badge, Instant at);

    Map<Badge, Instant> earnedBy(UUID userId);
}

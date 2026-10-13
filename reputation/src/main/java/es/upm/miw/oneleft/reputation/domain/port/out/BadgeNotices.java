package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.Badge;

import java.time.Instant;
import java.util.UUID;

/** Tells someone, in real time, that they earned a badge. */
public interface BadgeNotices {

    void earned(UUID userId, Badge badge, Instant at);
}

package es.upm.miw.oneleft.plans.domain.port.out;

import java.time.Instant;
import java.util.UUID;

/** The copy of the blocks of the social service (HU-033). Repeating a change changes nothing. */
public interface BlockCopies {

    void block(UUID blockerId, UUID blockedId, Instant at);

    void unblock(UUID blockerId, UUID blockedId);
}

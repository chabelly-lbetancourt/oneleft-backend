package es.upm.miw.oneleft.social.domain.port.out;

import es.upm.miw.oneleft.social.domain.model.BlockedPerson;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;

import java.time.Instant;
import java.util.UUID;

/** Who blocked whom (HU-033). */
public interface Blocks {

    /** @return whether {@code blockedId} was not blocked by {@code blockerId} yet */
    boolean block(UUID blockerId, UUID blockedId, Instant at);

    /** @return whether there was a block to remove */
    boolean unblock(UUID blockerId, UUID blockedId);

    boolean blocked(UUID blockerId, UUID blockedId);

    /** The people {@code blockerId} blocked, the latest first, with their names. */
    PageResult<BlockedPerson> blockedBy(UUID blockerId, PageQuery page);
}

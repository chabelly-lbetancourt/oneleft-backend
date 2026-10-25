package es.upm.miw.oneleft.notifications.domain.port.in;

import java.time.Instant;
import java.util.UUID;

/** Keeping the copy of the blocks of the social service (HU-033). */
public interface KeepBlocksUseCase {

    void blocked(UUID blockerId, UUID blockedId, Instant at);

    void unblocked(UUID blockerId, UUID blockedId);
}

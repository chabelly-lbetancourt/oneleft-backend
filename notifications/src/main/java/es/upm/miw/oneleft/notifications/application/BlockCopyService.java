package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.port.in.KeepBlocksUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.BlockCopies;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The blocks of the social service (HU-033), copied here so that the notices of nearby plans leave them out without
 * asking that service for each person.
 */
@Service
@Transactional
public class BlockCopyService implements KeepBlocksUseCase {

    private final BlockCopies blocks;

    public BlockCopyService(BlockCopies blocks) {
        this.blocks = blocks;
    }

    @Override
    public void blocked(UUID blockerId, UUID blockedId, Instant at) {
        blocks.block(blockerId, blockedId, at);
    }

    @Override
    public void unblocked(UUID blockerId, UUID blockedId) {
        blocks.unblock(blockerId, blockedId);
    }
}

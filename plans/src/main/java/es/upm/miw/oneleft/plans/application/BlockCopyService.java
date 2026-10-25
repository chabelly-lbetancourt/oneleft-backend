package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.port.in.KeepBlocksUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.BlockCopies;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The blocks of the social service (HU-033), copied here so that the searches, the plan detail, joining and the free
 * people near a plan can leave them out without asking that service each time.
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

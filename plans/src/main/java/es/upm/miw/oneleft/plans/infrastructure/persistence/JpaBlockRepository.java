package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.port.out.BlockCopies;
import es.upm.miw.oneleft.plans.domain.port.out.Blocks;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/** Output adapter: the copy of the blocks, in PostgreSQL (HU-033). */
@Repository
public class JpaBlockRepository implements Blocks, BlockCopies {

    private final SpringDataBlockRepository blocks;

    public JpaBlockRepository(SpringDataBlockRepository blocks) {
        this.blocks = blocks;
    }

    @Override
    public boolean between(UUID userId, UUID otherId) {
        return userId != null && otherId != null && !userId.equals(otherId) && blocks.between(userId, otherId);
    }

    @Override
    public void block(UUID blockerId, UUID blockedId, Instant at) {
        blocks.block(blockerId, blockedId, at);
    }

    @Override
    public void unblock(UUID blockerId, UUID blockedId) {
        blocks.unblock(blockerId, blockedId);
    }
}

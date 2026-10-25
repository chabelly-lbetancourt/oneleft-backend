package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.BlockedPerson;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.port.out.Blocks;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/** Output adapter: who blocked whom, in PostgreSQL (HU-033). */
@Repository
public class JpaBlockRepository implements Blocks {

    private final SpringDataBlockRepository blocks;

    public JpaBlockRepository(SpringDataBlockRepository blocks) {
        this.blocks = blocks;
    }

    @Override
    public boolean block(UUID blockerId, UUID blockedId, Instant at) {
        return blocks.block(blockerId, blockedId, at) == 1;
    }

    @Override
    public boolean unblock(UUID blockerId, UUID blockedId) {
        return blocks.unblock(blockerId, blockedId) > 0;
    }

    @Override
    public boolean blocked(UUID blockerId, UUID blockedId) {
        return blocks.existsById(new BlockEntity.Key(blockerId, blockedId));
    }

    @Override
    public PageResult<BlockedPerson> blockedBy(UUID blockerId, PageQuery page) {
        var rows = blocks.blockedBy(blockerId, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(rows.map(BlockedRow::toDomain).getContent(), page.page(), page.size(),
                rows.getTotalElements());
    }
}

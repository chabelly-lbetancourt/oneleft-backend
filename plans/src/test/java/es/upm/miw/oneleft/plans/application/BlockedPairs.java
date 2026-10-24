package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.port.out.Blocks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** The copy of the blocks in memory (HU-033), with the same rule as the SQL query: either direction counts. */
class BlockedPairs implements Blocks {

    final Set<String> pairs = new HashSet<>();

    void block(UUID blockerId, UUID blockedId) {
        pairs.add(blockerId + ">" + blockedId);
    }

    @Override
    public boolean between(UUID userId, UUID otherId) {
        return pairs.contains(userId + ">" + otherId) || pairs.contains(otherId + ">" + userId);
    }
}

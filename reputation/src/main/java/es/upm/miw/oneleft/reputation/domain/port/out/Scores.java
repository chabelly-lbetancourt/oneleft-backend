package es.upm.miw.oneleft.reputation.domain.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** The projection of the ledger: the score of each person, kept up to date entry by entry. */
public interface Scores {

    void add(UUID userId, int points);

    /** The scores of the people that have one */
    Map<UUID, Integer> scoresOf(Collection<UUID> userIds);

    /** Computes every score again as the sum of its entries; @return how many people have a score */
    int rebuildFromLedger();
}

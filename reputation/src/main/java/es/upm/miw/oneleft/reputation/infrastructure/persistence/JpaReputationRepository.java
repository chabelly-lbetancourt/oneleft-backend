package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;
import es.upm.miw.oneleft.reputation.domain.port.out.Ledger;
import es.upm.miw.oneleft.reputation.domain.port.out.Scores;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Output adapter: the ledger and the scores in PostgreSQL. */
@Repository
public class JpaReputationRepository implements Ledger, Scores {

    private final SpringDataLedgerRepository ledger;
    private final SpringDataScoreRepository scores;

    public JpaReputationRepository(SpringDataLedgerRepository ledger, SpringDataScoreRepository scores) {
        this.ledger = ledger;
        this.scores = scores;
    }

    @Override
    public boolean append(LedgerEntry entry) {
        return ledger.append(entry.userId(), entry.reason().name(), entry.points(), entry.sourceId(),
                entry.planTitle(), entry.occurredAt()) == 1;
    }

    @Override
    public PageResult<LedgerEntry> entriesOf(UUID userId, PageQuery page) {
        var rows = ledger.findByUserIdOrderByOccurredAtDescIdDesc(userId, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(rows.map(LedgerEntryEntity::toDomain).getContent(), page.page(), page.size(),
                rows.getTotalElements());
    }

    @Override
    public void add(UUID userId, int points) {
        scores.add(userId, points);
    }

    @Override
    public Map<UUID, Integer> scoresOf(Collection<UUID> userIds) {
        return scores.findAllById(userIds).stream().collect(Collectors.toMap(ScoreEntity::userId, ScoreEntity::score));
    }

    @Override
    public int rebuildFromLedger() {
        scores.deleteWithoutEntries();
        return scores.rebuildFromLedger();
    }
}

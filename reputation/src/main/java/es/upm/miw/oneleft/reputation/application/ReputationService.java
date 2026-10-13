package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.model.AttendanceFact;
import es.upm.miw.oneleft.reputation.domain.model.CancellationFact;
import es.upm.miw.oneleft.reputation.domain.model.CheckInFact;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;
import es.upm.miw.oneleft.reputation.domain.model.ReportFact;
import es.upm.miw.oneleft.reputation.domain.model.Reputation;
import es.upm.miw.oneleft.reputation.domain.model.ReputationRules;
import es.upm.miw.oneleft.reputation.domain.model.ValidationException;
import es.upm.miw.oneleft.reputation.domain.port.in.RebuildScoresUseCase;
import es.upm.miw.oneleft.reputation.domain.port.in.RecordReputationUseCase;
import es.upm.miw.oneleft.reputation.domain.port.in.ReputationQueryUseCase;
import es.upm.miw.oneleft.reputation.domain.port.out.Ledger;
import es.upm.miw.oneleft.reputation.domain.port.out.Scores;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Reputation from events (HU-009). Each entry goes to the immutable ledger and, only if it is new, to the score of its
 * person, in the same transaction: an event delivered twice counts once.
 */
@Service
@Transactional
public class ReputationService implements RecordReputationUseCase, ReputationQueryUseCase, RebuildScoresUseCase {

    private final Ledger ledger;
    private final Scores scores;

    public ReputationService(Ledger ledger, Scores scores) {
        this.ledger = ledger;
        this.scores = scores;
    }

    @Override
    public void record(CheckInFact checkIn) {
        append(ReputationRules.attended(checkIn));
    }

    @Override
    public void record(AttendanceFact attendance) {
        ReputationRules.attendanceClosed(attendance).forEach(this::append);
    }

    @Override
    public void record(CancellationFact cancellation) {
        ReputationRules.cancelled(cancellation).ifPresent(this::append);
    }

    @Override
    public void record(ReportFact report) {
        append(ReputationRules.reportConfirmed(report));
    }

    @Override
    @Transactional(readOnly = true)
    public Reputation reputationOf(UUID userId) {
        return reputationsOf(List.of(userId)).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reputation> reputationsOf(Collection<UUID> userIds) {
        var people = new LinkedHashSet<>(userIds);
        if (people.isEmpty() || people.size() > MAX_PEOPLE) {
            throw new ValidationException("reputation.people", "Ask for 1 to " + MAX_PEOPLE + " people");
        }
        var known = scores.scoresOf(people);
        return people.stream().map(userId -> new Reputation(userId, known.getOrDefault(userId, 0))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<LedgerEntry> ledgerOf(UUID userId, PageQuery page) {
        return ledger.entriesOf(userId, page);
    }

    @Override
    public int rebuildScores() {
        return scores.rebuildFromLedger();
    }

    private void append(LedgerEntry entry) {
        if (ledger.append(entry)) {
            scores.add(entry.userId(), entry.points());
        }
    }
}

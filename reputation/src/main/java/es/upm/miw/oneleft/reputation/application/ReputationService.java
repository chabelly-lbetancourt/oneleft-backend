package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.model.AttendanceFact;
import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.BadgeProgress;
import es.upm.miw.oneleft.reputation.domain.model.BadgeRules;
import es.upm.miw.oneleft.reputation.domain.model.CancellationFact;
import es.upm.miw.oneleft.reputation.domain.model.CheckInFact;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.model.ReportFact;
import es.upm.miw.oneleft.reputation.domain.model.Streak;
import es.upm.miw.oneleft.reputation.domain.model.StreakRules;
import es.upm.miw.oneleft.reputation.domain.model.TrackRecord;
import es.upm.miw.oneleft.reputation.domain.model.Reputation;
import es.upm.miw.oneleft.reputation.domain.model.ReputationRules;
import es.upm.miw.oneleft.reputation.domain.model.ValidationException;
import es.upm.miw.oneleft.reputation.domain.port.in.BadgesUseCase;
import es.upm.miw.oneleft.reputation.domain.port.in.RebuildScoresUseCase;
import es.upm.miw.oneleft.reputation.domain.port.in.RecordReputationUseCase;
import es.upm.miw.oneleft.reputation.domain.port.in.ReputationQueryUseCase;
import es.upm.miw.oneleft.reputation.domain.port.out.BadgeNotices;
import es.upm.miw.oneleft.reputation.domain.port.out.Badges;
import es.upm.miw.oneleft.reputation.domain.port.out.Ledger;
import es.upm.miw.oneleft.reputation.domain.port.out.Scores;
import es.upm.miw.oneleft.reputation.domain.port.out.TrackRecords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reputation from events (HU-009). Each entry goes to the immutable ledger and, only if it is new, to the score of its
 * person, in the same transaction: an event delivered twice counts once. When the attendance of a plan closes, its
 * group also adds to their track record and may earn badges (HU-044), each one once, with a notice. The first
 * check-in of a week also gives the bonus of the weekly streak (HU-062), once a week. What adds points has a daily
 * cap per reason (HU-066).
 */
@Service
@Transactional
public class ReputationService implements RecordReputationUseCase, ReputationQueryUseCase, RebuildScoresUseCase,
        BadgesUseCase {

    private final Ledger ledger;
    private final Scores scores;
    private final TrackRecords trackRecords;
    private final Badges badges;
    private final BadgeAwards awards;
    private final DailyCaps dailyCaps;
    private final Clock clock;

    public ReputationService(Ledger ledger, Scores scores, TrackRecords trackRecords, Badges badges,
                             BadgeNotices notices, Clock clock) {
        this.ledger = ledger;
        this.scores = scores;
        this.trackRecords = trackRecords;
        this.badges = badges;
        this.awards = new BadgeAwards(trackRecords, badges, notices);
        this.dailyCaps = new DailyCaps(ledger);
        this.clock = clock;
    }

    @Override
    public void record(CheckInFact checkIn) {
        append(ReputationRules.attended(checkIn));
        append(StreakRules.bonus(checkIn.userId(), checkIn.occurredAt(),
                weeksWithPlans(checkIn.userId(), checkIn.occurredAt())));
    }

    @Override
    public void record(AttendanceFact attendance) {
        ReputationRules.attendanceClosed(attendance).forEach(this::append);
        var lines = BadgeRules.attendances(attendance);
        trackRecords.record(lines);
        lines.forEach(line -> awards.awardReached(line.userId(), attendance.occurredAt()));
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
    @Transactional(readOnly = true)
    public Streak streakOf(UUID userId) {
        var now = clock.instant();
        return StreakRules.streakAt(weeksWithPlans(userId, now), now);
    }

    /** The weeks with a check-in of someone, as far back as a streak is looked for */
    private Set<LocalDate> weeksWithPlans(UUID userId, Instant at) {
        return StreakRules.weeksOf(ledger.occurrencesOf(userId, Reason.ATTENDED, StreakRules.since(at)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeProgress> myBadges(UUID me) {
        var record = trackRecords.of(me);
        var earned = badges.earnedBy(me);
        return Arrays.stream(Badge.values())
                .map(badge -> new BadgeProgress(badge, record.progressOf(badge), badge.goal(), earned.get(badge)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeProgress> earnedBadgesOf(UUID userId) {
        var earned = badges.earnedBy(userId);
        return Arrays.stream(Badge.values()).filter(earned::containsKey)
                .map(badge -> new BadgeProgress(badge, badge.goal(), badge.goal(), earned.get(badge))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrackRecord trackRecordOf(UUID userId) {
        return trackRecords.of(userId);
    }

    @Override
    public int rebuildScores() {
        return scores.rebuildFromLedger();
    }

    /** The entry as the daily caps leave it (HU-066), kept even if it gives nothing, so the ledger explains why */
    private void append(LedgerEntry entry) {
        var limited = dailyCaps.apply(entry);
        if (ledger.append(limited)) {
            scores.add(limited.userId(), limited.points());
        }
    }
}

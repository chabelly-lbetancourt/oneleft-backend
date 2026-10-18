package es.upm.miw.oneleft.reputation;

import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.model.TrackRecord;
import es.upm.miw.oneleft.reputation.domain.port.out.Badges;
import es.upm.miw.oneleft.reputation.domain.port.out.KudosStore;
import es.upm.miw.oneleft.reputation.domain.port.out.Ledger;
import es.upm.miw.oneleft.reputation.domain.port.out.Scores;
import es.upm.miw.oneleft.reputation.domain.port.out.TrackRecords;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The ledger, the scores, the track record, the badges and the kudos in memory, with the same rules as the SQL (tested
 * in ReputationIntegrationTest and KudosIntegrationTest).
 */
public class InMemoryReputation implements Ledger, Scores, TrackRecords, Badges, KudosStore {

    public final List<LedgerEntry> entries = new ArrayList<>();
    public final Map<UUID, Integer> scores = new HashMap<>();
    public final Map<String, Attendance> lines = new HashMap<>();
    public final Map<UUID, Map<Badge, Instant>> badges = new HashMap<>();
    public final List<Kudos> kudos = new ArrayList<>();

    @Override
    public void record(List<Attendance> attendances) {
        attendances.forEach(line -> lines.putIfAbsent(line.userId() + ">" + line.planId(), line));
    }

    @Override
    public TrackRecord of(UUID userId) {
        var mine = lines.values().stream().filter(line -> line.userId().equals(userId))
                .sorted(Comparator.comparing(Attendance::closedAt).reversed()).toList();
        var streak = (int) mine.stream().filter(Attendance::held).takeWhile(Attendance::arrived).count();
        var received = (int) entries.stream()
                .filter(entry -> entry.userId().equals(userId) && entry.reason() == Reason.KUDOS).count();
        return new TrackRecord((int) mine.stream().filter(line -> line.organizer() && line.held()).count(),
                (int) mine.stream().filter(Attendance::arrived).count(),
                (int) mine.stream().filter(Attendance::arrived).map(Attendance::activity).distinct().count(),
                streak, received);
    }

    @Override
    public Optional<Attendance> attendance(UUID userId, UUID planId) {
        return Optional.ofNullable(lines.get(userId + ">" + planId));
    }

    @Override
    public List<Attendance> attendancesOf(UUID planId) {
        return lines.values().stream().filter(line -> line.planId().equals(planId)).toList();
    }

    @Override
    public boolean award(UUID userId, Badge badge, Instant at) {
        return badges.computeIfAbsent(userId, id -> new EnumMap<>(Badge.class)).putIfAbsent(badge, at) == null;
    }

    @Override
    public Map<Badge, Instant> earnedBy(UUID userId) {
        return badges.getOrDefault(userId, Map.of());
    }

    @Override
    public boolean append(LedgerEntry entry) {
        var known = entries.stream().anyMatch(other -> other.userId().equals(entry.userId())
                && other.reason() == entry.reason() && other.sourceId().equals(entry.sourceId()));
        return !known && entries.add(entry);
    }

    @Override
    public PageResult<LedgerEntry> entriesOf(UUID userId, PageQuery page) {
        var mine = entries.stream().filter(entry -> entry.userId().equals(userId))
                .sorted(Comparator.comparing(LedgerEntry::occurredAt).reversed()).toList();
        return new PageResult<>(mine, page.page(), page.size(), mine.size());
    }

    @Override
    public Optional<String> planTitle(UUID planId) {
        return entries.stream().filter(entry -> entry.sourceId().equals(planId) && entry.planTitle() != null)
                .map(LedgerEntry::planTitle).findFirst();
    }

    @Override
    public void add(UUID userId, int points) {
        scores.merge(userId, points, Integer::sum);
    }

    @Override
    public Map<UUID, Integer> scoresOf(Collection<UUID> userIds) {
        return scores.entrySet().stream().filter(score -> userIds.contains(score.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public int rebuildFromLedger() {
        scores.clear();
        entries.forEach(entry -> add(entry.userId(), entry.points()));
        return scores.size();
    }

    @Override
    public boolean save(Kudos given) {
        var known = kudos.stream().anyMatch(other -> other.giverId().equals(given.giverId())
                && other.receiverId().equals(given.receiverId()) && other.planId().equals(given.planId()));
        return !known && kudos.add(given);
    }

    @Override
    public List<Kudos> givenBy(UUID giverId, UUID planId) {
        return kudos.stream().filter(given -> given.giverId().equals(giverId) && given.planId().equals(planId))
                .toList();
    }

    @Override
    public Map<KudosKind, Long> countsOf(UUID receiverId) {
        return kudos.stream().filter(given -> given.receiverId().equals(receiverId))
                .collect(Collectors.groupingBy(Kudos::kind, () -> new EnumMap<>(KudosKind.class),
                        Collectors.counting()));
    }
}

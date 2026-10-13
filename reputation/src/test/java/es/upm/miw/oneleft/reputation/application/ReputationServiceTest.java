package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.AttendanceFact;
import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.BadgeProgress;
import es.upm.miw.oneleft.reputation.domain.model.CancellationFact;
import es.upm.miw.oneleft.reputation.domain.model.CheckInFact;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.model.ReportFact;
import es.upm.miw.oneleft.reputation.domain.model.Reputation;
import es.upm.miw.oneleft.reputation.domain.model.TrackRecord;
import es.upm.miw.oneleft.reputation.domain.model.ValidationException;
import es.upm.miw.oneleft.reputation.domain.port.out.Badges;
import es.upm.miw.oneleft.reputation.domain.port.out.Ledger;
import es.upm.miw.oneleft.reputation.domain.port.out.Scores;
import es.upm.miw.oneleft.reputation.domain.port.out.TrackRecords;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-009: each event counts once, the score is the sum of the ledger and can be rebuilt from it. */
class ReputationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T16:30:00Z");
    private final InMemory store = new InMemory();
    private final List<String> notices = new ArrayList<>();
    private final ReputationService service = new ReputationService(store, store, store, store,
            (userId, badge, at) -> notices.add(userId + ":" + badge));
    private final UUID plan = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    @Test
    void anEventDeliveredTwiceCountsOnce() {
        var checkIn = new CheckInFact(plan, "Padel", lucia, NOW);

        service.record(checkIn);
        service.record(checkIn);

        assertThat(service.reputationOf(lucia)).isEqualTo(new Reputation(lucia, 10));
        assertThat(store.entries).hasSize(1);
    }

    @Test
    void theScoreIsTheSumOfTheLedgerAndTheLedgerExplainsItTheLatestFirst() {
        service.record(new CheckInFact(plan, "Padel", lucia, NOW));
        service.record(new AttendanceFact(plan, "Padel", "PADEL", ana, List.of(lucia), List.of(ana), true,
                NOW.plusSeconds(60)));
        service.record(new ReportFact(UUID.randomUUID(), ana, NOW.plusSeconds(120)));

        assertThat(service.reputationOf(ana).score()).isEqualTo(-15 + 20 - 50);
        assertThat(service.ledgerOf(ana, new PageQuery(0, 20)).items()).extracting(LedgerEntry::reason)
                .containsExactly(Reason.CONFIRMED_REPORT, Reason.NO_SHOW, Reason.ORGANIZED_HELD);
    }

    @Test
    void aCancellationForTheMinimumCostsNothing() {
        service.record(new CancellationFact(plan, "Padel", ana, NOW.plusSeconds(600), false, NOW));
        service.record(new CancellationFact(plan, "Padel", ana, NOW.plus(Duration.ofMinutes(30)), true, NOW));

        assertThat(service.reputationOf(ana).score()).isEqualTo(-10);
    }

    @Test
    void severalPeopleComeInTheOrderAskedAndWithoutEntriesTheyHaveZero() {
        service.record(new CheckInFact(plan, "Padel", lucia, NOW));

        assertThat(service.reputationsOf(List.of(ana, lucia, ana)))
                .containsExactly(new Reputation(ana, 0), new Reputation(lucia, 10));
        assertThatThrownBy(() -> service.reputationsOf(List.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("reputation.people"));
        var tooMany = IntStream.range(0, 51).mapToObj(i -> UUID.randomUUID()).toList();
        assertThatThrownBy(() -> service.reputationsOf(tooMany)).isInstanceOf(ValidationException.class);
    }

    @Test
    void theScoresAreRebuiltFromTheLedger() {
        service.record(new CheckInFact(plan, "Padel", lucia, NOW));
        store.scores.put(lucia, 999);

        assertThat(service.rebuildScores()).isEqualTo(1);

        assertThat(service.reputationOf(lucia).score()).isEqualTo(10);
    }

    private AttendanceFact heldPlan(String activity, List<UUID> attendees, List<UUID> absentees, int minute) {
        return new AttendanceFact(UUID.randomUUID(), "Plan", activity, ana, attendees, absentees, true,
                NOW.plusSeconds(60L * minute));
    }

    @Test
    void theFirstPlanOrganizedAndHeldIsABadgeWithANoticeOnce() {
        var held = heldPlan("PADEL", List.of(lucia), List.of(ana), 0);

        service.record(held);
        service.record(held);

        assertThat(notices).containsExactly(ana + ":FIRST_PLAN_ORGANIZED");
        assertThat(service.earnedBadgesOf(ana)).extracting(BadgeProgress::badge)
                .containsExactly(Badge.FIRST_PLAN_ORGANIZED);
        // Lucía arrived once: one of ten plans and one of ten activities
        assertThat(service.myBadges(lucia)).filteredOn(badge -> badge.badge() == Badge.TEN_PLANS_COMPLETED)
                .singleElement().satisfies(badge -> {
                    assertThat(badge.progress()).isEqualTo(1);
                    assertThat(badge.earned()).isFalse();
                });
    }

    @Test
    void tenActivitiesTenPlansAndAStreakThatANoShowBreaks() {
        var activities = List.of("PADEL", "FOOTBALL", "BASKETBALL", "TENNIS", "RUNNING", "CYCLING", "HIKING",
                "BOARD_GAMES", "CINEMA", "CONCERTS");
        for (var i = 0; i < activities.size(); i++) {
            service.record(heldPlan(activities.get(i), List.of(ana, lucia), List.of(), i));
        }
        service.record(heldPlan("PADEL", List.of(ana), List.of(lucia), 20));

        assertThat(notices).contains(lucia + ":TEN_ACTIVITIES", lucia + ":TEN_PLANS_COMPLETED");
        assertThat(service.myBadges(lucia)).filteredOn(badge -> badge.badge() == Badge.ALWAYS_PUNCTUAL)
                .singleElement().satisfies(badge -> assertThat(badge.progress()).isZero());
        assertThat(service.myBadges(ana)).filteredOn(badge -> badge.badge() == Badge.ALWAYS_PUNCTUAL)
                .singleElement().satisfies(badge -> assertThat(badge.progress()).isEqualTo(11));
    }

    @Test
    void aPlanNobodyArrivedAtGivesNoBadge() {
        service.record(new AttendanceFact(UUID.randomUUID(), "Plan", "PADEL", ana, List.of(), List.of(ana, lucia),
                true, NOW));

        assertThat(notices).isEmpty();
        assertThat(service.myBadges(ana)).allSatisfy(badge -> assertThat(badge.progress()).isZero());
    }

    /** The ledger and the scores in memory, with the same rules as the SQL (tested in ReputationIntegrationTest). */
    private static final class InMemory implements Ledger, Scores, TrackRecords, Badges {
        private final List<LedgerEntry> entries = new ArrayList<>();
        private final Map<UUID, Integer> scores = new HashMap<>();
        private final Map<String, Attendance> lines = new HashMap<>();
        private final Map<UUID, Map<Badge, Instant>> badges = new HashMap<>();

        @Override
        public void record(List<Attendance> attendances) {
            attendances.forEach(line -> lines.putIfAbsent(line.userId() + ">" + line.planId(), line));
        }

        @Override
        public TrackRecord of(UUID userId) {
            var mine = lines.values().stream().filter(line -> line.userId().equals(userId))
                    .sorted(Comparator.comparing(Attendance::closedAt).reversed()).toList();
            var streak = (int) mine.stream().filter(Attendance::held).takeWhile(Attendance::arrived).count();
            return new TrackRecord((int) mine.stream().filter(line -> line.organizer() && line.held()).count(),
                    (int) mine.stream().filter(Attendance::arrived).count(),
                    (int) mine.stream().filter(Attendance::arrived).map(Attendance::activity).distinct().count(),
                    streak, 0);
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
    }
}

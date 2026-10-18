package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.InMemoryReputation;
import es.upm.miw.oneleft.reputation.domain.model.AttendanceFact;
import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.BadgeProgress;
import es.upm.miw.oneleft.reputation.domain.model.CancellationFact;
import es.upm.miw.oneleft.reputation.domain.model.CheckInFact;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.model.ReportFact;
import es.upm.miw.oneleft.reputation.domain.model.Reputation;
import es.upm.miw.oneleft.reputation.domain.model.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-009: each event counts once, the score is the sum of the ledger and can be rebuilt from it. */
class ReputationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T16:30:00Z");
    private final InMemoryReputation store = new InMemoryReputation();
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
        // The statistics of the social profile (HU-048) come from the same plans
        var record = service.trackRecordOf(lucia);
        assertThat(record.completed()).isEqualTo(10);
        assertThat(record.activities()).isEqualTo(10);
        assertThat(service.trackRecordOf(ana).organizedHeld()).isEqualTo(11);
    }

    @Test
    void aPlanNobodyArrivedAtGivesNoBadge() {
        service.record(new AttendanceFact(UUID.randomUUID(), "Plan", "PADEL", ana, List.of(), List.of(ana, lucia),
                true, NOW));

        assertThat(notices).isEmpty();
        assertThat(service.myBadges(ana)).allSatisfy(badge -> assertThat(badge.progress()).isZero());
    }
}

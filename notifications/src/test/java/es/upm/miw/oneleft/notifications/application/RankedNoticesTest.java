package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.application.NearbyPlanNotifierTest.Batches;
import es.upm.miw.oneleft.notifications.application.NearbyPlanNotifierTest.Log;
import es.upm.miw.oneleft.notifications.application.NearbyPlanNotifierTest.Preferences;
import es.upm.miw.oneleft.notifications.application.NearbyPlanNotifierTest.Push;
import es.upm.miw.oneleft.notifications.application.NearbyPlanNotifierTest.Subscriptions;
import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.Candidate;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.NoticeHistory;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.QuietHours;
import es.upm.miw.oneleft.notifications.domain.model.RankingSettings;
import es.upm.miw.oneleft.notifications.domain.port.out.AcceptanceModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Notices ranked by the acceptance model and sent in batches (HU-012). */
class RankedNoticesTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    /** 16/11/2026 at 17:00 in Madrid */
    private static final Instant NOW = Instant.parse("2026-11-16T16:00:00Z");
    private static final UUID ANA = UUID.randomUUID();

    private final Preferences preferences = new Preferences();
    private final Log log = new Log();
    private final Batches batches = new Batches();
    private final Subscriptions subscriptions = new Subscriptions();
    private final Push push = new Push();
    private final List<NearbyPlanNotice> published = new ArrayList<>();
    private final Set<String> blocks = new HashSet<>();
    /** What the model answers (empty: it is down) and the candidates it was asked about. */
    private Optional<Map<UUID, Double>> answer = Optional.empty();
    private final List<Candidate> asked = new ArrayList<>();
    private final AcceptanceModel model = (plan, candidates, now) -> {
        asked.addAll(candidates);
        return answer;
    };
    private final List<UUID> people = new ArrayList<>();

    /** Six people at different distances from the courts: the first is the nearest. */
    @BeforeEach
    void setUp() {
        for (var i = 0; i < 6; i++) {
            var userId = UUID.randomUUID();
            people.add(userId);
            // Each about 1.1 km further south than the previous one, all inside a 20 km radius
            preferences.save(new NotificationPreferences(userId, true, 40.39 - 0.01 * i, -3.63, 20_000, Set.of(),
                    null, 5));
        }
    }

    private static PublishedPlan plan(Duration startsIn) {
        return new PublishedPlan(UUID.randomUUID(), ANA, Activity.PADEL, "Pádel", "Pistas", 40.39, -3.63,
                NOW.plus(startsIn), 2);
    }

    private NoticeBatchService service(RankingSettings settings, Instant now) {
        return new NoticeBatchService(batches, preferences, log, delivery(), blocked(), settings,
                Clock.fixed(now, MADRID));
    }

    private NoticeDelivery delivery() {
        return new NoticeDelivery(published::add, subscriptions, push);
    }

    private es.upm.miw.oneleft.notifications.domain.port.out.Blocks blocked() {
        return (userId, otherId) -> blocks.contains(userId + ">" + otherId) || blocks.contains(otherId + ">" + userId);
    }

    private NearbyPlanNotifier notifier(RankingSettings settings) {
        return new NearbyPlanNotifier(preferences, new NearbyPlanNotifierTest.Alerts(), log, delivery(), blocked(),
                new CandidateRanker(model, settings), batches, settings, Clock.fixed(NOW, MADRID));
    }

    private static RankingSettings byModel(int batchSize) {
        return new RankingSettings(true, batchSize, Duration.ofMinutes(3), Duration.ofMinutes(15));
    }

    private List<UUID> notified() {
        return published.stream().map(NearbyPlanNotice::userId).toList();
    }

    @Test
    void theLikeliestAreNotifiedFirstAndTheRestWait() {
        // The model prefers the farthest two over the nearest
        answer = Optional.of(Map.of(people.get(5), 0.9, people.get(4), 0.8, people.get(0), 0.3, people.get(1), 0.2,
                people.get(2), 0.1, people.get(3), 0.05));
        var plan = plan(Duration.ofHours(2));

        assertThat(notifier(byModel(2)).notifyNearby(plan)).isEqualTo(2);

        assertThat(notified()).containsExactlyInAnyOrder(people.get(5), people.get(4));
        assertThat(log.ranking.get(people.get(5) + ":" + plan.planId())).isEqualTo("model#0");
        assertThat(batches.plans.get(plan.planId()).people())
                .containsExactly(people.get(0), people.get(1), people.get(2), people.get(3));
        assertThat(batches.plans.get(plan.planId()).nextAt()).isEqualTo(NOW.plus(Duration.ofMinutes(3)));
    }

    @Test
    void theModelIsAskedWhatEachPersonDidWithTheirLatestNotices() {
        // One notice three days ago, accepted
        var earlier = UUID.randomUUID();
        log.record(people.get(0), earlier, NOW.minus(Duration.ofDays(3)), "model", 0);
        log.accepted(people.get(0), earlier, NOW.minus(Duration.ofDays(3)));
        answer = Optional.of(new HashMap<>());

        notifier(byModel(10)).notifyNearby(plan(Duration.ofHours(2)));

        assertThat(asked).hasSize(6);
        var first = asked.stream().filter(c -> c.userId().equals(people.get(0))).findFirst().orElseThrow();
        assertThat(first.history()).isEqualTo(new NoticeHistory(1, 1, 0));
        assertThat(first.history().rate()).isEqualTo(2 / 5.0);
        assertThat(asked.stream().filter(c -> c.userId().equals(people.get(1))).findFirst().orElseThrow().history())
                .isEqualTo(NoticeHistory.NONE);
        assertThat(first.distanceMeters()).isZero();
    }

    @Test
    void ifTheModelIsNotAvailableTheNearestGoFirst() {
        var plan = plan(Duration.ofHours(2));

        notifier(byModel(2)).notifyNearby(plan);

        assertThat(notified()).containsExactlyInAnyOrder(people.get(0), people.get(1));
        assertThat(log.ranking.get(people.get(0) + ":" + plan.planId())).isEqualTo("fallback#0");
    }

    @Test
    void theDistanceRuleNeverAsksTheModel() {
        var plan = plan(Duration.ofHours(2));

        notifier(new RankingSettings(false, 3, Duration.ofMinutes(3), Duration.ofMinutes(15))).notifyNearby(plan);

        assertThat(asked).isEmpty();
        assertThat(notified()).containsExactlyInAnyOrder(people.get(0), people.get(1), people.get(2));
        assertThat(log.ranking.get(people.get(0) + ":" + plan.planId())).isEqualTo("distance#0");
    }

    @Test
    void whenEveryoneFitsInTheFirstBatchNothingIsLeftToSchedule() {
        notifier(byModel(10)).notifyNearby(plan(Duration.ofHours(2)));

        assertThat(published).hasSize(6);
        assertThat(batches.plans).isEmpty();
    }

    @Test
    void theNextBatchesGoOutOnTimeUntilNobodyIsLeft() {
        var settings = byModel(2);
        var plan = plan(Duration.ofHours(2));
        notifier(settings).notifyNearby(plan);
        published.clear();

        assertThat(service(settings, NOW.plus(Duration.ofMinutes(2))).sendDue()).as("not due yet").isZero();
        assertThat(service(settings, NOW.plus(Duration.ofMinutes(3))).sendDue()).isEqualTo(2);
        assertThat(batches.plans.get(plan.planId()).batched().nextBatch()).isEqualTo(2);
        assertThat(service(settings, NOW.plus(Duration.ofMinutes(6))).sendDue()).isEqualTo(2);

        assertThat(published).hasSize(4);
        assertThat(batches.plans).as("everyone has been notified").isEmpty();
        assertThat(log.ranking.get(people.get(2) + ":" + plan.planId())).isEqualTo("fallback#1");
        assertThat(log.ranking.get(people.get(4) + ":" + plan.planId())).isEqualTo("fallback#2");
    }

    @Test
    void aFullPlanOrACancelledOneGetsNoMoreBatches() {
        var settings = byModel(2);
        var plan = plan(Duration.ofHours(2));
        var otherPlan = plan(Duration.ofHours(3));
        notifier(settings).notifyNearby(plan);
        notifier(settings).notifyNearby(otherPlan);
        published.clear();
        var tracker = new AcceptanceTracker(log, batches);

        tracker.joined(plan.planId(), people.get(0), true, NOW.plusSeconds(60));
        tracker.closed(otherPlan.planId());

        assertThat(service(settings, NOW.plus(Duration.ofMinutes(10))).sendDue()).isZero();
        assertThat(published).isEmpty();
        assertThat(log.acceptedAt).containsKey(people.get(0) + ":" + plan.planId());
    }

    @Test
    void aJoinOnlyCountsIfThePersonHadBeenNotified() {
        var plan = plan(Duration.ofHours(2));
        notifier(byModel(2)).notifyNearby(plan);
        var tracker = new AcceptanceTracker(log, batches);

        tracker.joined(plan.planId(), people.get(5), false, NOW.plusSeconds(60));
        tracker.joined(plan.planId(), people.get(0), false, NOW.plusSeconds(90));

        assertThat(log.acceptedAt).containsOnlyKeys(people.get(0) + ":" + plan.planId());
        assertThat(batches.plans).as("it still has free spots").containsKey(plan.planId());
    }

    @Test
    void noBatchIsSentInTheLastMinutesBeforeThePlanStarts() {
        var settings = byModel(2);
        var plan = plan(Duration.ofMinutes(20));
        notifier(settings).notifyNearby(plan);
        published.clear();

        // 3 minutes later the plan starts in 17 minutes: still fine; 6 minutes later, in 14: it is too late
        assertThat(service(settings, NOW.plus(Duration.ofMinutes(3))).sendDue()).isEqualTo(2);
        assertThat(service(settings, NOW.plus(Duration.ofMinutes(6))).sendDue()).isZero();
        assertThat(batches.plans).isEmpty();
    }

    @Test
    void eachPersonIsCheckedAgainWhenTheirBatchIsSent() {
        var settings = byModel(4);
        var plan = plan(Duration.ofHours(2));
        notifier(settings).notifyNearby(plan);
        published.clear();
        // After the plan was published: one of the remaining two turns notices off, the other is blocked by Ana
        preferences.save(new NotificationPreferences(people.get(4), false, null, null, 3_000, Set.of(), null, 5));
        blocks.add(ANA + ">" + people.get(5));

        assertThat(service(settings, NOW.plus(Duration.ofMinutes(3))).sendDue()).isZero();
        assertThat(published).isEmpty();
        assertThat(batches.plans).isEmpty();
    }

    @Test
    void quietHoursAndTheDailyLimitHoldBackALaterBatch() {
        var settings = byModel(4);
        var plan = plan(Duration.ofHours(2));
        notifier(settings).notifyNearby(plan);
        published.clear();
        // 17:03 local time: the fifth person is quiet from 17:00 to 18:00, the sixth reached the limit today
        preferences.save(new NotificationPreferences(people.get(4), true, 40.35, -3.63, 20_000, Set.of(),
                new QuietHours(LocalTime.of(17, 0), LocalTime.of(18, 0)), 5));
        preferences.save(new NotificationPreferences(people.get(5), true, 40.34, -3.63, 20_000, Set.of(), null, 1));
        log.record(people.get(5), UUID.randomUUID(), NOW.minus(Duration.ofHours(1)), "model", 0);

        assertThat(service(settings, NOW.plus(Duration.ofMinutes(3))).sendDue()).isZero();
        assertThat(published).isEmpty();
    }
}

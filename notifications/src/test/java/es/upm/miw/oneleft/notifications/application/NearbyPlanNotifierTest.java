package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.BatchedPlan;
import es.upm.miw.oneleft.notifications.domain.model.Level;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.NoticeHistory;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.model.PlanCancellation;
import es.upm.miw.oneleft.notifications.domain.model.PlanReminder;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;
import es.upm.miw.oneleft.notifications.domain.model.RankedCandidate;
import es.upm.miw.oneleft.notifications.domain.model.RankingSettings;
import es.upm.miw.oneleft.notifications.domain.model.QuietHours;
import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;
import es.upm.miw.oneleft.notifications.domain.port.out.AlertRepository;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeBatches;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeLog;
import es.upm.miw.oneleft.notifications.domain.port.out.PreferencesRepository;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSender;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NearbyPlanNotifierTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    /** 16/11/2026 at 17:00 in Madrid */
    private static final Instant NOW = Instant.parse("2026-11-16T16:00:00Z");
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();

    private final Preferences preferences = new Preferences();
    private final Alerts alerts = new Alerts();
    private final Log log = new Log();
    private final List<NearbyPlanNotice> published = new ArrayList<>();
    private final Subscriptions subscriptions = new Subscriptions();
    private final Push push = new Push();
    /** Who blocked whom (HU-033): "blocker>blocked" */
    private final Set<String> blocks = new HashSet<>();
    private NearbyPlanNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = at(NOW);
    }

    private NearbyPlanNotifier at(Instant now) {
        // Before HU-012: everyone at once, nearest first
        var settings = RankingSettings.everyoneAtOnce();
        return new NearbyPlanNotifier(preferences, alerts, log, new NoticeDelivery(published::add, subscriptions, push),
                (userId, otherId) -> blocks.contains(userId + ">" + otherId) || blocks.contains(otherId + ">" + userId),
                new CandidateRanker((plan, candidates, at) -> Optional.empty(), settings), new Batches(), settings,
                Clock.fixed(now, MADRID));
    }

    private static PublishedPlan padel(Duration startsIn) {
        return new PublishedPlan(UUID.randomUUID(), ANA, Activity.PADEL, "Pádel 2 contra 2, falta uno",
                "Pistas de la Albufera", 40.3964, -3.6297, NOW.plus(startsIn), 1);
    }

    private static NotificationPreferences wantsEverything(UUID userId, QuietHours quiet, int maxPerDay) {
        return new NotificationPreferences(userId, true, 40.39, -3.63, 3_000, Set.of(), quiet, maxPerDay);
    }

    @Test
    void notifiesThePeopleWhoWantThePlanInTheAppAndInTheirBrowsers() {
        preferences.save(wantsEverything(LUCIA, null, 5));
        preferences.save(new NotificationPreferences(DIEGO, true, 40.39, -3.63, 3_000, Set.of(Activity.CINEMA), null, 5));
        preferences.save(wantsEverything(ANA, null, 5));
        subscriptions.save(new PushSubscription(LUCIA, "https://push.example/lucia", "k", "a", "es"));
        var plan = padel(Duration.ofHours(1));

        assertThat(notifier.notifyNearby(plan)).isEqualTo(1);

        assertThat(published).singleElement().satisfies(notice -> {
            assertThat(notice.userId()).isEqualTo(LUCIA);
            assertThat(notice.planId()).isEqualTo(plan.planId());
            assertThat(notice.distanceMeters()).isEqualTo(700);
        });
        assertThat(push.sent).containsExactly("https://push.example/lucia");
    }

    @Test
    void eachPersonHearsAboutAPlanOnce() {
        preferences.save(wantsEverything(LUCIA, null, 5));
        var plan = padel(Duration.ofHours(1));

        notifier.notifyNearby(plan);

        assertThat(notifier.notifyNearby(plan)).isZero();
        assertThat(published).hasSize(1);
    }

    @Test
    void respectsTheQuietHoursInLocalTime() {
        // 17:00 in Madrid (16:00 UTC): quiet from 16:30 to 18:00 local time
        preferences.save(wantsEverything(LUCIA, new QuietHours(LocalTime.of(16, 30), LocalTime.of(18, 0)), 5));

        assertThat(notifier.notifyNearby(padel(Duration.ofHours(2)))).isZero();
        assertThat(at(NOW.plus(Duration.ofHours(1))).notifyNearby(padel(Duration.ofHours(3)))).isEqualTo(1);
    }

    @Test
    void neverSendsMoreThanTheDailyLimitAndStartsAgainTheNextDay() {
        preferences.save(wantsEverything(LUCIA, null, 2));

        notifier.notifyNearby(padel(Duration.ofHours(1)));
        notifier.notifyNearby(padel(Duration.ofHours(2)));

        assertThat(notifier.notifyNearby(padel(Duration.ofHours(3)))).isZero();
        assertThat(published).hasSize(2);
        // Next morning in Madrid
        var tomorrow = Instant.parse("2026-11-17T08:00:00Z");
        assertThat(at(tomorrow).notifyNearby(new PublishedPlan(UUID.randomUUID(), ANA, Activity.RUNNING, "Rodaje",
                "Parque", 40.39, -3.63, tomorrow.plus(Duration.ofHours(1)), 3))).isEqualTo(1);
    }

    @Test
    void plansThatHaveStartedOrAreFullAreNotAnnounced() {
        preferences.save(wantsEverything(LUCIA, null, 5));

        assertThat(notifier.notifyNearby(padel(Duration.ofMinutes(-5)))).isZero();
        var full = new PublishedPlan(UUID.randomUUID(), ANA, Activity.PADEL, "Completo", "Pistas", 40.39, -3.63,
                NOW.plus(Duration.ofHours(1)), 0);
        assertThat(notifier.notifyNearby(full)).isZero();
        assertThat(published).isEmpty();
    }

    @Test
    void forgetsTheBrowsersThatDroppedTheSubscription() {
        preferences.save(wantsEverything(LUCIA, null, 5));
        subscriptions.save(new PushSubscription(LUCIA, "https://push.example/old", "k", "a", "es"));
        subscriptions.save(new PushSubscription(LUCIA, "https://push.example/new", "k", "a", "es"));
        push.gone.add("https://push.example/old");

        notifier.notifyNearby(padel(Duration.ofHours(1)));

        assertThat(subscriptions.findByUser(LUCIA)).extracting(PushSubscription::endpoint)
                .containsExactly("https://push.example/new");
    }

    @Test
    void withoutWebPushOnlyTheInAppNoticeIsSent() {
        preferences.save(wantsEverything(LUCIA, null, 5));
        subscriptions.save(new PushSubscription(LUCIA, "https://push.example/lucia", "k", "a", "es"));
        push.enabled = false;

        assertThat(notifier.notifyNearby(padel(Duration.ofHours(1)))).isEqualTo(1);

        assertThat(published).hasSize(1);
        assertThat(push.sent).isEmpty();
    }

    @Test
    void preferencesServiceGivesTheDefaultsAndSavesChanges() {
        var service = new PreferencesService(preferences, subscriptions);
        assertThat(service.preferencesOf(LUCIA).enabled()).isFalse();

        service.update(wantsEverything(LUCIA, null, 5));
        service.subscribe(new PushSubscription(LUCIA, "https://push.example/lucia", "k", "a", "en"));

        assertThat(service.preferencesOf(LUCIA).enabled()).isTrue();
        assertThat(subscriptions.findByUser(LUCIA)).hasSize(1);
        service.unsubscribe(DIEGO, "https://push.example/lucia");
        assertThat(subscriptions.findByUser(LUCIA)).as("only its owner can remove it").hasSize(1);
        service.unsubscribe(LUCIA, "https://push.example/lucia");
        assertThat(subscriptions.findByUser(LUCIA)).isEmpty();
    }

    /** «Padel after work» around the courts: Monday to Friday from 17:00 to 21:00 (HU-036). */
    private static SavedAlert afterWork(UUID userId, Level level) {
        return new SavedAlert(UUID.randomUUID(), userId, "Pádel al salir", Set.of(Activity.PADEL), level, 40.39,
                -3.63, 2_000, EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime.of(17, 0),
                LocalTime.of(21, 0));
    }

    @Test
    void aSavedAlertThatMatchesThePlanNotifiesItsOwnerOnce() {
        // Two alerts and the notices of HU-006, all for the same plan: Lucía hears about it once
        alerts.save(afterWork(LUCIA, Level.INTERMEDIATE));
        alerts.save(afterWork(LUCIA, null));
        preferences.save(wantsEverything(LUCIA, null, 5));
        // Diego has no notices of HU-006: the alert works with the default limits
        alerts.save(afterWork(DIEGO, null));
        var plan = new PublishedPlan(UUID.randomUUID(), ANA, Activity.PADEL, "Pádel 2 contra 2, falta uno",
                "Pistas de la Albufera", 40.3964, -3.6297, NOW.plus(Duration.ofHours(1)), 1, Level.INTERMEDIATE);

        assertThat(notifier.notifyNearby(plan)).isEqualTo(2);

        assertThat(published).extracting(NearbyPlanNotice::userId).containsExactlyInAnyOrder(LUCIA, DIEGO);
        assertThat(published).filteredOn(notice -> notice.userId().equals(DIEGO)).singleElement()
                .satisfies(notice -> assertThat(notice.distanceMeters()).isEqualTo(700));
    }

    @Test
    void nobodyHearsAboutAPlanOfSomeoneTheyBlockedOrWhoBlockedThem() {
        // Lucía blocked Ana; Ana blocked Diego, who has an alert (HU-033)
        preferences.save(wantsEverything(LUCIA, null, 5));
        alerts.save(afterWork(DIEGO, null));
        blocks.add(LUCIA + ">" + ANA);
        blocks.add(ANA + ">" + DIEGO);
        var plan = new PublishedPlan(UUID.randomUUID(), ANA, Activity.PADEL, "Pádel 2 contra 2, falta uno",
                "Pistas de la Albufera", 40.3964, -3.6297, NOW.plus(Duration.ofHours(1)), 1);

        assertThat(notifier.notifyNearby(plan)).isZero();

        assertThat(published).isEmpty();
        assertThat(push.sent).isEmpty();
    }

    @Test
    void anAlertOnlyNotifiesThePlansItLooksFor() {
        alerts.save(afterWork(DIEGO, Level.ADVANCED));
        alerts.save(afterWork(LUCIA, null));

        // Intermediate: not for Diego's advanced alert; at 22:00 on a Monday: outside Lucía's hours
        var late = new PublishedPlan(UUID.randomUUID(), ANA, Activity.PADEL, "Pádel tarde", "Pistas", 40.3964,
                -3.6297, NOW.plus(Duration.ofHours(5)), 1, Level.INTERMEDIATE);
        assertThat(notifier.notifyNearby(late)).isZero();

        // The same in the afternoon: Lucía; and a plan for any level reaches Diego too
        var anyLevel = new PublishedPlan(UUID.randomUUID(), ANA, Activity.PADEL, "Pádel", "Pistas", 40.3964, -3.6297,
                NOW.plus(Duration.ofHours(1)), 1, null);
        assertThat(notifier.notifyNearby(anyLevel)).isEqualTo(2);
    }

    @Test
    void theQuietHoursOfThePersonAlsoSilenceTheirAlerts() {
        alerts.save(afterWork(LUCIA, null));
        preferences.save(new NotificationPreferences(LUCIA, false, null, null, 3_000, Set.of(),
                new QuietHours(LocalTime.of(16, 0), LocalTime.of(18, 0)), 5));

        assertThat(notifier.notifyNearby(padel(Duration.ofHours(1)))).isZero();
    }

    /** Also used by {@link AlertServiceTest}. */
    static final class Alerts implements AlertRepository {
        final Map<UUID, SavedAlert> all = new java.util.LinkedHashMap<>();

        @Override
        public List<SavedAlert> findByUser(UUID userId) {
            return all.values().stream().filter(alert -> alert.userId().equals(userId)).toList();
        }

        @Override
        public Optional<SavedAlert> findById(UUID alertId) {
            return Optional.ofNullable(all.get(alertId));
        }

        @Override
        public long countByUser(UUID userId) {
            return findByUser(userId).size();
        }

        @Override
        public SavedAlert save(SavedAlert alert) {
            all.put(alert.id(), alert);
            return alert;
        }

        @Override
        public void delete(UUID alertId) {
            all.remove(alertId);
        }

        @Override
        public List<SavedAlert> findFor(Activity activity) {
            return all.values().stream()
                    .filter(alert -> alert.activities().isEmpty() || alert.activities().contains(activity)).toList();
        }
    }

    static final class Preferences implements PreferencesRepository {
        private final Map<UUID, NotificationPreferences> all = new HashMap<>();

        @Override
        public Optional<NotificationPreferences> findByUser(UUID userId) {
            return Optional.ofNullable(all.get(userId));
        }

        @Override
        public NotificationPreferences save(NotificationPreferences preferences) {
            all.put(preferences.userId(), preferences);
            return preferences;
        }

        @Override
        public List<NotificationPreferences> findEnabledFor(Activity activity) {
            return all.values().stream().filter(NotificationPreferences::enabled)
                    .filter(p -> p.activities().isEmpty() || p.activities().contains(activity)).toList();
        }
    }

    /** Also used by the tests of the batches (HU-012). */
    static final class Log implements NoticeLog {
        private final Map<String, Instant> sent = new HashMap<>();
        final Map<String, String> ranking = new HashMap<>();
        final Map<String, Instant> acceptedAt = new HashMap<>();

        @Override
        public boolean record(UUID userId, UUID planId, Instant at, String how, int batch) {
            ranking.putIfAbsent(userId + ":" + planId, how + "#" + batch);
            return sent.putIfAbsent(userId + ":" + planId, at) == null;
        }

        @Override
        public void accepted(UUID userId, UUID planId, Instant at) {
            if (sent.containsKey(userId + ":" + planId)) {
                acceptedAt.putIfAbsent(userId + ":" + planId, at);
            }
        }

        @Override
        public Map<UUID, NoticeHistory> historyOf(Collection<UUID> userIds, Instant since, Instant startOfDay) {
            var history = new HashMap<UUID, NoticeHistory>();
            for (var userId : userIds) {
                var mine = sent.entrySet().stream()
                        .filter(e -> e.getKey().startsWith(userId + ":") && !e.getValue().isBefore(since)).toList();
                if (!mine.isEmpty()) {
                    history.put(userId, new NoticeHistory(mine.size(),
                            mine.stream().filter(e -> acceptedAt.containsKey(e.getKey())).count(),
                            mine.stream().filter(e -> !e.getValue().isBefore(startOfDay)).count()));
                }
            }
            return history;
        }

        @Override
        public long countSince(UUID userId, Instant since) {
            return sent.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(userId + ":") && !e.getValue().isBefore(since)).count();
        }
    }

    /** Also used by {@link PlanReminderNotifierTest} and {@link PlanCancellationNotifierTest}. */
    static final class Subscriptions implements PushSubscriptionRepository {
        final Map<String, PushSubscription> all = new java.util.LinkedHashMap<>();

        @Override
        public void save(PushSubscription subscription) {
            all.put(subscription.endpoint(), subscription);
        }

        @Override
        public List<PushSubscription> findByUser(UUID userId) {
            return all.values().stream().filter(s -> s.userId().equals(userId)).toList();
        }

        @Override
        public void delete(String endpoint) {
            all.remove(endpoint);
        }

        @Override
        public void delete(UUID userId, String endpoint) {
            var subscription = all.get(endpoint);
            if (subscription != null && subscription.userId().equals(userId)) {
                all.remove(endpoint);
            }
        }
    }

    /** The people still to be notified of each plan, in memory. */
    static final class Batches implements NoticeBatches {
        record Waiting(BatchedPlan batched, Instant nextAt, List<UUID> people) {
        }

        final Map<UUID, Waiting> plans = new HashMap<>();

        @Override
        public void schedule(PublishedPlan plan, String ranking, List<RankedCandidate> rest, Instant nextBatchAt) {
            plans.put(plan.planId(), new Waiting(new BatchedPlan(plan, ranking, 1), nextBatchAt,
                    new ArrayList<>(rest.stream().map(r -> r.candidate().userId()).toList())));
        }

        @Override
        public List<BatchedPlan> due(Instant now) {
            return plans.values().stream().filter(w -> !w.nextAt().isAfter(now)).map(Waiting::batched).toList();
        }

        @Override
        public List<UUID> takeNext(UUID planId, int limit) {
            var people = plans.get(planId).people();
            var next = new ArrayList<>(people.subList(0, Math.min(limit, people.size())));
            people.removeAll(next);
            return next;
        }

        @Override
        public void rescheduleOrFinish(UUID planId, Instant nextBatchAt) {
            var waiting = plans.get(planId);
            if (waiting.people().isEmpty()) {
                plans.remove(planId);
            } else {
                var batched = waiting.batched();
                plans.put(planId, new Waiting(new BatchedPlan(batched.plan(), batched.ranking(),
                        batched.nextBatch() + 1), nextBatchAt, waiting.people()));
            }
        }

        @Override
        public void close(UUID planId) {
            plans.remove(planId);
        }
    }

    /** Also used by {@link PlanReminderNotifierTest}. */
    static final class Push implements PushSender {
        boolean enabled = true;
        final List<String> sent = new ArrayList<>();
        final Set<String> gone = new java.util.HashSet<>();

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public Result send(PushSubscription subscription, NearbyPlanNotice notice) {
            if (gone.contains(subscription.endpoint())) {
                return Result.GONE;
            }
            sent.add(subscription.endpoint());
            return Result.SENT;
        }

        @Override
        public Result send(PushSubscription subscription, PlanReminder reminder) {
            if (gone.contains(subscription.endpoint())) {
                return Result.GONE;
            }
            sent.add("reminder:" + subscription.endpoint());
            return Result.SENT;
        }

        @Override
        public Result send(PushSubscription subscription, PlanCancellation cancellation) {
            if (gone.contains(subscription.endpoint())) {
                return Result.GONE;
            }
            sent.add("cancelled:" + subscription.endpoint());
            return Result.SENT;
        }
    }
}

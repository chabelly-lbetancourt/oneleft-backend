package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;
import es.upm.miw.oneleft.notifications.domain.model.QuietHours;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeLog;
import es.upm.miw.oneleft.notifications.domain.port.out.PreferencesRepository;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSender;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final Log log = new Log();
    private final List<NearbyPlanNotice> published = new ArrayList<>();
    private final Subscriptions subscriptions = new Subscriptions();
    private final Push push = new Push();
    private NearbyPlanNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = at(NOW);
    }

    private NearbyPlanNotifier at(Instant now) {
        return new NearbyPlanNotifier(preferences, log, published::add, subscriptions, push, Clock.fixed(now, MADRID));
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

    private static final class Preferences implements PreferencesRepository {
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

    private static final class Log implements NoticeLog {
        private final Map<String, Instant> sent = new HashMap<>();

        @Override
        public boolean record(UUID userId, UUID planId, Instant at) {
            return sent.putIfAbsent(userId + ":" + planId, at) == null;
        }

        @Override
        public long countSince(UUID userId, Instant since) {
            return sent.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(userId + ":") && !e.getValue().isBefore(since)).count();
        }
    }

    private static final class Subscriptions implements PushSubscriptionRepository {
        private final Map<String, PushSubscription> all = new java.util.LinkedHashMap<>();

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

    private static final class Push implements PushSender {
        private boolean enabled = true;
        private final List<String> sent = new ArrayList<>();
        private final Set<String> gone = new java.util.HashSet<>();

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
    }
}

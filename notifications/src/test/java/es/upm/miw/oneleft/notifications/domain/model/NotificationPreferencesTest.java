package es.upm.miw.oneleft.notifications.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationPreferencesTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID ANA = UUID.randomUUID();
    /** Pistas de la Albufera, in Vallecas */
    private static final PublishedPlan PADEL = new PublishedPlan(UUID.randomUUID(), ANA, Activity.PADEL,
            "Pádel 2 contra 2, falta uno", "Pistas de la Albufera", 40.3964, -3.6297,
            Instant.parse("2026-11-16T18:00:00Z"), 1);

    private static NotificationPreferences near(Set<Activity> activities, int radius) {
        return new NotificationPreferences(LUCIA, true, 40.39, -3.63, radius, activities, null, 5);
    }

    @Test
    void untilThePersonChoosesNoticesAreOff() {
        var defaults = NotificationPreferences.defaults(LUCIA);
        assertThat(defaults.enabled()).isFalse();
        assertThat(defaults.radiusMeters()).isEqualTo(3_000);
        assertThat(defaults.activities()).isEmpty();
        assertThat(defaults.quietHours()).isEqualTo(new QuietHours(LocalTime.of(23, 0), LocalTime.of(8, 0)));
        assertThat(defaults.maxPerDay()).isEqualTo(5);
        assertThat(defaults.wants(PADEL)).isFalse();
    }

    @Test
    void theZoneIsKeptApproximate() {
        var preferences = new NotificationPreferences(LUCIA, true, 40.39641, -3.62974, 3_000, Set.of(), null, 5);
        assertThat(preferences.latitude()).isEqualTo(40.40);
        assertThat(preferences.longitude()).isEqualTo(-3.63);
    }

    @Test
    void wantsNearbyPlansOfItsActivities() {
        assertThat(near(Set.of(), 3_000).wants(PADEL)).isTrue();
        assertThat(near(Set.of(Activity.PADEL, Activity.TENNIS), 3_000).wants(PADEL)).isTrue();
        assertThat(near(Set.of(Activity.CINEMA), 3_000).wants(PADEL)).isFalse();
        // The plan is about 700 m away from the approximate zone
        assertThat(near(Set.of(), 500).wants(PADEL)).isFalse();
        assertThat(near(Set.of(), 3_000).distanceTo(PADEL)).isBetween(500.0, 1_000.0);
    }

    @Test
    void nobodyIsToldAboutTheirOwnPlan() {
        var ana = new NotificationPreferences(ANA, true, 40.39, -3.63, 3_000, Set.of(), null, 5);
        assertThat(ana.wants(PADEL)).isFalse();
    }

    @Test
    void quietHoursAreOptional() {
        assertThat(near(Set.of(), 3_000).isQuietAt(LocalTime.of(3, 0))).isFalse();
        assertThat(NotificationPreferences.defaults(LUCIA).isQuietAt(LocalTime.of(3, 0))).isTrue();
    }

    @Test
    void rejectsInvalidPreferencesWithStableCodes() {
        assertThatThrownBy(() -> new NotificationPreferences(LUCIA, true, null, null, 3_000, Set.of(), null, 5))
                .hasFieldOrPropertyWithValue("code", "notifications.zoneRequired");
        assertThatThrownBy(() -> new NotificationPreferences(LUCIA, false, 95.0, -3.63, 3_000, Set.of(), null, 5))
                .hasFieldOrPropertyWithValue("code", "notifications.zone");
        assertThatThrownBy(() -> new NotificationPreferences(LUCIA, false, 40.39, null, 3_000, Set.of(), null, 5))
                .hasFieldOrPropertyWithValue("code", "notifications.zone");
        assertThatThrownBy(() -> new NotificationPreferences(LUCIA, false, 40.39, 200.0, 3_000, Set.of(), null, 5))
                .hasFieldOrPropertyWithValue("code", "notifications.zone");
        assertThatThrownBy(() -> new NotificationPreferences(LUCIA, false, null, null, 100, Set.of(), null, 5))
                .hasFieldOrPropertyWithValue("code", "notifications.radius");
        assertThatThrownBy(() -> new NotificationPreferences(LUCIA, false, null, null, 30_000, Set.of(), null, 5))
                .hasFieldOrPropertyWithValue("code", "notifications.radius");
        assertThatThrownBy(() -> new NotificationPreferences(LUCIA, false, null, null, 3_000, Set.of(), null, 0))
                .hasFieldOrPropertyWithValue("code", "notifications.maxPerDay");
        assertThatThrownBy(() -> new NotificationPreferences(LUCIA, false, null, null, 3_000, Set.of(), null, 21))
                .hasFieldOrPropertyWithValue("code", "notifications.maxPerDay");
    }

    @Test
    void theNoticeCarriesTheDistanceRoundedTo100Metres() {
        var notice = NearbyPlanNotice.of(LUCIA, PADEL, 742.3);
        assertThat(notice.distanceMeters()).isEqualTo(700);
        assertThat(notice.userId()).isEqualTo(LUCIA);
        assertThat(notice.title()).isEqualTo(PADEL.title());
    }

    @Test
    void pushSubscriptionsNeedHttpsAndKeys() {
        assertThatThrownBy(() -> new PushSubscription(LUCIA, "http://push.example/1", "k", "a", "es"))
                .hasFieldOrPropertyWithValue("code", "notifications.pushEndpoint");
        assertThatThrownBy(() -> new PushSubscription(LUCIA, null, "k", "a", "es"))
                .hasFieldOrPropertyWithValue("code", "notifications.pushEndpoint");
        assertThatThrownBy(() -> new PushSubscription(LUCIA, "https://push.example/1", " ", "a", "es"))
                .hasFieldOrPropertyWithValue("code", "notifications.pushKeys");
        assertThatThrownBy(() -> new PushSubscription(LUCIA, "https://push.example/1", "k", null, "es"))
                .hasFieldOrPropertyWithValue("code", "notifications.pushKeys");
        assertThat(new PushSubscription(LUCIA, "https://push.example/1", "k", "a", "fr").language()).isEqualTo("es");
        assertThat(new PushSubscription(LUCIA, "https://push.example/1", "k", "a", "en").language()).isEqualTo("en");
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NearbySearchTest {

    private static final double LAT = 40.3912;
    private static final double LON = -3.6287;
    private static final UUID ME = UUID.randomUUID();

    private static NearbySearch search(Set<Activity> activities, Duration within) {
        return new NearbySearch(LAT, LON, 2_000, activities, within, ME);
    }

    private static PlanPublished event(Activity activity, UUID organizer, double lat, Duration startsIn, int free) {
        return new PlanPublished(UUID.randomUUID(), organizer, activity, lat, LON, NOW.plus(startsIn), free,
                Level.INTERMEDIATE, NOW);
    }

    @Test
    void defaultsToAllActivitiesAndTheWholeHorizon() {
        var search = new NearbySearch(LAT, LON, NearbySearch.DEFAULT_RADIUS_METERS, null, null, null);
        assertThat(search.activities()).isEmpty();
        assertThat(search.startsWithin()).isEqualTo(Plan.MAX_HORIZON);
        assertThat(search.includes(Activity.CINEMA)).isTrue();
        assertThat(search.until(NOW)).isEqualTo(NOW.plus(Duration.ofHours(12)));
    }

    @Test
    void rejectsOutOfRangeParameters() {
        assertThatThrownBy(() -> new NearbySearch(91, LON, 1_000, null, null, ME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NearbySearch(LAT, 181, 1_000, null, null, ME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NearbySearch(LAT, LON, 499, null, null, ME))
                .hasMessage("The radius must be between 500 m and 25 km");
        assertThatThrownBy(() -> new NearbySearch(LAT, LON, 25_001, null, null, ME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NearbySearch(LAT, LON, 1_000, null, Duration.ofMinutes(30), ME))
                .hasMessage("Only plans starting in the next 1 to 12 hours can be searched");
        assertThatThrownBy(() -> new NearbySearch(LAT, LON, 1_000, null, Duration.ofHours(13), ME))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activitiesAreCopied() {
        var activities = new java.util.HashSet<>(Set.of(Activity.PADEL));
        var search = search(activities, null);
        activities.add(Activity.CINEMA);
        assertThat(search.activities()).containsExactly(Activity.PADEL);
        assertThat(search.includes(Activity.CINEMA)).isFalse();
    }

    @Test
    void matchesAPlanNearbyThatStartsSoonWithFreeSpots() {
        var search = search(Set.of(Activity.PADEL), Duration.ofHours(3));
        var other = UUID.randomUUID();

        // ~1.1 km north
        assertThat(search.matches(event(Activity.PADEL, other, LAT + 0.01, Duration.ofHours(1), 1), NOW)).isTrue();
        assertThat(search.matches(event(Activity.PADEL, other, LAT + 0.01, Duration.ofHours(3), 1), NOW)).isTrue();
    }

    @Test
    void doesNotMatchPlansOutsideTheSearch() {
        var search = search(Set.of(Activity.PADEL), Duration.ofHours(3));
        var other = UUID.randomUUID();

        assertThat(search.matches(event(Activity.CINEMA, other, LAT, Duration.ofHours(1), 1), NOW))
                .as("another activity").isFalse();
        assertThat(search.matches(event(Activity.PADEL, ME, LAT, Duration.ofHours(1), 1), NOW))
                .as("own plan").isFalse();
        assertThat(search.matches(event(Activity.PADEL, other, LAT, Duration.ofHours(1), 0), NOW))
                .as("no free spots").isFalse();
        assertThat(search.matches(event(Activity.PADEL, other, LAT, Duration.ofHours(4), 1), NOW))
                .as("starts too late").isFalse();
        assertThat(search.matches(event(Activity.PADEL, other, LAT, Duration.ofMinutes(-1), 1), NOW))
                .as("already started").isFalse();
        assertThat(search.matches(event(Activity.PADEL, other, LAT + 0.02, Duration.ofHours(1), 1), NOW))
                .as("2.2 km away").isFalse();
    }
}

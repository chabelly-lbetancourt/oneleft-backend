package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-026: which plans show the weather and when rain is highlighted. */
class ForecastTest {

    @Test
    void rainIsLikelyFromSixtyPerCent() {
        var time = Instant.parse("2026-10-08T18:00:00Z");
        assertThat(new Forecast(time, 17.4, 59, 10).rainLikely()).isFalse();
        assertThat(new Forecast(time, 17.4, 60, 10).rainLikely()).isTrue();
    }

    @Test
    void onlyOutdoorActivitiesShowTheWeather() {
        // The whole list of outdoor activities (HU-068) is checked in ActivityTest
        assertThat(Activity.values()).filteredOn(Activity::outdoor).contains(Activity.PADEL, Activity.FOOTBALL,
                Activity.BASKETBALL, Activity.TENNIS, Activity.RUNNING, Activity.CYCLING, Activity.HIKING)
                .doesNotContain(Activity.BOARD_GAMES, Activity.CINEMA, Activity.CONCERTS, Activity.OTHER);
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-068: the catalog of activities, grouped by category, and what is outdoor. */
class ActivityTest {

    @Test
    void everyCategoryHasItsActivitiesAndAnActivityIsInOneCategoryOnly() {
        var all = Arrays.stream(ActivityCategory.values()).flatMap(category -> category.activities().stream()).toList();

        assertThat(all).containsExactlyInAnyOrder(Activity.values()).doesNotHaveDuplicates();
        assertThat(ActivityCategory.SPORT.activities()).contains(Activity.PADEL, Activity.YOGA, Activity.PETANQUE)
                .doesNotContain(Activity.CINEMA);
        assertThat(ActivityCategory.CULTURE.activities())
                .containsExactly(Activity.CINEMA, Activity.CONCERTS, Activity.THEATER, Activity.MUSEUMS_EXHIBITIONS);
        assertThat(ActivityCategory.LEISURE.activities()).containsExactly(Activity.BOARD_GAMES, Activity.VIDEO_GAMES,
                Activity.DANCING, Activity.LANGUAGE_EXCHANGE, Activity.ESCAPE_ROOM);
        assertThat(ActivityCategory.FOOD_DRINK.activities()).containsExactly(Activity.TAPAS, Activity.MEAL,
                Activity.COFFEE);
        assertThat(ActivityCategory.OTHER.activities()).containsExactly(Activity.OTHER);
    }

    @Test
    void theCodesFitTheColumnsOfEveryService() {
        // The narrowest column that stores an activity is 20 characters (notifications and the free mode)
        assertThat(Arrays.stream(Activity.values()).map(Activity::name)).allMatch(code -> code.length() <= 20);
    }

    @Test
    void theWeatherMattersOnlyOutdoors() {
        assertThat(Arrays.stream(Activity.values()).filter(Activity::outdoor)).containsExactlyInAnyOrder(
                Activity.PADEL, Activity.FOOTBALL, Activity.BASKETBALL, Activity.TENNIS, Activity.RUNNING,
                Activity.CYCLING, Activity.HIKING, Activity.VOLLEYBALL, Activity.CLIMBING, Activity.SKATING,
                Activity.PADEL_SURF, Activity.PETANQUE);
    }

    @Test
    void theActivitiesOfSeveralCategoriesAreJoined() {
        var joined = ActivityCategory.activitiesOf(List.of(ActivityCategory.FOOD_DRINK, ActivityCategory.CULTURE));

        assertThat(joined).containsExactlyInAnyOrder(Activity.TAPAS, Activity.MEAL, Activity.COFFEE, Activity.CINEMA,
                Activity.CONCERTS, Activity.THEATER, Activity.MUSEUMS_EXHIBITIONS);
        assertThat(ActivityCategory.activitiesOf(List.of())).isEmpty();
    }

    @Test
    void anotherActivityIsNotAnInterestOfSomeoneFree() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new Interest(Activity.OTHER, null))
                .isInstanceOf(ValidationException.class).hasMessageContaining("Other");
        assertThat(new Interest(Activity.YOGA, null).activity()).isEqualTo(Activity.YOGA);
    }
}

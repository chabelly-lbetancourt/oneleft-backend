package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.domain.model.CompatibilityReason.ANY_LEVEL;
import static es.upm.miw.oneleft.plans.domain.model.CompatibilityReason.NEW_ACTIVITY;
import static es.upm.miw.oneleft.plans.domain.model.CompatibilityReason.SAME_ACTIVITY;
import static es.upm.miw.oneleft.plans.domain.model.CompatibilityReason.SAME_LEVEL;
import static es.upm.miw.oneleft.plans.domain.model.CompatibilityReason.SIMILAR_ACTIVITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU-031: the compatibility of a plan with a person is a fixed table of rules over the activity and the level of the
 * plan and the hobbies of the profile. Lucía, in these tests, plays padel at an intermediate level and likes cinema.
 */
class CompatibilityTest {

    private static final Map<Activity, Level> LUCIA = Map.of(Activity.PADEL, Level.INTERMEDIATE,
            Activity.CINEMA, Level.BEGINNER);

    private static Level level(String text) {
        return "ANY".equals(text) ? null : Level.valueOf(text);
    }

    @ParameterizedTest(name = "{0} at level {1}: {2} because {3} and {4}")
    @CsvSource({
            // A hobby of hers: 60, plus the level against her own in it
            "PADEL, INTERMEDIATE, 100, SAME_ACTIVITY, SAME_LEVEL",
            "PADEL, ANY, 100, SAME_ACTIVITY, ANY_LEVEL",
            "PADEL, BEGINNER, 80, SAME_ACTIVITY, CLOSE_LEVEL",
            "PADEL, ADVANCED, 80, SAME_ACTIVITY, CLOSE_LEVEL",
            "CINEMA, BEGINNER, 100, SAME_ACTIVITY, SAME_LEVEL",
            "CINEMA, INTERMEDIATE, 80, SAME_ACTIVITY, CLOSE_LEVEL",
            "CINEMA, ADVANCED, 60, SAME_ACTIVITY, LEVEL_GAP",
            // Not a hobby, but of the category of one (sport, like padel; culture, like cinema): 30, as a beginner
            "TENNIS, ANY, 70, SIMILAR_ACTIVITY, ANY_LEVEL",
            "TENNIS, BEGINNER, 70, SIMILAR_ACTIVITY, BEGINNER_FRIENDLY",
            "TENNIS, INTERMEDIATE, 50, SIMILAR_ACTIVITY, NEEDS_EXPERIENCE",
            "TENNIS, ADVANCED, 30, SIMILAR_ACTIVITY, NEEDS_EXPERIENCE",
            "THEATER, ANY, 70, SIMILAR_ACTIVITY, ANY_LEVEL",
            // Nothing like it in her profile: only the level counts, as a beginner
            "BOARD_GAMES, ANY, 40, NEW_ACTIVITY, ANY_LEVEL",
            "BOARD_GAMES, BEGINNER, 40, NEW_ACTIVITY, BEGINNER_FRIENDLY",
            "TAPAS, INTERMEDIATE, 20, NEW_ACTIVITY, NEEDS_EXPERIENCE",
            "TAPAS, ADVANCED, 0, NEW_ACTIVITY, NEEDS_EXPERIENCE",
            // «Other» is like nothing else
            "OTHER, ANY, 40, NEW_ACTIVITY, ANY_LEVEL",
            "OTHER, ADVANCED, 0, NEW_ACTIVITY, NEEDS_EXPERIENCE"})
    void eachPlanHasItsScoreAndItsTwoReasons(Activity activity, String planLevel, int score,
                                             CompatibilityReason activityReason, CompatibilityReason levelReason) {
        var compatibility = Compatibility.of(activity, level(planLevel), LUCIA);

        assertThat(compatibility.score()).isEqualTo(score);
        assertThat(compatibility.reasons()).containsExactly(activityReason, levelReason);
    }

    @Test
    void anAdvancedPersonIsTwoLevelsAwayFromAPlanForBeginners() {
        var compatibility = Compatibility.of(Activity.CLIMBING, Level.BEGINNER,
                Map.of(Activity.CLIMBING, Level.ADVANCED));

        assertThat(compatibility).isEqualTo(new Compatibility(60,
                List.of(SAME_ACTIVITY, CompatibilityReason.LEVEL_GAP)));
    }

    @Test
    void withoutHobbiesOnlyTheLevelCounts() {
        assertThat(Compatibility.of(Activity.PADEL, null, Map.of()))
                .isEqualTo(new Compatibility(40, List.of(NEW_ACTIVITY, ANY_LEVEL)));
        assertThat(Compatibility.of(Activity.PADEL, Level.ADVANCED, Map.of()).score()).isZero();
    }

    @Test
    void theCategoryOfOtherActivitiesIsNotSomethingInCommon() {
        // Only possible with data of another version of the users service: «other» is never a hobby
        var hobbies = Map.of(Activity.OTHER, Level.ADVANCED);

        assertThat(Compatibility.of(Activity.YOGA, null, hobbies).reasons()).containsExactly(NEW_ACTIVITY, ANY_LEVEL);
    }

    @Test
    void theScoreIsAlwaysBetween0And100WithTwoReasonsAndTheSameEveryTime() {
        var planLevels = new Level[]{null, Level.BEGINNER, Level.INTERMEDIATE, Level.ADVANCED};
        for (var activity : Activity.values()) {
            for (var planLevel : planLevels) {
                for (var hobby : Activity.values()) {
                    for (var ownLevel : Level.values()) {
                        var hobbies = new EnumMap<Activity, Level>(Map.of(hobby, ownLevel));

                        var compatibility = Compatibility.of(activity, planLevel, hobbies);

                        assertThat(compatibility.score()).isBetween(0, 100);
                        assertThat(compatibility.score() % 10).isZero();
                        assertThat(compatibility.reasons()).hasSize(2);
                        assertThat(Compatibility.of(activity, planLevel, hobbies)).isEqualTo(compatibility);
                    }
                }
            }
        }
    }

    @Test
    void theOrderOfTheHobbiesDoesNotChangeTheResult() {
        var oneWay = new LinkedHashMap<Activity, Level>();
        oneWay.put(Activity.PADEL, Level.INTERMEDIATE);
        oneWay.put(Activity.YOGA, Level.BEGINNER);
        var otherWay = new LinkedHashMap<Activity, Level>();
        otherWay.put(Activity.YOGA, Level.BEGINNER);
        otherWay.put(Activity.PADEL, Level.INTERMEDIATE);

        assertThat(Compatibility.of(Activity.TENNIS, Level.BEGINNER, oneWay))
                .isEqualTo(Compatibility.of(Activity.TENNIS, Level.BEGINNER, otherWay));
    }

    @Test
    void thePlanAndTheProfileOfThePersonGiveTheSameAsTheirParts() {
        var plan = Plan.publish(ana(), Activity.PADEL, "Padel", null, COURTS, NOW.plus(Duration.ofHours(1)), 1,
                Level.INTERMEDIATE, CLOCK);
        var lucia = new AudienceMember(PersonGender.WOMAN, 33, "25–34", LUCIA);

        assertThat(Compatibility.of(plan, lucia)).isEqualTo(new Compatibility(100, List.of(SAME_ACTIVITY, SAME_LEVEL)));
        // Someone whose profile has no hobbies
        assertThat(Compatibility.of(plan, new AudienceMember(PersonGender.MAN, 40)).score()).isEqualTo(20);
    }

    @Test
    void aScoreOutOfRangeIsNotACompatibilityAndItsReasonsCannotBeChanged() {
        assertThatThrownBy(() -> new Compatibility(101, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Compatibility(-1, List.of())).isInstanceOf(IllegalArgumentException.class);
        var reasons = new ArrayList<>(List.of(SIMILAR_ACTIVITY));
        var compatibility = new Compatibility(30, reasons);
        reasons.clear();

        assertThat(compatibility.reasons()).containsExactly(SIMILAR_ACTIVITY);
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-068: a plan of «another» activity carries a short name of its own. */
class PlanActivityOtherTest {

    private static Plan publish(Activity activity, String other) {
        return Plan.publish(ana(), activity, other, "Taller de cerámica en Lavapiés", null, COURTS,
                NOW.plus(Duration.ofHours(2)), 2, null, null, null, CLOCK);
    }

    @Test
    void anotherActivityKeepsItsNameWithoutSurroundingSpaces() {
        var plan = publish(Activity.OTHER, "  Cerámica  ");

        assertThat(plan.activity()).isEqualTo(Activity.OTHER);
        assertThat(plan.activityOther()).isEqualTo("Cerámica");
    }

    @Test
    void anyOtherActivityDiscardsTheName() {
        assertThat(publish(Activity.PADEL, "Cerámica").activityOther()).isNull();
        assertThat(publish(Activity.PADEL, null).activityOther()).isNull();
    }

    @Test
    void anotherActivityNeedsAShortNameWithoutControlCharacters() {
        for (var invalid : List.of("", "   ", "x".repeat(31), "Cerá\nmica", "Cerá\tmica")) {
            assertThatThrownBy(() -> publish(Activity.OTHER, invalid)).as(invalid)
                    .isInstanceOf(ValidationException.class)
                    .satisfies(error -> assertThat(((ValidationException) error).code())
                            .isEqualTo("plan.activityOther"));
        }
        assertThatThrownBy(() -> publish(Activity.OTHER, null)).isInstanceOf(ValidationException.class);
        assertThat(publish(Activity.OTHER, "x".repeat(30)).activityOther()).hasSize(30);
    }

    @Test
    void theNameSurvivesEveryChangeOfThePlan() {
        var plan = publish(Activity.OTHER, "Cerámica");
        var joined = plan.join(java.util.UUID.randomUUID(), "Lucía", CLOCK);

        assertThat(joined.activityOther()).isEqualTo("Cerámica");
    }
}

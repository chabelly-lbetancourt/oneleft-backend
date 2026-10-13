package es.upm.miw.oneleft.reputation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-044: the track record each closed attendance adds, and when each badge is reached. */
class BadgeRulesTest {

    private static final Instant NOW = Instant.parse("2026-10-13T16:30:00Z");
    private final UUID plan = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    @Test
    void everyoneOfTheGroupGetsALineWithWhetherTheyArrivedAndWhetherThePlanWasHeld() {
        var fact = new AttendanceFact(plan, "Padel", "PADEL", ana, List.of(lucia), List.of(ana), false, NOW);

        assertThat(BadgeRules.attendances(fact)).containsExactly(
                new Attendance(lucia, plan, "PADEL", false, true, true, NOW),
                new Attendance(ana, plan, "PADEL", true, false, true, NOW));
    }

    @Test
    void aPlanNobodyArrivedAtWasNotHeld() {
        var fact = new AttendanceFact(plan, "Padel", "PADEL", ana, List.of(), List.of(ana, lucia), true, NOW);

        assertThat(BadgeRules.attendances(fact)).allSatisfy(line -> {
            assertThat(line.held()).isFalse();
            assertThat(line.arrived()).isFalse();
        });
    }

    @Test
    void eachBadgeIsReachedAtItsGoalAndTheProgressStopsThere() {
        assertThat(BadgeRules.reached(TrackRecord.EMPTY)).isEmpty();
        var record = new TrackRecord(1, 12, 9, 20, 49);

        assertThat(BadgeRules.reached(record))
                .containsExactly(Badge.FIRST_PLAN_ORGANIZED, Badge.TEN_PLANS_COMPLETED, Badge.ALWAYS_PUNCTUAL);
        assertThat(record.progressOf(Badge.TEN_PLANS_COMPLETED)).isEqualTo(10);
        assertThat(record.progressOf(Badge.TEN_ACTIVITIES)).isEqualTo(9);
        assertThat(record.progressOf(Badge.NEIGHBOURHOOD_SOUL)).isEqualTo(49);
        assertThat(new BadgeProgress(Badge.TEN_ACTIVITIES, 9, 10, null).earned()).isFalse();
    }
}

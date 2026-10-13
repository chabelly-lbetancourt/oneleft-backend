package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;

/** HU-009: who of the group arrived when the check-in closes. */
class PlanAttendanceTest {

    private static final UUID LUCIA = UUID.randomUUID();

    private final Plan plan = padelPlan(ana(), Duration.ofHours(2), CLOCK).join(LUCIA, "Lucía", CLOCK);
    private final Instant closing = plan.startsAt().plus(CheckIn.CLOSES_AFTER);

    private CheckIn checkIn(UUID userId) {
        return new CheckIn(plan.id(), userId, "Someone", plan.startsAt());
    }

    @Test
    void theGroupIsSplitIntoWhoArrivedAndWhoDidNot() {
        var organizer = plan.organizer().id();

        var attendance = plan.attendance(List.of(checkIn(LUCIA)), closing);

        assertThat(attendance).isEqualTo(new PlanAttendanceClosed(plan.id(), plan.title(), organizer, List.of(LUCIA),
                List.of(organizer), true, plan.startsAt(), closing));
    }

    @Test
    void checkInsOfPeopleWhoLeftTheGroupDoNotCount() {
        var attendance = plan.attendance(List.of(checkIn(UUID.randomUUID())), closing);

        assertThat(attendance.attendeeIds()).isEmpty();
        assertThat(attendance.absenteeIds()).containsExactly(plan.organizer().id(), LUCIA);
    }

    @Test
    void aPlanWithFreeSpotsIsNotFull() {
        var bigger = Plan.publish(plan.organizer(), Activity.PADEL, "Padel 2 vs 2", null, COURTS,
                plan.startsAt(), 3, null, CLOCK);

        assertThat(bigger.attendance(List.of(), closing).full()).isFalse();
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanJoinTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();

    private static Plan plan(int spots) {
        return Plan.publish(ana(), Activity.PADEL, "Padel match", null, new MeetingPoint("Courts", 40.39, -3.62),
                NOW.plus(Duration.ofHours(1)), spots, null, CLOCK);
    }

    private static void assertRejected(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(JoinRejectedException.class,
                rejected -> assertThat(rejected.code()).isEqualTo(code));
    }

    @Test
    void takesAFreeSpotAndKeepsThePlanOpen() {
        var joined = plan(2).join(LUCIA, "Lucía", CLOCK);

        assertThat(joined.occupied()).isEqualTo(1);
        assertThat(joined.freeSpots()).isEqualTo(1);
        assertThat(joined.status()).isEqualTo(PlanStatus.OPEN);
        assertThat(joined.participants()).singleElement()
                .isEqualTo(new Participant(LUCIA, "Lucía", NOW));
        assertThat(joined.isParticipant(LUCIA)).isTrue();
        assertThat(joined.isParticipant(DIEGO)).isFalse();
        var event = joined.joinedEvent();
        assertThat(event.participantName()).isEqualTo("Lucía");
        assertThat(event.freeSpots()).isEqualTo(1);
        assertThat(event.full()).isFalse();
    }

    @Test
    void theLastSpotClosesThePlan() {
        var full = plan(2).join(LUCIA, "Lucía", CLOCK).join(DIEGO, "Diego", CLOCK);

        assertThat(full.status()).isEqualTo(PlanStatus.FULL);
        assertThat(full.freeSpots()).isZero();
        assertThat(full.joinedEvent().full()).isTrue();
        assertRejected(() -> full.join(UUID.randomUUID(), "Marta", CLOCK), "plan.full");
    }

    @Test
    void rejectsTheOrganizerAndRepeatedParticipants() {
        var plan = plan(3);
        assertRejected(() -> plan.join(plan.organizer().id(), "Ana", CLOCK), "plan.ownPlan");
        var joined = plan.join(LUCIA, "Lucía", CLOCK);
        assertRejected(() -> joined.join(LUCIA, "Lucía", CLOCK), "plan.alreadyJoined");
    }

    @Test
    void rejectsPlansThatHaveStartedOrAreNotOpen() {
        var plan = plan(3);
        var later = Clock.fixed(plan.startsAt(), ZoneOffset.UTC);
        assertRejected(() -> plan.join(LUCIA, "Lucía", later), "plan.started");

        var cancelled = new Plan(plan.id(), plan.organizer(), plan.activity(), plan.title(), null, plan.meetingPoint(),
                plan.startsAt(), 3, 0, null, PlanStatus.CANCELLED, plan.publishedAt());
        assertRejected(() -> cancelled.join(LUCIA, "Lucía", CLOCK), "plan.notOpen");
    }

    @Test
    void participantsNeedAUserAndGetADefaultName() {
        assertThat(new Participant(LUCIA, " ", NOW).name()).isEqualTo("Participant");
        assertThatThrownBy(() -> new Participant(null, "Lucía", NOW)).isInstanceOf(ValidationException.class);
    }
}

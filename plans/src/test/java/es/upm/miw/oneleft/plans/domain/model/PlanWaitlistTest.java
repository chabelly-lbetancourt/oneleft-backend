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

/** HU-023: leave a plan and waiting list. */
class PlanWaitlistTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();
    private static final Clock LATER = Clock.fixed(NOW.plus(Duration.ofMinutes(10)), ZoneOffset.UTC);

    private static Plan plan(int spots) {
        return Plan.publish(ana(), Activity.PADEL, "Padel match", null, new MeetingPoint("Courts", 40.39, -3.62),
                NOW.plus(Duration.ofHours(1)), spots, null, CLOCK);
    }

    private static Plan fullWithLucia() {
        return plan(1).join(LUCIA, "Lucía", CLOCK);
    }

    private static void assertRejected(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(JoinRejectedException.class,
                rejected -> assertThat(rejected.code()).isEqualTo(code));
    }

    @Test
    void leavingFreesTheSpotAndReopensAFullPlan() {
        var left = fullWithLucia().leave(LUCIA, LATER);

        var plan = left.plan();
        assertThat(plan.isParticipant(LUCIA)).isFalse();
        assertThat(plan.occupied()).isZero();
        assertThat(plan.freeSpots()).isEqualTo(1);
        assertThat(plan.status()).isEqualTo(PlanStatus.OPEN);
        assertThat(left.promoted()).isNull();
        var event = left.event();
        assertThat(event.participantName()).isEqualTo("Lucía");
        assertThat(event.promotedId()).isNull();
        assertThat(event.promotedName()).isNull();
        assertThat(event.freeSpots()).isEqualTo(1);
        assertThat(event.full()).isFalse();
        assertThat(event.occurredAt()).isEqualTo(LATER.instant());
    }

    @Test
    void theFirstPersonWaitingTakesTheFreedSpot() {
        var waiting = fullWithLucia().joinWaitlist(DIEGO, "Diego", CLOCK).joinWaitlist(MARTA, "Marta", LATER);
        assertThat(waiting.waitlist()).extracting(Participant::name).containsExactly("Diego", "Marta");
        assertThat(waiting.isWaiting(DIEGO)).isTrue();

        var left = waiting.leave(LUCIA, LATER);

        var plan = left.plan();
        assertThat(plan.status()).isEqualTo(PlanStatus.FULL);
        assertThat(plan.occupied()).isEqualTo(1);
        assertThat(plan.isParticipant(DIEGO)).isTrue();
        assertThat(plan.participants()).singleElement().isEqualTo(new Participant(DIEGO, "Diego", LATER.instant()));
        assertThat(plan.waitlist()).extracting(Participant::name).containsExactly("Marta");
        var event = left.event();
        assertThat(event.promotedId()).isEqualTo(DIEGO);
        assertThat(event.promotedName()).isEqualTo("Diego");
        assertThat(event.full()).isTrue();
        assertThat(event.freeSpots()).isZero();
    }

    @Test
    void onlyParticipantsCanLeaveAndOnlyBeforeTheStart() {
        var plan = fullWithLucia();
        assertRejected(() -> plan.leave(DIEGO, CLOCK), "plan.notParticipant");
        var started = Clock.fixed(NOW.plus(Duration.ofHours(1)), ZoneOffset.UTC);
        assertRejected(() -> plan.leave(LUCIA, started), "plan.started");
    }

    @Test
    void theWaitingListIsOnlyForFullPlans() {
        var open = plan(2);
        assertRejected(() -> open.joinWaitlist(DIEGO, "Diego", CLOCK), "plan.notFull");

        var full = fullWithLucia();
        assertRejected(() -> full.joinWaitlist(full.organizer().id(), "Ana", CLOCK), "plan.ownPlan");
        assertRejected(() -> full.joinWaitlist(LUCIA, "Lucía", CLOCK), "plan.alreadyJoined");
        var waiting = full.joinWaitlist(DIEGO, "Diego", CLOCK);
        assertRejected(() -> waiting.joinWaitlist(DIEGO, "Diego", CLOCK), "plan.alreadyWaiting");
        var started = Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC);
        assertRejected(() -> full.joinWaitlist(MARTA, "Marta", started), "plan.started");
    }

    @Test
    void theWaitingListHasALimitAndClosedPlansHaveNone() {
        var full = fullWithLucia();
        for (var i = 0; i < Plan.MAX_WAITLIST; i++) {
            full = full.joinWaitlist(UUID.randomUUID(), "Person " + i, CLOCK);
        }
        var crowded = full;
        assertRejected(() -> crowded.joinWaitlist(MARTA, "Marta", CLOCK), "plan.waitlistFull");

        var base = plan(1);
        var cancelled = new Plan(base.id(), base.organizer(), base.activity(), base.title(), null, base.meetingPoint(),
                base.startsAt(), 1, 0, null, PlanStatus.CANCELLED, base.publishedAt());
        assertRejected(() -> cancelled.joinWaitlist(MARTA, "Marta", CLOCK), "plan.notOpen");
    }

    @Test
    void leavingTheWaitingListKeepsTheOrderOfTheOthers() {
        var waiting = fullWithLucia().joinWaitlist(DIEGO, "Diego", CLOCK).joinWaitlist(MARTA, "Marta", CLOCK);

        var plan = waiting.leaveWaitlist(DIEGO);

        assertThat(plan.waitlist()).extracting(Participant::name).containsExactly("Marta");
        assertThat(plan.isWaiting(DIEGO)).isFalse();
        assertRejected(() -> plan.leaveWaitlist(DIEGO), "plan.notWaiting");
    }
}

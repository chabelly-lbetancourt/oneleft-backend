package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-039: minimum of participants with a deadline. */
class PlanMinimumTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final Instant START = NOW.plus(Duration.ofHours(2));
    private static final Instant DEADLINE = START.minus(Duration.ofHours(1));

    private static Clock at(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static Plan publish(int spots, Integer minimum, Instant deadline) {
        return Plan.publish(ana(), Activity.PADEL, "Padel 2 vs 2", null, COURTS, START, spots, Level.INTERMEDIATE,
                minimum, deadline, CLOCK);
    }

    private static void assertRejected(Integer minimum, Instant deadline, String code) {
        assertThatThrownBy(() -> publish(3, minimum, deadline))
                .isInstanceOfSatisfying(ValidationException.class, error -> assertThat(error.code()).isEqualTo(code));
    }

    @Test
    void aPlanIsPublishedWithAMinimumAndItsDeadline() {
        var plan = publish(3, 2, DEADLINE);

        assertThat(plan.minimum()).isEqualTo(new Minimum(2, DEADLINE, null));
        assertThat(plan.minimum().pending()).isTrue();
        assertThat(publish(3, null, null).minimum()).isNull();
    }

    @Test
    void theMinimumIsBetweenOneAndTheSpotsAndGoesWithItsDeadline() {
        assertRejected(0, DEADLINE, "plan.minimum");
        assertRejected(4, DEADLINE, "plan.minimum");
        assertRejected(2, null, "plan.minimumDeadline");
        assertRejected(null, DEADLINE, "plan.minimumDeadline");
        assertThat(publish(3, 3, DEADLINE).minimum().participants()).isEqualTo(3);
    }

    @Test
    void theDeadlineIsAtLeastFiveMinutesAwayAndNotAfterTheStart() {
        assertRejected(1, NOW.plus(Plan.MIN_LEAD_TIME).minusSeconds(1), "plan.minimumDeadlineTooSoon");
        assertRejected(1, START.plusSeconds(1), "plan.minimumDeadline");
        assertThat(publish(3, 1, NOW.plus(Plan.MIN_LEAD_TIME)).minimum()).isNotNull();
        assertThat(publish(3, 1, START).minimum().deadline()).isEqualTo(START);
    }

    @Test
    void theMinimumIsCheckedOnlyAtItsDeadline() {
        var plan = publish(3, 2, DEADLINE);

        assertThat(plan.needsMinimumCheck(DEADLINE.minusSeconds(1))).isFalse();
        assertThat(plan.needsMinimumCheck(DEADLINE)).isTrue();
        assertThat(publish(3, null, null).needsMinimumCheck(START)).isFalse();
        assertThatThrownBy(() -> plan.checkMinimum(DEADLINE.minusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reachingTheMinimumConfirmsThePlanAndItGoesAheadEvenIfSomeoneLeaves() {
        var plan = publish(3, 2, DEADLINE).join(LUCIA, "Lucía", CLOCK).join(DIEGO, "Diego", CLOCK);

        var checked = plan.checkMinimum(DEADLINE);

        assertThat(checked.cancellation()).isEmpty();
        var confirmed = checked.plan();
        assertThat(confirmed.status()).isEqualTo(PlanStatus.OPEN);
        assertThat(confirmed.minimum().confirmedAt()).isEqualTo(DEADLINE);
        assertThat(confirmed.needsMinimumCheck(DEADLINE.plusSeconds(60))).isFalse();

        var afterLeaving = confirmed.leave(DIEGO, at(DEADLINE.plusSeconds(60))).plan();
        assertThat(afterLeaving.status()).isEqualTo(PlanStatus.OPEN);
        assertThat(afterLeaving.needsMinimumCheck(DEADLINE.plusSeconds(120))).isFalse();
    }

    @Test
    void withoutTheMinimumThePlanIsCancelledAndEveryoneInItIsTold() {
        // Two needed, one joined and another one came and went
        var plan = publish(3, 2, DEADLINE).join(LUCIA, "Lucía", CLOCK).join(DIEGO, "Diego", CLOCK)
                .leave(DIEGO, CLOCK).plan();

        var checked = plan.checkMinimum(DEADLINE.plusSeconds(30));

        assertThat(checked.plan().status()).isEqualTo(PlanStatus.CANCELLED);
        assertThat(checked.plan().minimum().pending()).isTrue();
        assertThat(checked.cancellation()).hasValueSatisfying(event -> {
            assertThat(event.planId()).isEqualTo(plan.id());
            assertThat(event.title()).isEqualTo("Padel 2 vs 2");
            assertThat(event.placeName()).isEqualTo(COURTS.name());
            assertThat(event.startsAt()).isEqualTo(START);
            assertThat(event.reason()).isEqualTo(PlanCancelled.Reason.MINIMUM_NOT_REACHED);
            assertThat(event.recipientIds()).containsExactly(plan.organizer().id(), LUCIA);
            assertThat(event.occurredAt()).isEqualTo(DEADLINE.plusSeconds(30));
        });
    }

    @Test
    void aCancelledPlanTakesNobodyAndDoesNotMoveAnyMore() {
        var cancelled = publish(3, 2, DEADLINE).join(LUCIA, "Lucía", CLOCK).checkMinimum(DEADLINE).plan();

        assertThat(cancelled.status()).isEqualTo(PlanStatus.CANCELLED);
        assertThatThrownBy(() -> cancelled.join(DIEGO, "Diego", at(DEADLINE.plusSeconds(1))))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.notOpen"));
        assertThat(cancelled.advance(START.plus(Plan.DURATION))).isSameAs(cancelled);
        assertThat(cancelled.needsReminder(START.minus(Plan.REMINDER_LEAD))).isFalse();
    }

    @Test
    void nobodyIsRemindedOfAPlanThatMayStillBeCancelled() {
        // The deadline is after the reminder time: the reminder waits until the plan is confirmed
        var lateDeadline = START.minus(Duration.ofMinutes(10));
        var plan = publish(3, 1, lateDeadline).join(LUCIA, "Lucía", CLOCK);
        var reminderTime = START.minus(Plan.REMINDER_LEAD);

        assertThat(plan.needsReminder(reminderTime)).isFalse();

        var confirmed = plan.checkMinimum(lateDeadline).plan();
        assertThat(confirmed.needsReminder(lateDeadline)).isTrue();
        assertThat(confirmed.remind(lateDeadline).event().recipientIds())
                .containsExactly(plan.organizer().id(), LUCIA);
    }

    @Test
    void theMinimumTravelsThroughEveryChangeOfThePlan() {
        var minimum = new Minimum(2, DEADLINE, null);
        var plan = publish(3, 2, DEADLINE);

        assertThat(plan.join(LUCIA, "Lucía", CLOCK).minimum()).isEqualTo(minimum);
        var full = publish(1, 1, DEADLINE).join(LUCIA, "Lucía", CLOCK).joinWaitlist(DIEGO, "Diego", CLOCK);
        assertThat(full.leaveWaitlist(DIEGO).minimum()).isEqualTo(new Minimum(1, DEADLINE, null));
        assertThat(plan.advance(START).minimum()).isEqualTo(minimum);
    }

    @Test
    void aStoredMinimumMustFitThePlan() {
        assertThatThrownBy(() -> new Plan(UUID.randomUUID(), ana(), Activity.PADEL, "Padel 2 vs 2", null, COURTS,
                START, 2, 0, null, PlanStatus.OPEN, NOW, null, null, null, new Minimum(3, DEADLINE, null), 0))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.minimum"));
        assertThatThrownBy(() -> new Minimum(1, null, null))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.minimumDeadline"));
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-007: automatic expiry and the reminder before the start. */
class PlanLifecycleTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();

    private static Clock at(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private final Plan plan = padelPlan(ana(), Duration.ofHours(2), CLOCK);
    private final Instant start = plan.startsAt();

    @Test
    void anOpenPlanIsInProgressFromItsStartAndFinishedThreeHoursLater() {
        assertThat(plan.advance(start.minusSeconds(1))).isSameAs(plan);

        var started = plan.advance(start);
        assertThat(started.status()).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(started.advance(start.plus(Plan.DURATION).minusSeconds(1))).isSameAs(started);

        assertThat(started.advance(start.plus(Plan.DURATION)).status()).isEqualTo(PlanStatus.FINISHED);
    }

    @Test
    void aPlanFoundLongAfterItsEndGoesStraightToFinished() {
        assertThat(plan.advance(start.plus(Duration.ofDays(1))).status()).isEqualTo(PlanStatus.FINISHED);
    }

    @Test
    void aFullPlanStartsTooAndNobodyKeepsWaiting() {
        var full = plan.join(LUCIA, "Lucía", CLOCK).joinWaitlist(DIEGO, "Diego", CLOCK);
        assertThat(full.status()).isEqualTo(PlanStatus.FULL);

        var started = full.advance(start);

        assertThat(started.status()).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(started.waitlist()).isEmpty();
        assertThat(started.isParticipant(LUCIA)).isTrue();
    }

    @Test
    void finishedAndCancelledPlansDoNotMove() {
        var finished = plan.advance(start.plus(Plan.DURATION));
        assertThat(finished.advance(start.plus(Duration.ofDays(2)))).isSameAs(finished);

        var cancelled = new Plan(plan.id(), plan.organizer(), plan.activity(), plan.title(), plan.description(),
                plan.meetingPoint(), start, plan.spots(), 0, plan.level(), PlanStatus.CANCELLED, plan.publishedAt());
        assertThat(cancelled.advance(start.plus(Duration.ofDays(2)))).isSameAs(cancelled);
        assertThat(cancelled.needsReminder(start.minusSeconds(60))).isFalse();
    }

    @Test
    void whoeverIsInThePlanIsRemindedOnceHalfAnHourBefore() {
        var joined = plan.join(LUCIA, "Lucía", CLOCK);
        var reminderTime = start.minus(Plan.REMINDER_LEAD);
        assertThat(joined.needsReminder(reminderTime.minusSeconds(1))).isFalse();
        assertThat(joined.needsReminder(reminderTime)).isTrue();

        var reminded = joined.remind(reminderTime.plusSeconds(20));

        assertThat(reminded.event().recipientIds()).containsExactly(plan.organizer().id(), LUCIA);
        assertThat(reminded.event()).extracting(PlanReminder::planId, PlanReminder::title, PlanReminder::placeName,
                PlanReminder::startsAt).containsExactly(plan.id(), plan.title(), plan.meetingPoint().name(), start);
        assertThat(reminded.plan().remindedAt()).isEqualTo(reminderTime.plusSeconds(20));
        assertThat(reminded.plan().needsReminder(reminderTime.plusSeconds(30))).isFalse();
        assertThatThrownBy(() -> reminded.plan().remind(reminderTime.plusSeconds(30)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theReminderSurvivesTheChangesOfThePlanAndIsNotSentOnceStarted() {
        var reminded = plan.remind(start.minus(Plan.REMINDER_LEAD)).plan();
        var later = reminded.join(LUCIA, "Lucía", at(start.minusSeconds(600)))
                .leave(LUCIA, at(start.minusSeconds(300))).plan();
        assertThat(later.remindedAt()).isEqualTo(reminded.remindedAt());

        assertThat(plan.needsReminder(start)).isFalse();
    }

    @Test
    void aPlanPublishedLessThanHalfAnHourBeforeHasNoReminder() {
        var soon = padelPlan(ana(), Duration.ofMinutes(20), CLOCK);
        assertThat(soon.remindedAt()).isEqualTo(NOW);
        assertThat(soon.needsReminder(NOW.plusSeconds(60))).isFalse();

        assertThat(plan.remindedAt()).isNull();
    }
}

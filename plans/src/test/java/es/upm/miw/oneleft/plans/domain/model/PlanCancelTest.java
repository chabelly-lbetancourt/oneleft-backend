package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The organizer cancels their own plan before it starts. */
class PlanCancelTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final Instant START = NOW.plus(Duration.ofHours(2));
    private static final Instant LATER = NOW.plusSeconds(60);

    private final Plan plan = Plan.publish(ana(), Activity.PADEL, "Padel, one missing", null, COURTS, START, 1,
            Level.INTERMEDIATE, CLOCK);
    private final UUID organizer = plan.organizer().id();

    private static void assertStarted(Plan plan, Instant now) {
        assertThatThrownBy(() -> plan.cancel(plan.organizer().id(), now))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.started"));
    }

    @Test
    void theOrganizerCancelsAndWhoeverJoinedOrWasWaitingIsTold() {
        var full = plan.join(LUCIA, "Lucía", CLOCK).joinWaitlist(DIEGO, "Diego", CLOCK)
                .announce(LUCIA, ArrivalStatus.ON_THE_WAY, null, CLOCK).plan();

        var cancelled = full.cancel(organizer, LATER);

        assertThat(cancelled.plan().status()).isEqualTo(PlanStatus.CANCELLED);
        assertThat(cancelled.plan().isParticipant(LUCIA)).isTrue();
        assertThat(cancelled.plan().waitlist()).isEmpty();
        assertThat(cancelled.plan().arrivals()).isEmpty();
        assertThat(cancelled.event()).hasValueSatisfying(event -> {
            assertThat(event.planId()).isEqualTo(plan.id());
            assertThat(event.organizerId()).isEqualTo(organizer);
            assertThat(event.title()).isEqualTo("Padel, one missing");
            assertThat(event.placeName()).isEqualTo(COURTS.name());
            assertThat(event.startsAt()).isEqualTo(START);
            assertThat(event.reason()).isEqualTo(PlanCancelled.Reason.ORGANIZER);
            // The organizer decided it: only the others are told
            assertThat(event.recipientIds()).containsExactly(LUCIA, DIEGO);
            assertThat(event.occurredAt()).isEqualTo(LATER);
        });
    }

    @Test
    void aPlanWithNobodyInItIsCancelledAndNobodyIsTold() {
        var cancelled = plan.cancel(organizer, LATER);

        assertThat(cancelled.plan().status()).isEqualTo(PlanStatus.CANCELLED);
        assertThat(cancelled.event()).hasValueSatisfying(event -> assertThat(event.recipientIds()).isEmpty());
    }

    @Test
    void onlyTheOrganizerCancelsWhateverTheStateOfThePlan() {
        var joined = plan.join(LUCIA, "Lucía", CLOCK);

        assertThatThrownBy(() -> joined.cancel(LUCIA, LATER)).isInstanceOf(NotOrganizerException.class);
        assertThatThrownBy(() -> joined.cancel(DIEGO, START)).isInstanceOf(NotOrganizerException.class);
        var cancelled = joined.cancel(organizer, LATER).plan();
        assertThatThrownBy(() -> cancelled.cancel(LUCIA, LATER)).isInstanceOf(NotOrganizerException.class);
    }

    @Test
    void aPlanThatHasStartedOrFinishedIsNotCancelled() {
        // From its start time, even if the lifecycle has not moved it yet
        assertStarted(plan, START);
        assertStarted(plan.advance(START), START.plusSeconds(1));
        assertStarted(plan.advance(START.plus(Plan.DURATION)), START.plus(Plan.DURATION));
        assertThat(plan.cancel(organizer, START.minusSeconds(1)).plan().status()).isEqualTo(PlanStatus.CANCELLED);
    }

    @Test
    void cancellingACancelledPlanChangesNothingAndTellsNobodyAgain() {
        var cancelled = plan.join(LUCIA, "Lucía", CLOCK).cancel(organizer, LATER).plan();

        var again = cancelled.cancel(organizer, LATER.plusSeconds(60));

        assertThat(again.plan()).isSameAs(cancelled);
        assertThat(again.event()).isEmpty();
        // Also after its start time: it stays cancelled
        assertThat(cancelled.cancel(organizer, START.plusSeconds(1)).event()).isEmpty();
    }

    @Test
    void aCancelledPlanTakesNobodyAndDoesNotMoveAnyMore() {
        var cancelled = plan.cancel(organizer, LATER).plan();

        assertThatThrownBy(() -> cancelled.join(DIEGO, "Diego", CLOCK))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.notOpen"));
        assertThat(cancelled.advance(START.plus(Plan.DURATION))).isSameAs(cancelled);
        assertThat(cancelled.needsReminder(START.minus(Plan.REMINDER_LEAD))).isFalse();
    }
}

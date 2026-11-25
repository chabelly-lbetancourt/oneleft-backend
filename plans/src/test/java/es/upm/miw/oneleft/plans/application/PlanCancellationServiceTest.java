package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.NotOrganizerException;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PlanCancellationServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final PlanEventPublisher publisher = mock(PlanEventPublisher.class);
    private final Plan plan = plans.save(Plan.publish(ana(), Activity.PADEL, "Padel match", null,
            new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofMinutes(30)), 1, null, CLOCK)
            .join(LUCIA, "Lucía", CLOCK));
    private final UUID organizer = plan.organizer().id();

    private PlanCancellationService serviceAt(Clock clock) {
        return new PlanCancellationService(plans, new OptimisticPlanUpdates(plans,
                TransactionOperations.withoutTransaction()), publisher, clock);
    }

    private PlanCancelled published() {
        var event = ArgumentCaptor.forClass(PlanCancelled.class);
        verify(publisher).publish(event.capture());
        return event.getValue();
    }

    @Test
    void cancelsThePlanAndPublishesItOnceHoweverManyTimesItIsAsked() {
        var service = serviceAt(CLOCK);

        service.cancel(plan.id(), organizer);
        service.cancel(plan.id(), organizer);

        assertThat(plans.data.get(plan.id()).status()).isEqualTo(PlanStatus.CANCELLED);
        var event = published();
        assertThat(event.planId()).isEqualTo(plan.id());
        assertThat(event.reason()).isEqualTo(PlanCancelled.Reason.ORGANIZER);
        assertThat(event.recipientIds()).containsExactly(LUCIA);
        // With the organizer, the start and the moment, the reputation service tells a late cancellation (HU-009)
        assertThat(event.organizerId()).isEqualTo(organizer);
        assertThat(event.startsAt()).isEqualTo(plan.startsAt());
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void someoneElseOrAPlanAlreadyStartedLeavesThePlanAsItWasAndPublishesNothing() {
        var started = Clock.fixed(plan.startsAt(), ZoneOffset.UTC);

        assertThatThrownBy(() -> serviceAt(CLOCK).cancel(plan.id(), LUCIA)).isInstanceOf(NotOrganizerException.class);
        assertThatThrownBy(() -> serviceAt(started).cancel(plan.id(), organizer))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        rejected -> assertThat(rejected.code()).isEqualTo("plan.started"));
        assertThatThrownBy(() -> serviceAt(CLOCK).cancel(UUID.randomUUID(), organizer))
                .isInstanceOf(PlanNotFoundException.class);

        assertThat(plans.data.get(plan.id()).status()).isEqualTo(PlanStatus.FULL);
        verifyNoInteractions(publisher);
    }

    @Test
    void retriesWhenSomeoneElseSavedThePlanFirstAndGivesUpWithoutAnnouncingAnything() {
        plans.conflictsLeft = OptimisticPlanUpdates.MAX_ATTEMPTS;
        assertThatThrownBy(() -> serviceAt(CLOCK).cancel(plan.id(), organizer))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        rejected -> assertThat(rejected.code()).isEqualTo("plan.busy"));
        verifyNoInteractions(publisher);

        plans.conflictsLeft = 1;
        serviceAt(CLOCK).cancel(plan.id(), organizer);

        assertThat(plans.data.get(plan.id()).status()).isEqualTo(PlanStatus.CANCELLED);
        assertThat(published().recipientIds()).containsExactly(LUCIA);
    }
}

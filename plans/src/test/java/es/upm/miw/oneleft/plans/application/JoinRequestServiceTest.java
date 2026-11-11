package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.application.JoinPlanServiceTest.Plans;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Availability;
import es.upm.miw.oneleft.plans.domain.model.JoinRequested;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.NotOrganizerException;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.RequestDecided;
import es.upm.miw.oneleft.plans.domain.model.RequestStatus;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** HU-069: asking for a spot and the decisions of the organizer, with their events. */
class JoinRequestServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();

    private final Plans plans = new Plans();
    private final PlanEventPublisher events = mock(PlanEventPublisher.class);
    private final Availabilities availabilities = new Availabilities();
    private final BlockedPairs blocks = new BlockedPairs();
    private final Organizer organizer = ana();
    private final JoinRequestService service = new JoinRequestService(
            new OptimisticPlanUpdates(plans, TransactionOperations.withoutTransaction()), plans, events,
            availabilities, blocks, CLOCK);

    private Plan plan(int spots) {
        return plans.save(Plan.publish(organizer, Activity.PADEL, null, "Pádel", null,
                new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofHours(1)), spots, null, null, null,
                true, CLOCK));
    }

    @Test
    void askingTellsTheOrganizerAndTakesNoSpot() {
        var plan = plan(1);

        var asked = service.request(plan.id(), LUCIA, "Lucía", "Hola");

        assertThat(asked.occupied()).isZero();
        var captor = ArgumentCaptor.forClass(JoinRequested.class);
        verify(events).publish(captor.capture());
        assertThat(captor.getValue()).satisfies(event -> {
            assertThat(event.organizerId()).isEqualTo(organizer.id());
            assertThat(event.requesterId()).isEqualTo(LUCIA);
            assertThat(event.requesterName()).isEqualTo("Lucía");
        });
    }

    @Test
    void aBlockedPersonCannotAsk() {
        var plan = plan(1);
        blocks.block(organizer.id(), LUCIA);

        assertThatThrownBy(() -> service.request(plan.id(), LUCIA, "Lucía", null))
                .isInstanceOf(PlanNotFoundException.class);
        verify(events, never()).publish(any(JoinRequested.class));
    }

    @Test
    void acceptingGivesTheSpotAndTellsEveryoneWhatBecameOfTheirRequest() {
        var plan = plan(1);
        service.request(plan.id(), LUCIA, "Lucía", null);
        service.request(plan.id(), DIEGO, "Diego", null);
        availabilities.save(Availability.start(LUCIA, 40.39, -3.62, 2, Set.of(), CLOCK));

        var decision = service.accept(plan.id(), organizer.id(), LUCIA);

        assertThat(plans.data.get(plan.id()).participants()).hasSize(1);
        assertThat(availabilities.data).as("whoever is accepted is no longer free").doesNotContainKey(LUCIA);
        verify(events).publish(any(PlanJoined.class));
        var told = ArgumentCaptor.forClass(RequestDecided.class);
        verify(events, org.mockito.Mockito.times(2)).publish(told.capture());
        assertThat(told.getAllValues()).extracting(RequestDecided::requesterId, RequestDecided::decision,
                RequestDecided::planFull).containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(LUCIA, RequestStatus.ACCEPTED, false),
                org.assertj.core.groups.Tuple.tuple(DIEGO, RequestStatus.REJECTED, true));
        assertThat(decision.autoRejected()).hasSize(1);
    }

    @Test
    void rejectingTellsTheRequesterAndWithdrawingTellsNobody() {
        var plan = plan(2);
        service.request(plan.id(), LUCIA, "Lucía", null);
        service.request(plan.id(), DIEGO, "Diego", null);

        service.reject(plan.id(), organizer.id(), LUCIA);
        service.withdraw(plan.id(), DIEGO);

        var told = ArgumentCaptor.forClass(RequestDecided.class);
        verify(events).publish(told.capture());
        assertThat(told.getValue().requesterId()).isEqualTo(LUCIA);
        assertThat(told.getValue().planFull()).isFalse();
        assertThat(plans.data.get(plan.id()).requestOf(DIEGO).orElseThrow().status())
                .isEqualTo(RequestStatus.WITHDRAWN);
    }

    @Test
    void onlyTheOrganizerSeesAndDecidesOnTheRequests() {
        var plan = plan(1);
        service.request(plan.id(), LUCIA, "Lucía", null);

        assertThat(service.requestsOf(plan.id(), organizer.id()).requests()).hasSize(1);
        assertThatThrownBy(() -> service.requestsOf(plan.id(), LUCIA)).isInstanceOf(NotOrganizerException.class);
        assertThatThrownBy(() -> service.accept(plan.id(), LUCIA, LUCIA)).isInstanceOf(NotOrganizerException.class);
        assertThatThrownBy(() -> service.reject(plan.id(), DIEGO, LUCIA)).isInstanceOf(NotOrganizerException.class);
        assertThatThrownBy(() -> service.requestsOf(UUID.randomUUID(), organizer.id()))
                .isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void twoAcceptancesForTheLastSpotCannotBothWin() {
        var plan = plan(1);
        service.request(plan.id(), LUCIA, "Lucía", null);
        service.request(plan.id(), DIEGO, "Diego", null);
        // Someone else saved the plan first: the change is retried with the real state, where the plan is full
        plans.conflictsLeft = 1;
        service.accept(plan.id(), organizer.id(), LUCIA);

        assertThatThrownBy(() -> service.accept(plan.id(), organizer.id(), DIEGO))
                .hasMessageContaining("no pending request");
        assertThat(plans.data.get(plan.id()).participants()).hasSize(1);
    }
}

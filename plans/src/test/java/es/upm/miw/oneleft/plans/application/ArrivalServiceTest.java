package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.ArrivalStatus;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanArrival;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.model.ValidationException;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-040: «on my way» and «running late», only for the group and until the start. */
class ArrivalServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final List<PlanArrival> events = new ArrayList<>();
    private final PlanEventPublisher publisher = new PlanEventPublisher() {
        @Override
        public void publish(PlanPublished event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanJoined event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanLeftEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanReminder event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanCancelled event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanArrival event) {
            events.add(event);
        }
    };
    private final OptimisticPlanUpdates updates = new OptimisticPlanUpdates(plans,
            TransactionOperations.withoutTransaction());
    private final ArrivalService service = new ArrivalService(updates, publisher, CLOCK);
    private final Plan plan = plans.save(Plan.publish(ana(), Activity.PADEL, "Padel 2 vs 2", null,
            new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofHours(1)), 3, null, CLOCK)
            .join(LUCIA, "Lucía", CLOCK).join(DIEGO, "Diego", CLOCK));

    @Test
    void aParticipantSaysTheyAreOnTheWayAndTheRestOfTheGroupIsTold() {
        var after = service.announce(plan.id(), LUCIA, ArrivalStatus.ON_THE_WAY, 10);

        assertThat(after.arrivals()).singleElement().satisfies(arrival -> {
            assertThat(arrival.name()).isEqualTo("Lucía");
            assertThat(arrival.status()).isEqualTo(ArrivalStatus.ON_THE_WAY);
            assertThat(arrival.minutesLate()).isNull();
        });
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.recipientIds()).containsExactly(plan.organizer().id(), DIEGO);
            assertThat(event.title()).isEqualTo("Padel 2 vs 2");
        });
    }

    @Test
    void aNewStatusReplacesThePreviousOneAndCanBeTakenBack() {
        service.announce(plan.id(), LUCIA, ArrivalStatus.ON_THE_WAY, null);
        var late = service.announce(plan.id(), LUCIA, ArrivalStatus.LATE, 15);
        service.announce(plan.id(), plan.organizer().id(), ArrivalStatus.LATE, 5);

        assertThat(late.arrivals()).singleElement().satisfies(arrival -> {
            assertThat(arrival.status()).isEqualTo(ArrivalStatus.LATE);
            assertThat(arrival.minutesLate()).isEqualTo(15);
        });
        // The organizer is told about the participants, and the participants about the organizer
        assertThat(events.getLast().recipientIds()).containsExactly(LUCIA, DIEGO);
        assertThat(events.getLast().name()).isEqualTo("Ana");

        var cleared = service.clear(plan.id(), LUCIA);
        assertThat(cleared.arrivals()).extracting(arrival -> arrival.userId()).containsExactly(plan.organizer().id());
    }

    @Test
    void onlyTheGroupAnnouncesAndOnlyBeforeTheStart() {
        assertThatThrownBy(() -> service.announce(plan.id(), MARTA, ArrivalStatus.ON_THE_WAY, null))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.notParticipant"));
        assertThatThrownBy(() -> service.announce(plan.id(), LUCIA, ArrivalStatus.LATE, 7))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("arrival.minutes"));

        var started = new ArrivalService(updates, publisher,
                Clock.fixed(plan.startsAt().plusSeconds(1), ZoneOffset.UTC));
        assertThatThrownBy(() -> started.announce(plan.id(), LUCIA, ArrivalStatus.ON_THE_WAY, null))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.started"));
        assertThat(events).isEmpty();
    }

    @Test
    void statusesDisappearWhenThePlanStartsOrThePersonLeaves() {
        service.announce(plan.id(), LUCIA, ArrivalStatus.ON_THE_WAY, null);
        service.announce(plan.id(), DIEGO, ArrivalStatus.LATE, 10);

        var left = plans.data.get(plan.id()).leave(DIEGO, CLOCK).plan();
        assertThat(left.arrivals()).extracting(arrival -> arrival.userId()).containsExactly(LUCIA);

        var started = left.advance(plan.startsAt());
        assertThat(started.status()).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(started.arrivals()).isEmpty();
    }
}

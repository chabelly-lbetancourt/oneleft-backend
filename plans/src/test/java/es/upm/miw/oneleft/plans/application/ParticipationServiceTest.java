package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Availability;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticipationServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final List<PlanLeftEvent> events = new ArrayList<>();
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
            events.add(event);
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
        public void publish(es.upm.miw.oneleft.plans.domain.model.PlanArrival event) {
            throw new UnsupportedOperationException();
        }
    };
    private final Availabilities availabilities = new Availabilities();
    private final ParticipationService service = new ParticipationService(
            new OptimisticPlanUpdates(plans, TransactionOperations.withoutTransaction()), publisher, availabilities,
            CLOCK);
    private final Plan plan = plans.save(Plan.publish(ana(), Activity.PADEL, "Padel match", null,
            new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofHours(1)), 1, null, CLOCK)
            .join(LUCIA, "Lucía", CLOCK));

    @Test
    void waitingAndLeavingHandsTheSpotOverAndTellsEveryone() {
        service.joinWaitlist(plan.id(), DIEGO, "Diego");
        availabilities.save(Availability.start(DIEGO, 40.39, -3.62, 2, Set.of(), CLOCK));
        assertThat(plans.data.get(plan.id()).isWaiting(DIEGO)).isTrue();

        var after = service.leave(plan.id(), LUCIA);

        assertThat(after.isParticipant(DIEGO)).isTrue();
        // Diego got a spot: no longer free (HU-035)
        assertThat(availabilities.data).doesNotContainKey(DIEGO);
        assertThat(plans.data.get(plan.id()).waitlist()).isEmpty();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.organizerId()).isEqualTo(plan.organizer().id());
            assertThat(event.participantId()).isEqualTo(LUCIA);
            assertThat(event.promotedId()).isEqualTo(DIEGO);
        });
    }

    @Test
    void leavingTheWaitingListIsSilent() {
        service.joinWaitlist(plan.id(), DIEGO, "Diego");

        var after = service.leaveWaitlist(plan.id(), DIEGO);

        assertThat(after.isWaiting(DIEGO)).isFalse();
        assertThat(events).isEmpty();
    }

    @Test
    void retriesConflictsAndGivesUpWithoutAnnouncingAnything() {
        plans.conflictsLeft = OptimisticPlanUpdates.MAX_ATTEMPTS;
        assertThatThrownBy(() -> service.joinWaitlist(plan.id(), DIEGO, "Diego"))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        rejected -> assertThat(rejected.code()).isEqualTo("plan.busy"));

        plans.conflictsLeft = 1;
        service.leave(plan.id(), LUCIA);
        assertThat(events).hasSize(1);
        assertThatThrownBy(() -> service.leave(UUID.randomUUID(), LUCIA)).isInstanceOf(PlanNotFoundException.class);
    }
}

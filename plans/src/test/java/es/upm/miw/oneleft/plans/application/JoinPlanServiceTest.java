package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.ConcurrentPlanUpdateException;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JoinPlanServiceTest {

    /** In-memory repository that can simulate someone else saving the plan first. */
    static class Plans implements PlanRepository {
        final Map<UUID, Plan> data = new HashMap<>();
        int conflictsLeft;

        @Override
        public Plan save(Plan plan) {
            if (conflictsLeft > 0) {
                conflictsLeft--;
                throw new ConcurrentPlanUpdateException(plan.id());
            }
            data.put(plan.id(), plan);
            return plan;
        }

        @Override
        public Optional<Plan> findById(UUID planId) {
            return Optional.ofNullable(data.get(planId));
        }

        @Override
        public List<Plan> findByOrganizerStartingAfter(UUID organizerId, Instant from) {
            return List.of();
        }

        @Override
        public List<Plan> findOpenNearby(NearbySearch search, Instant now, int limit) {
            return List.of();
        }
    }

    private final Plans plans = new Plans();
    private final List<PlanJoined> events = new ArrayList<>();
    private final PlanEventPublisher publisher = new PlanEventPublisher() {
        @Override
        public void publish(PlanPublished event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanJoined event) {
            events.add(event);
        }
    };
    private final JoinPlanService service =
            new JoinPlanService(plans, publisher, CLOCK, TransactionOperations.withoutTransaction());
    private final Plan plan = plans.save(Plan.publish(ana(), Activity.PADEL, "Padel match", null,
            new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofHours(1)), 1, null, CLOCK));

    @Test
    void takesTheSpotAndNotifiesTheOrganizer() {
        var lucia = UUID.randomUUID();

        var joined = service.join(plan.id(), lucia, "Lucía");

        assertThat(joined.isParticipant(lucia)).isTrue();
        assertThat(plans.data.get(plan.id()).freeSpots()).isZero();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.organizerId()).isEqualTo(plan.organizer().id());
            assertThat(event.full()).isTrue();
        });
    }

    @Test
    void retriesWhenSomeoneElseSavedThePlanFirst() {
        plans.conflictsLeft = 2;

        service.join(plan.id(), UUID.randomUUID(), "Lucía");

        assertThat(plans.data.get(plan.id()).occupied()).isEqualTo(1);
        assertThat(events).hasSize(1);
    }

    @Test
    void givesUpAfterTooManyConflictsWithoutAnnouncingAnything() {
        plans.conflictsLeft = JoinPlanService.MAX_ATTEMPTS;

        assertThatThrownBy(() -> service.join(plan.id(), UUID.randomUUID(), "Lucía"))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        rejected -> assertThat(rejected.code()).isEqualTo("plan.busy"));
        assertThat(events).isEmpty();
    }

    @Test
    void unknownPlansAreNotFound() {
        assertThatThrownBy(() -> service.join(UUID.randomUUID(), UUID.randomUUID(), "Lucía"))
                .isInstanceOf(PlanNotFoundException.class);
    }
}

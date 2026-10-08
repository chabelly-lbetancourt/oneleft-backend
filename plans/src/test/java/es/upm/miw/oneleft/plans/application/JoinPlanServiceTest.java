package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.ConcurrentPlanUpdateException;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
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

        /** Same conditions as the SQL query (tested in JpaPlanRepositoryTest), in memory. */
        @Override
        public List<UUID> findDueForLifecycle(Instant now) {
            return data.values().stream().filter(plan -> switch (plan.status()) {
                case OPEN, FULL -> !plan.startsAt().isAfter(now) || plan.remindedAt() == null
                        && !plan.startsAt().isAfter(now.plus(Plan.REMINDER_LEAD))
                        || plan.minimum() != null && plan.minimum().pending()
                        && !plan.minimum().deadline().isAfter(now);
                case IN_PROGRESS -> !plan.startsAt().isAfter(now.minus(Plan.DURATION));
                default -> false;
            }).map(Plan::id).toList();
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
    };
    private final JoinPlanService service =
            new JoinPlanService(new OptimisticPlanUpdates(plans, TransactionOperations.withoutTransaction()), publisher,
                    CLOCK);
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
        plans.conflictsLeft = OptimisticPlanUpdates.MAX_ATTEMPTS;

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

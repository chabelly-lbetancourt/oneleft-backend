package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanCommand;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanServiceTest {

    static class InMemoryPlans implements PlanRepository {
        final Map<UUID, Plan> data = new HashMap<>();

        @Override
        public Plan save(Plan plan) {
            data.put(plan.id(), plan);
            return plan;
        }

        @Override
        public Optional<Plan> findById(UUID planId) {
            return Optional.ofNullable(data.get(planId));
        }

        @Override
        public List<Plan> findByOrganizerStartingAfter(UUID organizerId, Instant from) {
            return data.values().stream()
                    .filter(p -> p.organizer().id().equals(organizerId) && p.startsAt().isAfter(from))
                    .sorted(Comparator.comparing(Plan::startsAt)).toList();
        }

        /** Filters like PostGIS but in memory; the real query is tested in JpaPlanRepositoryTest. */
        @Override
        public List<Plan> findOpenNearby(NearbySearch search, Instant now, int limit) {
            return data.values().stream()
                    .filter(p -> search.includes(p.activity()))
                    .filter(p -> search.distanceTo(p.meetingPoint().latitude(), p.meetingPoint().longitude())
                            <= search.radiusMeters())
                    .sorted(Comparator.comparingDouble(p -> search.distanceTo(p.meetingPoint().latitude(),
                            p.meetingPoint().longitude())))
                    .limit(limit).toList();
        }

        @Override
        public List<UUID> findDueForLifecycle(Instant now) {
            return List.of();
        }
    }

    private final InMemoryPlans repository = new InMemoryPlans();
    private final List<PlanPublished> events = new ArrayList<>();
    private final PlanEventPublisher publisher = new PlanEventPublisher() {
        @Override
        public void publish(PlanPublished event) {
            events.add(event);
        }

        @Override
        public void publish(es.upm.miw.oneleft.plans.domain.model.PlanJoined event) {
            throw new UnsupportedOperationException("Not used by PlanService");
        }

        @Override
        public void publish(es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent event) {
            throw new UnsupportedOperationException("Not used by PlanService");
        }

        @Override
        public void publish(es.upm.miw.oneleft.plans.domain.model.PlanReminder event) {
            throw new UnsupportedOperationException("Not used by PlanService");
        }

        @Override
        public void publish(es.upm.miw.oneleft.plans.domain.model.PlanCancelled event) {
            throw new UnsupportedOperationException("Not used by PlanService");
        }
    };
    private final PlanService service = new PlanService(repository, publisher, CLOCK);

    @Test
    void publishingStoresThePlanAndEmitsTheEvent() {
        var command = new PublishPlanCommand(ana(), Activity.PADEL, "Padel match", null, COURTS,
                NOW.plus(Duration.ofHours(1)), 1, Level.INTERMEDIATE);

        var plan = service.publish(command);

        assertThat(repository.data).containsKey(plan.id());
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.planId()).isEqualTo(plan.id());
            assertThat(event.freeSpots()).isEqualTo(1);
        });
    }

    @Test
    void invalidPlansAreNeitherStoredNorAnnounced() {
        var command = new PublishPlanCommand(ana(), Activity.PADEL, "Padel match", null, COURTS,
                NOW.plus(Duration.ofDays(1)), 1, null);
        assertThatThrownBy(() -> service.publish(command)).isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.data).isEmpty();
        assertThat(events).isEmpty();
    }

    @Test
    void findsAPlanOrFailsWhenItDoesNotExist() {
        var plan = repository.save(padelPlan(ana(), Duration.ofHours(1), CLOCK));
        assertThat(service.plan(plan.id())).isSameAs(plan);
        assertThatThrownBy(() -> service.plan(UUID.randomUUID())).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void upcomingPlansOfTheOrganizerAreSortedByStart() {
        var organizer = ana();
        var later = repository.save(padelPlan(organizer, Duration.ofHours(5), CLOCK));
        var sooner = repository.save(padelPlan(organizer, Duration.ofHours(1), CLOCK));
        repository.save(padelPlan(ana(), Duration.ofHours(2), CLOCK));

        assertThat(service.upcomingPlansOrganizedBy(organizer.id())).containsExactly(sooner, later);
    }

    @Test
    void nearbyPlansComeWithTheirDistance() {
        var near = repository.save(padelPlan(ana(), Duration.ofHours(1), CLOCK));
        var farPoint = new MeetingPoint("Park", COURTS.latitude() + 0.01, COURTS.longitude());
        var far = repository.save(Plan.publish(ana(), Activity.RUNNING, "Easy run", null, farPoint,
                NOW.plus(Duration.ofHours(1)), 2, null, CLOCK));
        var search = new NearbySearch(COURTS.latitude(), COURTS.longitude(), 5_000, null, null, UUID.randomUUID());

        var nearby = service.nearbyPlans(search);

        assertThat(nearby).extracting(n -> n.plan()).containsExactly(near, far);
        assertThat(nearby.get(0).distanceMeters()).isZero();
        assertThat(nearby.get(1).distanceMeters()).isBetween(1_100.0, 1_120.0);
    }
}

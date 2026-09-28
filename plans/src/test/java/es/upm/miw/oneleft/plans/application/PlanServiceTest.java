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
import static es.upm.miw.oneleft.plans.PlanFixtures.PISTAS;
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

        /** Filtra como PostGIS pero en memoria; la consulta real se prueba en JpaPlanRepositoryTest. */
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
    }

    private final InMemoryPlans repository = new InMemoryPlans();
    private final List<PlanPublished> events = new ArrayList<>();
    private final PlanEventPublisher publisher = events::add;
    private final PlanService service = new PlanService(repository, publisher, CLOCK);

    @Test
    void publishingStoresThePlanAndEmitsTheEvent() {
        var command = new PublishPlanCommand(ana(), Activity.PADEL, "Partido de pádel", null, PISTAS,
                NOW.plus(Duration.ofHours(1)), 1, Level.INTERMEDIO);

        var plan = service.publish(command);

        assertThat(repository.data).containsKey(plan.id());
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.planId()).isEqualTo(plan.id());
            assertThat(event.freeSpots()).isEqualTo(1);
        });
    }

    @Test
    void invalidPlansAreNeitherStoredNorAnnounced() {
        var command = new PublishPlanCommand(ana(), Activity.PADEL, "Partido de pádel", null, PISTAS,
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
        var farPoint = new MeetingPoint("Parque", PISTAS.latitude() + 0.01, PISTAS.longitude());
        var far = repository.save(Plan.publish(ana(), Activity.RUNNING, "Rodaje suave", null, farPoint,
                NOW.plus(Duration.ofHours(1)), 2, null, CLOCK));
        var search = new NearbySearch(PISTAS.latitude(), PISTAS.longitude(), 5_000, null, null, UUID.randomUUID());

        var nearby = service.nearbyPlans(search);

        assertThat(nearby).extracting(n -> n.plan()).containsExactly(near, far);
        assertThat(nearby.get(0).distanceMeters()).isZero();
        assertThat(nearby.get(1).distanceMeters()).isBetween(1_100.0, 1_120.0);
    }
}

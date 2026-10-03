package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration with a real PostgreSQL + PostGIS: Flyway schema, geometry type and queries.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JpaPlanRepositoryTest {

    @Autowired
    private JpaPlanRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private final Clock clock = Clock.systemUTC();

    @Test
    void savesThePlanWithItsMeetingPointAsAPostgisPoint() {
        var plan = repository.save(padelPlan(ana(), Duration.ofHours(1), clock));

        var loaded = repository.findById(plan.id()).orElseThrow();
        assertThat(loaded.meetingPoint()).isEqualTo(plan.meetingPoint());
        assertThat(loaded.title()).isEqualTo(plan.title());
        assertThat(loaded.freeSpots()).isEqualTo(1);

        var wkt = jdbc.queryForObject("select ST_AsText(location) from plan where id = ?", String.class, plan.id());
        assertThat(wkt).isEqualTo("POINT(-3.6287 40.3912)");
        var srid = jdbc.queryForObject("select ST_SRID(location) from plan where id = ?", Integer.class, plan.id());
        assertThat(srid).isEqualTo(4326);
    }

    @Test
    void findsTheUpcomingPlansOfAnOrganizer() {
        var organizer = ana();
        var later = repository.save(padelPlan(organizer, Duration.ofHours(6), clock));
        var sooner = repository.save(padelPlan(organizer, Duration.ofHours(1), clock));

        var upcoming = repository.findByOrganizerStartingAfter(organizer.id(), clock.instant());

        assertThat(upcoming).extracting(p -> p.id()).containsExactly(sooner.id(), later.id());
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    /** Each test searches at a different point of the ocean so that it does not see other tests' plans. */
    private static MeetingPoint somewhere() {
        var random = ThreadLocalRandom.current();
        return new MeetingPoint("Test point", random.nextDouble(-50, -20), random.nextDouble(-140, -100));
    }

    private static MeetingPoint north(MeetingPoint from, double degrees) {
        return new MeetingPoint("Up north", from.latitude() + degrees, from.longitude());
    }

    private Plan plan(Organizer organizer, Activity activity, MeetingPoint where, Duration startsIn) {
        return repository.save(Plan.publish(organizer, activity, "Test plan", null, where,
                clock.instant().plus(startsIn), 2, null, clock));
    }

    @Test
    void findsOpenPlansWithinTheRadiusFromTheNearest() {
        var here = somewhere();
        var at1km = plan(ana(), Activity.PADEL, north(here, 0.009), Duration.ofHours(1));
        var at0km = plan(ana(), Activity.RUNNING, here, Duration.ofHours(2));
        plan(ana(), Activity.PADEL, north(here, 0.03), Duration.ofHours(1)); // ~3.3 km: outside the radius
        var search = new NearbySearch(here.latitude(), here.longitude(), 2_000, null, null, null);

        var found = repository.findOpenNearby(search, clock.instant(), 50);

        assertThat(found).extracting(Plan::id).containsExactly(at0km.id(), at1km.id());
        assertThat(repository.findOpenNearby(search, clock.instant(), 1)).extracting(Plan::id)
                .containsExactly(at0km.id());
    }

    @Test
    void filtersByActivityStartAndOrganizerAndSkipsFullPlans() {
        var here = somewhere();
        var me = ana();
        var padel = plan(ana(), Activity.PADEL, here, Duration.ofHours(1));
        plan(ana(), Activity.CINEMA, here, Duration.ofHours(1));
        plan(ana(), Activity.PADEL, here, Duration.ofHours(5));
        plan(me, Activity.PADEL, here, Duration.ofHours(1));
        repository.save(new Plan(UUID.randomUUID(), ana(), Activity.PADEL, "Full plan", null, here,
                clock.instant().plus(Duration.ofHours(1)), 2, 2, null, PlanStatus.OPEN, clock.instant()));
        var search = new NearbySearch(here.latitude(), here.longitude(), 1_000, Set.of(Activity.PADEL),
                Duration.ofHours(3), me.id());

        assertThat(repository.findOpenNearby(search, clock.instant(), 50)).extracting(Plan::id)
                .containsExactly(padel.id());
    }

    @Test
    void findsThePlansWithALifecycleStepDueAndKeepsTheReminder() {
        var now = clock.instant();
        // PostgreSQL rounds times to microseconds: the test works with the plans as they come back from it
        var soon = stored(repository.save(padelPlan(ana(), Duration.ofMinutes(40), clock)));
        var later = stored(repository.save(padelPlan(ana(), Duration.ofHours(3), clock)));
        var reminded = soon.remind(soon.startsAt().minus(Plan.REMINDER_LEAD)).plan();
        repository.save(reminded);
        assertThat(stored(soon).remindedAt())
                .isEqualTo(reminded.remindedAt().truncatedTo(ChronoUnit.MICROS));

        // In 15 minutes «later» is still far, and «soon» has its reminder already
        assertThat(repository.findDueForLifecycle(now.plus(Duration.ofMinutes(15))))
                .doesNotContain(soon.id(), later.id());
        // At its start, «soon» starts; «later» is within its reminder time
        assertThat(repository.findDueForLifecycle(soon.startsAt()))
                .contains(soon.id()).doesNotContain(later.id());
        assertThat(repository.findDueForLifecycle(later.startsAt().minus(Plan.REMINDER_LEAD))).contains(later.id());

        var started = repository.save(stored(soon).advance(soon.startsAt()));
        assertThat(started.status()).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(repository.findDueForLifecycle(soon.startsAt().plusSeconds(60))).doesNotContain(soon.id());
        assertThat(repository.findDueForLifecycle(soon.startsAt().plus(Plan.DURATION))).contains(soon.id());
    }

    private Plan stored(Plan plan) {
        return repository.findById(plan.id()).orElseThrow();
    }

    @Test
    void theNearbySearchUsesTheGeographyIndex() {
        var plan = jdbc.execute((ConnectionCallback<String>) connection -> {
            try (var statement = connection.createStatement()) {
                // With few rows PostgreSQL prefers a sequential scan; disable it to see the chosen index
                statement.execute("SET enable_seqscan = off");
                var result = statement.executeQuery("""
                        EXPLAIN SELECT id FROM plan WHERE ST_DWithin(location::geography,
                            ST_SetSRID(ST_MakePoint(-3.6, 40.4), 4326)::geography, 1000)""");
                var lines = new StringBuilder();
                while (result.next()) {
                    lines.append(result.getString(1)).append('\n');
                }
                statement.execute("RESET enable_seqscan");
                return lines.toString();
            }
        });
        assertThat(plan).contains("plan_location_geography");
    }
}

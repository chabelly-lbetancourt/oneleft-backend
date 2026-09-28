package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integración con PostgreSQL + PostGIS reales: esquema de Flyway, tipo geometry y consultas.
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
}

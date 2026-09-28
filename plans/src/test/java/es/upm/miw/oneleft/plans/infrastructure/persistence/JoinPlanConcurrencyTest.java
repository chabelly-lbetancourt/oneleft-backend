package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.ConcurrentPlanUpdateException;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.port.in.JoinPlanUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU-005 against a real PostgreSQL: optimistic locking with the version column.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JoinPlanConcurrencyTest {

    @Autowired
    private JoinPlanUseCase joinPlan;

    @Autowired
    private JpaPlanRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void whenManyAskForTheLastSpotAtTheSameTimeOnlyOneGetsIt() throws Exception {
        var plan = repository.save(padelPlan(ana(), Duration.ofHours(1), Clock.systemUTC()));
        var start = new CountDownLatch(1);
        var people = 6;
        List<Callable<String>> attempts = new ArrayList<>();
        for (var i = 0; i < people; i++) {
            var name = "Person " + i;
            attempts.add(() -> {
                start.await();
                try {
                    joinPlan.join(plan.id(), UUID.randomUUID(), name);
                    return "joined";
                } catch (JoinRejectedException rejected) {
                    return rejected.code();
                }
            });
        }
        try (var executor = Executors.newFixedThreadPool(people)) {
            List<Future<String>> results = attempts.stream().map(executor::submit).toList();
            start.countDown();
            var outcomes = new ArrayList<String>();
            for (var result : results) {
                outcomes.add(result.get());
            }
            assertThat(outcomes).containsOnlyOnce("joined");
            assertThat(outcomes).filteredOn(outcome -> !outcome.equals("joined"))
                    .allMatch(code -> code.equals("plan.full") || code.equals("plan.busy"));
        }

        var stored = repository.findById(plan.id()).orElseThrow();
        assertThat(stored.occupied()).isEqualTo(1);
        assertThat(stored.status()).isEqualTo(PlanStatus.FULL);
        assertThat(stored.participants()).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from plan_participant where plan_id = ?", Integer.class,
                plan.id())).isEqualTo(1);
    }

    @Test
    void savingAStaleCopyIsAConcurrentUpdate() {
        var plan = repository.save(padelPlan(ana(), Duration.ofHours(1), Clock.systemUTC()));
        var saved = repository.save(plan.join(UUID.randomUUID(), "Lucía", Clock.systemUTC()));
        assertThat(saved.version()).isGreaterThan(plan.version());

        // A copy read before that change still has the old version
        var stale = plan.join(UUID.randomUUID(), "Diego", Clock.systemUTC());
        assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(ConcurrentPlanUpdateException.class);
    }

    @Test
    void theParticipantsArePersistedInJoinOrder() throws ExecutionException, InterruptedException {
        var plan = repository.save(padelPlan(ana(), Duration.ofHours(1), Clock.systemUTC()));
        var bigger = repository.save(new es.upm.miw.oneleft.plans.domain.model.Plan(plan.id(), plan.organizer(),
                plan.activity(), plan.title(), plan.description(), plan.meetingPoint(), plan.startsAt(), 3, 0,
                plan.level(), plan.status(), plan.publishedAt(), List.of(), plan.version()));
        joinPlan.join(bigger.id(), UUID.randomUUID(), "Lucía");
        joinPlan.join(bigger.id(), UUID.randomUUID(), "Diego");

        assertThat(repository.findById(plan.id()).orElseThrow().participants())
                .extracting(participant -> participant.name()).containsExactly("Lucía", "Diego");
    }
}

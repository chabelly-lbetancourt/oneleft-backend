package es.upm.miw.oneleft.notifications.infrastructure;

import es.upm.miw.oneleft.notifications.TestcontainersConfiguration;
import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.Candidate;
import es.upm.miw.oneleft.notifications.domain.model.Level;
import es.upm.miw.oneleft.notifications.domain.model.NoticeHistory;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.RankedCandidate;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeBatches;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The batches and the record of notices of HU-012 with a real PostgreSQL. What the test creates is removed at the end.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RankedNoticesIntegrationTest {

    @Autowired
    private NoticeBatches batches;

    @Autowired
    private NoticeLog notices;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID planId = UUID.randomUUID();
    private final UUID anotherPlanId = UUID.randomUUID();
    private final List<UUID> people = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @AfterEach
    void clean() {
        batches.close(planId);
        batches.close(anotherPlanId);
        for (var person : people) {
            jdbc.update("DELETE FROM sent_notice WHERE user_id = ?", person);
        }
    }

    private PublishedPlan plan(UUID id, Level level) {
        return new PublishedPlan(id, UUID.randomUUID(), Activity.PADEL, "Pádel", "Pistas", 40.39, -3.63,
                now.plus(Duration.ofHours(2)), 2, level);
    }

    private static List<RankedCandidate> ranked(List<UUID> people) {
        var ranked = new java.util.ArrayList<RankedCandidate>();
        for (var i = 0; i < people.size(); i++) {
            ranked.add(new RankedCandidate(new Candidate(people.get(i), 100 * i, 3_000, false, NoticeHistory.NONE),
                    1.0 - i / 10.0));
        }
        return ranked;
    }

    @Test
    void thePeopleWaitInOrderAndEachBatchTakesTheNextOnes() {
        var plan = plan(planId, Level.INTERMEDIATE);
        batches.schedule(plan, "model", ranked(people), now.plusSeconds(180));

        assertThat(batches.due(now.plusSeconds(179))).isEmpty();
        var due = batches.due(now.plusSeconds(180));
        assertThat(due).singleElement().satisfies(batched -> {
            assertThat(batched.plan()).isEqualTo(plan);
            assertThat(batched.ranking()).isEqualTo("model");
            assertThat(batched.nextBatch()).isEqualTo(1);
        });

        assertThat(batches.takeNext(planId, 2)).containsExactly(people.get(0), people.get(1));
        batches.rescheduleOrFinish(planId, now.plusSeconds(360));
        assertThat(batches.due(now.plusSeconds(200))).isEmpty();
        assertThat(batches.due(now.plusSeconds(360))).singleElement()
                .satisfies(batched -> assertThat(batched.nextBatch()).isEqualTo(2));

        assertThat(batches.takeNext(planId, 2)).containsExactly(people.get(2));
        batches.rescheduleOrFinish(planId, now.plusSeconds(540));
        assertThat(batches.due(now.plusSeconds(10_000))).as("nobody is left: the plan is finished").isEmpty();
    }

    @Test
    void closingAPlanForgetsItsPeopleAndOnlyThatPlan() {
        batches.schedule(plan(planId, null), "distance", ranked(people), now);
        batches.schedule(plan(anotherPlanId, null), "distance", ranked(people.subList(0, 1)), now);

        batches.close(planId);

        assertThat(batches.due(now)).extracting(batched -> batched.plan().planId()).containsExactly(anotherPlanId);
        assertThat(batches.takeNext(planId, 5)).isEmpty();
        assertThat(batches.takeNext(anotherPlanId, 5)).containsExactly(people.get(0));
    }

    @Test
    void schedulingAgainReplacesThePreviousBatches() {
        batches.schedule(plan(planId, null), "model", ranked(people), now);
        batches.schedule(plan(planId, null), "model", ranked(people.subList(0, 1)), now);

        assertThat(batches.takeNext(planId, 5)).containsExactly(people.get(0));
    }

    @Test
    void theHistoryCountsWhatWasSentWhatWorkedAndWhatArrivedToday() {
        var startOfDay = now.minus(Duration.ofHours(1));
        notices.record(people.get(0), planId, now.minus(Duration.ofDays(2)), "model", 0);
        notices.record(people.get(0), anotherPlanId, now.minus(Duration.ofMinutes(10)), "model", 1);
        notices.record(people.get(1), planId, now.minus(Duration.ofDays(200)), "model", 0);
        assertThat(notices.record(people.get(0), planId, now, "model", 0)).as("only once per plan").isFalse();

        notices.accepted(people.get(0), planId, now);
        // Someone who was never notified, or a join that is repeated, changes nothing
        notices.accepted(people.get(2), planId, now);
        notices.accepted(people.get(0), planId, now.plusSeconds(60));

        var history = notices.historyOf(people, now.minus(Duration.ofDays(90)), startOfDay);

        assertThat(history).containsOnlyKeys(people.get(0));
        assertThat(history.get(people.get(0))).isEqualTo(new NoticeHistory(2, 1, 1));
        assertThat(jdbc.queryForObject("SELECT ranking || '#' || batch FROM sent_notice WHERE user_id = ? "
                + "AND plan_id = ?", String.class, people.get(0), anotherPlanId)).isEqualTo("model#1");
        assertThat(notices.historyOf(List.of(), now, now)).isEmpty();
    }
}

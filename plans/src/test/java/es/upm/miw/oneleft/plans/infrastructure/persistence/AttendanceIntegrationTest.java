package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.CheckIn;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanAttendanceClosed;
import es.upm.miw.oneleft.plans.domain.port.in.CloseAttendanceUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.CheckInRepository;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import es.upm.miw.oneleft.plans.infrastructure.messaging.RabbitConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-009 with a real PostgreSQL and RabbitMQ: the plans whose check-in has closed publish who arrived and who did not,
 * once each; the plans still to come wait. The plans created are deleted at the end (their check-ins go with them).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AttendanceIntegrationTest {

    private static final ParameterizedTypeReference<PlanAttendanceClosed> EVENT = new ParameterizedTypeReference<>() {
    };

    @Autowired
    private PlanRepository plans;

    @Autowired
    private CheckInRepository checkIns;

    @Autowired
    private CloseAttendanceUseCase attendance;

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private TopicExchange plansExchange;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<UUID> created = new ArrayList<>();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    /** Not exclusive to a connection: it survives a connection that is renewed while the test runs */
    private final Queue queue = new Queue("test.attendance." + UUID.randomUUID(), false, false, false);

    @AfterEach
    void deleteThePlansAndTheQueue() {
        created.forEach(id -> jdbc.update("DELETE FROM plan WHERE id = ?", id));
        admin.deleteQueue(queue.getName());
    }

    private Plan planStartingAt(Instant startsAt, Clock publishedWith) {
        var plan = plans.save(Plan.publish(new Organizer(ana, "Ana"), Activity.PADEL, "Padel 2 vs 2", null,
                new MeetingPoint("Courts", 40.39, -3.62), startsAt, 2, null, publishedWith)
                .join(lucia, "Lucía", publishedWith));
        created.add(plan.id());
        return plan;
    }

    private Instant closedAt(UUID planId) {
        return jdbc.queryForObject("SELECT attendance_closed_at FROM plan WHERE id = ?", Instant.class, planId);
    }

    /** The events received until none is left, only those of the plans of this test. */
    private List<PlanAttendanceClosed> received(String queue) {
        var events = new ArrayList<PlanAttendanceClosed>();
        for (var event = rabbit.receiveAndConvert(queue, 3000, EVENT); event != null;
             event = rabbit.receiveAndConvert(queue, 1000, EVENT)) {
            if (created.contains(event.planId())) {
                events.add(event);
            }
        }
        return events;
    }

    @Test
    void thePlansWhoseCheckInClosedPublishWhoArrivedOnce() {
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(plansExchange).with(RabbitConfig.PLAN_ATTENDANCE_CLOSED));
        var twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        var held = planStartingAt(twoHoursAgo.instant().plus(Duration.ofHours(1)), twoHoursAgo);
        checkIns.save(new CheckIn(held.id(), lucia, "Lucía", held.startsAt()));
        var upcoming = planStartingAt(Instant.now().plus(Duration.ofHours(1)), Clock.systemUTC());

        attendance.closeDue();

        assertThat(received(queue.getName())).singleElement().satisfies(event -> {
            assertThat(event.planId()).isEqualTo(held.id());
            assertThat(event.attendeeIds()).containsExactly(lucia);
            assertThat(event.absenteeIds()).containsExactly(ana);
            assertThat(event.full()).isFalse();
        });
        assertThat(closedAt(held.id())).isNotNull();
        assertThat(closedAt(upcoming.id())).isNull();

        attendance.closeDue();
        assertThat(received(queue.getName())).isEmpty();
    }
}

package es.upm.miw.oneleft.plans.infrastructure.rest;

import com.jayway.jsonpath.JsonPath;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.ReputationPerks;
import es.upm.miw.oneleft.plans.domain.port.out.PerksDirectory;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import es.upm.miw.oneleft.plans.infrastructure.messaging.RabbitConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The organizer cancels their plan, end to end with a real PostgreSQL and RabbitMQ. The reputation service is
 * simulated. The plans published and the queue of the test are erased at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CancelPlanControllerTest {

    private static final ParameterizedTypeReference<PlanCancelled> EVENT = new ParameterizedTypeReference<>() {
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlanRepository plans;

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private TopicExchange plansExchange;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private PerksDirectory perks;

    private final UUID organizer = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();

    /** Durable and not exclusive, like the one of AttendanceIntegrationTest; deleted at the end. */
    private final Queue queue = new Queue("test.cancelled." + UUID.randomUUID(), true, false, false);

    @BeforeEach
    void listenToTheCancellations() {
        when(perks.perksOf(any())).thenReturn(ReputationPerks.BASE);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(plansExchange).with(RabbitConfig.PLAN_CANCELLED));
    }

    @AfterEach
    void deleteThePlansAndTheQueue() {
        jdbc.update("DELETE FROM plan WHERE organizer_id = ?", organizer);
        admin.deleteQueue(queue.getName());
    }

    private static RequestPostProcessor user(UUID subject, String name) {
        return jwt().jwt(token -> token.subject(subject.toString()).claim("name", name));
    }

    private ResultActions publish() throws Exception {
        var body = """
                {"activity": "PADEL", "title": "Padel match", "spots": 1, "startsAt": "%s",
                 "meetingPoint": {"name": "Courts", "latitude": 40.39, "longitude": -3.62}}"""
                .formatted(Instant.now().plus(Duration.ofHours(2)));
        return mockMvc.perform(post(PlanController.PLANS).with(user(organizer, "Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String published() throws Exception {
        return JsonPath.read(publish().andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString(), "$.id");
    }

    private static String plan(Object planId) {
        return PlanController.PLANS + "/" + planId;
    }

    /** The cancellations of the plans of this test received until none is left. */
    private List<PlanCancelled> received() {
        var events = new ArrayList<PlanCancelled>();
        for (var event = rabbit.receiveAndConvert(queue.getName(), 3000, EVENT); event != null;
             event = rabbit.receiveAndConvert(queue.getName(), 1000, EVENT)) {
            if (organizer.equals(event.organizerId())) {
                events.add(event);
            }
        }
        return events;
    }

    @Test
    void theOrganizerCancelsOnceAndWhoeverJoinedOrWasWaitingIsTold() throws Exception {
        var planId = published();
        mockMvc.perform(post(plan(planId) + "/participants").with(user(lucia, "Lucía"))).andExpect(status().isOk());
        mockMvc.perform(post(plan(planId) + "/waitlist").with(user(diego, "Diego"))).andExpect(status().isOk());

        mockMvc.perform(delete(plan(planId)).with(user(organizer, "Ana"))).andExpect(status().isNoContent());
        // Idempotent: the plan is already cancelled and nobody is told again
        mockMvc.perform(delete(plan(planId)).with(user(organizer, "Ana"))).andExpect(status().isNoContent());

        mockMvc.perform(get(plan(planId)).with(user(lucia, "Lucía")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.waitlist", hasSize(0)));
        mockMvc.perform(get(PlanController.PLANS + PlanController.MINE).with(user(organizer, "Ana")))
                .andExpect(jsonPath("$", hasSize(0)));
        // Nobody takes a spot of a cancelled plan
        mockMvc.perform(post(plan(planId) + "/participants").with(user(diego, "Diego")))
                .andExpect(status().isConflict());
        assertThat(received()).singleElement().satisfies(event -> {
            assertThat(event.planId()).hasToString(planId);
            assertThat(event.reason()).isEqualTo(PlanCancelled.Reason.ORGANIZER);
            assertThat(event.title()).isEqualTo("Padel match");
            assertThat(event.placeName()).isEqualTo("Courts");
            assertThat(event.recipientIds()).containsExactly(lucia, diego);
            assertThat(event.occurredAt()).isBefore(event.startsAt());
        });
    }

    @Test
    void onlyTheOrganizerWithATokenCancelsAPlanThatExists() throws Exception {
        var planId = published();

        mockMvc.perform(delete(plan(planId))).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(plan(planId)).with(user(lucia, "Lucía")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("plan.notOrganizer"));
        mockMvc.perform(delete(plan(UUID.randomUUID())).with(user(organizer, "Ana")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("plan.notFound"));

        mockMvc.perform(get(plan(planId)).with(user(organizer, "Ana")))
                .andExpect(jsonPath("$.status").value("OPEN"));
        assertThat(received()).isEmpty();
    }

    @Test
    void aPlanThatHasAlreadyStartedIsNotCancelled() throws Exception {
        var anHourAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        var started = plans.save(Plan.publish(new Organizer(organizer, "Ana"), Activity.PADEL, "Padel match", null,
                new MeetingPoint("Courts", 40.39, -3.62), anHourAgo.instant().plus(Duration.ofMinutes(30)), 1, null,
                anHourAgo));

        mockMvc.perform(delete(plan(started.id())).with(user(organizer, "Ana")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.started"));

        assertThat(jdbc.queryForObject("SELECT status FROM plan WHERE id = ?", String.class, started.id()))
                .isEqualTo("OPEN");
        assertThat(received()).isEmpty();
    }

    @Test
    void aCancelledPlanNoLongerCountsForTheLimitOfPlansAtOnce() throws Exception {
        var first = published();
        for (var others = 1; others < ReputationPerks.BASE_PLANS; others++) {
            publish().andExpect(status().isCreated());
        }
        publish().andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("plan.tooManyActive"));

        mockMvc.perform(delete(plan(first)).with(user(organizer, "Ana"))).andExpect(status().isNoContent());

        publish().andExpect(status().isCreated());
        publish().andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("plan.tooManyActive"));
    }
}

package es.upm.miw.oneleft.plans.infrastructure.rest;

import com.jayway.jsonpath.JsonPath;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-023 end to end with a real PostgreSQL and RabbitMQ: leave a plan and waiting list.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class WaitlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor user(String subject, String name) {
        return jwt().jwt(token -> token.subject(subject).claim("name", name));
    }

    private String publish(String organizer, int spots) throws Exception {
        var body = """
                {"activity": "PADEL", "title": "Padel match", "spots": %d, "startsAt": "%s",
                 "meetingPoint": {"name": "Courts", "latitude": 40.39, "longitude": -3.62}}"""
                .formatted(spots, Instant.now().plus(Duration.ofHours(1)));
        var response = mockMvc.perform(post(PlanController.PLANS).with(user(organizer, "Ana"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private static String plan(String planId) {
        return PlanController.PLANS + "/" + planId;
    }

    private MockHttpServletResponse events(String userId, String name) throws Exception {
        var stream = mockMvc.perform(get(PlanController.PLANS + PlanController.EVENTS_STREAM).with(user(userId, name)))
                .andExpect(request().asyncStarted()).andReturn().getResponse();
        assertThat(stream.getContentAsString()).contains("event:ready");
        return stream;
    }

    @Test
    void theFirstPersonWaitingTakesTheSpotAndEveryoneIsTold() throws Exception {
        var organizer = UUID.randomUUID().toString();
        var lucia = UUID.randomUUID().toString();
        var diego = UUID.randomUUID().toString();
        var marta = UUID.randomUUID().toString();
        var planId = publish(organizer, 1);
        mockMvc.perform(post(plan(planId) + "/participants").with(user(lucia, "Lucía"))).andExpect(status().isOk());

        mockMvc.perform(post(plan(planId) + "/waitlist").with(user(diego, "Diego")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULL"))
                .andExpect(jsonPath("$.waitlist", hasSize(1)))
                .andExpect(jsonPath("$.waitlist[0].name").value("Diego"));
        mockMvc.perform(post(plan(planId) + "/waitlist").with(user(marta, "Marta"))).andExpect(status().isOk());

        var organizerEvents = events(organizer, "Ana");
        var diegoEvents = events(diego, "Diego");

        mockMvc.perform(delete(plan(planId) + "/participants/me").with(user(lucia, "Lucía")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULL"))
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.participants[0].name").value("Diego"))
                .andExpect(jsonPath("$.waitlist[0].name").value("Marta"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(organizerEvents.getContentAsString(StandardCharsets.UTF_8))
                    .contains("event:plan-left").contains("\"participantName\":\"Lucía\"")
                    .contains("\"promotedName\":\"Diego\"");
            assertThat(diegoEvents.getContentAsString(StandardCharsets.UTF_8))
                    .contains("event:plan-spot").contains("\"planId\":\"" + planId + "\"");
        });

        // Marta leaves the list; the next departure frees the spot and reopens the plan
        mockMvc.perform(delete(plan(planId) + "/waitlist/me").with(user(marta, "Marta")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlist", hasSize(0)));
        mockMvc.perform(delete(plan(planId) + "/participants/me").with(user(diego, "Diego")))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.freeSpots").value(1));
    }

    @Test
    void explainsWhyEachRequestIsRejected() throws Exception {
        var organizer = UUID.randomUUID().toString();
        var lucia = UUID.randomUUID().toString();
        var planId = publish(organizer, 2);

        mockMvc.perform(post(plan(planId) + "/waitlist").with(user(lucia, "Lucía")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.notFull"));
        mockMvc.perform(delete(plan(planId) + "/participants/me").with(user(lucia, "Lucía")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.notParticipant"));
        mockMvc.perform(delete(plan(planId) + "/waitlist/me").with(user(lucia, "Lucía")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.notWaiting"));
        mockMvc.perform(delete(plan(UUID.randomUUID().toString()) + "/participants/me").with(user(lucia, "Lucía")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(plan(planId) + "/participants/me")).andExpect(status().isUnauthorized());
    }
}

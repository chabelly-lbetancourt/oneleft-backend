package es.upm.miw.oneleft.plans.infrastructure.rest;

import com.jayway.jsonpath.JsonPath;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-005 end to end with a real PostgreSQL and RabbitMQ.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class JoinPlanControllerTest {

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

    private String participants(String planId) {
        return PlanController.PLANS + "/" + planId + "/participants";
    }

    @Test
    void joiningTakesASpotAndTheLastOneClosesThePlan() throws Exception {
        var planId = publish(UUID.randomUUID().toString(), 2);
        var lucia = UUID.randomUUID().toString();

        mockMvc.perform(post(participants(planId)).with(user(lucia, "Lucía")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freeSpots").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.participants[0].userId").value(lucia))
                .andExpect(jsonPath("$.participants[0].name").value("Lucía"));

        mockMvc.perform(post(participants(planId)).with(user(UUID.randomUUID().toString(), "Diego")))
                .andExpect(jsonPath("$.freeSpots").value(0))
                .andExpect(jsonPath("$.status").value("FULL"));

        mockMvc.perform(post(participants(planId)).with(user(UUID.randomUUID().toString(), "Marta")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.full"));
        mockMvc.perform(get(PlanController.PLANS + "/" + planId).with(user(lucia, "Lucía")))
                .andExpect(jsonPath("$.participants", hasSize(2)));
    }

    @Test
    void rejectsTheOrganizerRepeatedJoinsAndUnknownPlans() throws Exception {
        var organizer = UUID.randomUUID().toString();
        var planId = publish(organizer, 3);
        mockMvc.perform(post(participants(planId)).with(user(organizer, "Ana")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.ownPlan"));

        var lucia = UUID.randomUUID().toString();
        mockMvc.perform(post(participants(planId)).with(user(lucia, "Lucía"))).andExpect(status().isOk());
        mockMvc.perform(post(participants(planId)).with(user(lucia, "Lucía")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.alreadyJoined"));

        mockMvc.perform(post(participants(UUID.randomUUID().toString())).with(user(lucia, "Lucía")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("plan.notFound"));
        mockMvc.perform(post(participants(planId))).andExpect(status().isUnauthorized());
    }

    @Test
    void theOrganizerIsNotifiedInRealTime() throws Exception {
        var organizer = UUID.randomUUID().toString();
        var planId = publish(organizer, 2);
        var stream = mockMvc.perform(get(PlanController.PLANS + PlanController.EVENTS_STREAM)
                        .with(user(organizer, "Ana")))
                .andExpect(request().asyncStarted())
                .andReturn().getResponse();
        assertThat(stream.getContentAsString()).contains("event:ready");

        // Someone else's plan is not announced to this organizer
        var otherPlan = publish(UUID.randomUUID().toString(), 2);
        mockMvc.perform(post(participants(otherPlan)).with(user(UUID.randomUUID().toString(), "Diego")));
        mockMvc.perform(post(participants(planId)).with(user(UUID.randomUUID().toString(), "Lucía")));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(stream.getContentAsString()).contains("event:plan-joined"));
        // Server-Sent Events are always UTF-8 (the mock response would decode them as ISO-8859-1)
        var content = stream.getContentAsString(StandardCharsets.UTF_8);
        assertThat(content.split("event:plan-joined", -1)).hasSize(2);
        assertThat(content).contains("\"participantName\":\"Lucía\"").contains("\"freeSpots\":1")
                .contains("\"full\":false").contains(planId);
    }
}

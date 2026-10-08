package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor user(String subject) {
        return jwt().jwt(token -> token.subject(subject).claim("name", "Ana Test"));
    }

    private static String planJson(Instant startsAt, int spots) {
        return """
                {"activity": "PADEL", "title": "Padel match, one player missing", "description": "Intermediate level",
                 "meetingPoint": {"name": "Sports centre courts", "latitude": 40.3912, "longitude": -3.6287},
                 "startsAt": "%s", "spots": %d, "level": "INTERMEDIATE"}""".formatted(startsAt, spots);
    }

    @Test
    void publishesAPlanAndReturnsItsLocation() throws Exception {
        var subject = UUID.randomUUID().toString();
        var body = planJson(Instant.now().plus(Duration.ofHours(1)), 1);

        mockMvc.perform(post(PlanController.PLANS).with(user(subject))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/v1/plans/")))
                .andExpect(jsonPath("$.organizerId").value(subject))
                .andExpect(jsonPath("$.organizerName").value("Ana Test"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.freeSpots").value(1))
                .andExpect(jsonPath("$.meetingPoint.latitude").value(40.3912));

        mockMvc.perform(get(PlanController.PLANS + PlanController.MINE).with(user(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Padel match, one player missing"));
    }

    @Test
    void locationUsesThePublicHostWhenBehindTheGateway() throws Exception {
        mockMvc.perform(post(PlanController.PLANS).with(user(UUID.randomUUID().toString()))
                        .header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "api.oneleft.es")
                        .header("X-Forwarded-Port", "443")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson(Instant.now().plus(Duration.ofHours(1)), 1)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("https://api.oneleft.es/api/v1/plans/")));
    }

    @Test
    void findsAPublishedPlan() throws Exception {
        var result = mockMvc.perform(post(PlanController.PLANS).with(user(UUID.randomUUID().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson(Instant.now().plus(Duration.ofHours(2)), 3)))
                .andReturn();
        var location = result.getResponse().getHeader("Location");

        mockMvc.perform(get(location).with(user(UUID.randomUUID().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spots").value(3));
        mockMvc.perform(get(PlanController.PLANS + "/" + UUID.randomUUID()).with(user(UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsPlansOutsideTheNextHoursOrWithWrongData() throws Exception {
        var subject = UUID.randomUUID().toString();
        mockMvc.perform(post(PlanController.PLANS).with(user(subject)).contentType(MediaType.APPLICATION_JSON)
                        .content(planJson(Instant.now().plus(Duration.ofDays(2)), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The plan must start within the next 12 hours"))
                .andExpect(jsonPath("$.code").value("plan.startsTooLate"));
        mockMvc.perform(post(PlanController.PLANS).with(user(subject)).contentType(MediaType.APPLICATION_JSON)
                        .content(planJson(Instant.now().plus(Duration.ofHours(1)), 0)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(PlanController.PLANS).with(user(subject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activity\": \"PADEL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishingRequiresAuthentication() throws Exception {
        mockMvc.perform(post(PlanController.PLANS).contentType(MediaType.APPLICATION_JSON)
                        .content(planJson(Instant.now().plus(Duration.ofHours(1)), 1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publishesAPlanWithAMinimumOfParticipants() throws Exception {
        var startsAt = Instant.now().plus(Duration.ofHours(2));
        var deadline = startsAt.minus(Duration.ofHours(1));
        var body = planJson(startsAt, 3).replace("\"level\"",
                "\"minParticipants\": 2, \"minimumDeadline\": \"%s\", \"level\"".formatted(deadline));

        mockMvc.perform(post(PlanController.PLANS).with(user(UUID.randomUUID().toString()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.minimum.participants").value(2))
                .andExpect(jsonPath("$.minimum.deadline").value(deadline.toString()))
                .andExpect(jsonPath("$.minimum.confirmed").value(false));
        mockMvc.perform(post(PlanController.PLANS).with(user(UUID.randomUUID().toString()))
                        .contentType(MediaType.APPLICATION_JSON).content(planJson(startsAt, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.minimum").doesNotExist());
    }

    @Test
    void rejectsAMinimumAboveTheSpotsOrWithoutItsDeadline() throws Exception {
        var subject = UUID.randomUUID().toString();
        var startsAt = Instant.now().plus(Duration.ofHours(2));
        mockMvc.perform(post(PlanController.PLANS).with(user(subject)).contentType(MediaType.APPLICATION_JSON)
                        .content(planJson(startsAt, 2).replace("\"level\"",
                                "\"minParticipants\": 3, \"minimumDeadline\": \"%s\", \"level\""
                                        .formatted(startsAt.minus(Duration.ofHours(1))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("plan.minimum"));
        mockMvc.perform(post(PlanController.PLANS).with(user(subject)).contentType(MediaType.APPLICATION_JSON)
                        .content(planJson(startsAt, 2).replace("\"level\"", "\"minParticipants\": 1, \"level\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("plan.minimumDeadline"));
    }
}

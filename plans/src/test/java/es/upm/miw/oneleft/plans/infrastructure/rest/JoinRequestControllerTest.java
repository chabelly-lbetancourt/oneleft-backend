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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-069 end to end with a real PostgreSQL: the organizer chooses who comes. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class JoinRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final String organizer = UUID.randomUUID().toString();
    private final String lucia = UUID.randomUUID().toString();
    private final String diego = UUID.randomUUID().toString();

    private static RequestPostProcessor user(String subject, String name) {
        return jwt().jwt(token -> token.subject(subject).claim("name", name));
    }

    private String publish(boolean choose, int spots) throws Exception {
        var body = """
                {"activity": "PADEL", "title": "Pádel, falta uno", "startsAt": "%s", "spots": %d,
                 "chooseCompanion": %s,
                 "meetingPoint": {"name": "Pistas", "latitude": 40.39, "longitude": -3.62}}"""
                .formatted(Instant.now().plus(Duration.ofHours(2)), spots, choose);
        var location = mockMvc.perform(post(PlanController.PLANS).with(user(organizer, "Ana"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiresApproval").value(choose))
                .andReturn().getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String requests(String planId) {
        return PlanController.PLANS + "/" + planId + "/requests";
    }

    @Test
    void peopleAskAndTheOrganizerAcceptsOneAndTheRestAreRejectedWhenItFills() throws Exception {
        var plan = publish(true, 1);
        var asking = post(requests(plan)).contentType(MediaType.APPLICATION_JSON);

        // Joining directly is not possible: they ask
        mockMvc.perform(post(PlanController.PLANS + "/" + plan + "/participants").with(user(lucia, "Lucía")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("plan.approvalRequired"));
        mockMvc.perform(asking.with(user(lucia, "Lucía")).content("{\"message\": \"¡Me encantaría!\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("¡Me encantaría!"));
        mockMvc.perform(asking.with(user(diego, "Diego"))).andExpect(status().isCreated());
        mockMvc.perform(asking.with(user(lucia, "Lucía"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.alreadyRequested"));
        mockMvc.perform(get(PlanController.PLANS + "/" + plan).with(user(lucia, "Lucía")))
                .andExpect(jsonPath("$.myRequest.status").value("PENDING"));

        // Only the organizer sees the requests and decides
        mockMvc.perform(get(requests(plan)).with(user(organizer, "Ana"))).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get(requests(plan)).with(user(lucia, "Lucía"))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("plan.notOrganizer"));
        mockMvc.perform(post(requests(plan) + "/" + lucia + "/accept").with(user(diego, "Diego")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(requests(plan) + "/" + lucia + "/accept").with(user(organizer, "Ana")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(get(PlanController.PLANS + "/" + plan).with(user(organizer, "Ana")))
                .andExpect(jsonPath("$.status").value("FULL"))
                .andExpect(jsonPath("$.participants", hasSize(1)));
        // The plan filled up: Diego's request was rejected on its own
        mockMvc.perform(get(PlanController.PLANS + "/" + plan).with(user(diego, "Diego")))
                .andExpect(jsonPath("$.myRequest.status").value("REJECTED"));
    }

    @Test
    void theOrganizerRejectsAndAPersonWithdraws() throws Exception {
        var plan = publish(true, 2);
        var asking = post(requests(plan)).contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(asking.with(user(lucia, "Lucía"))).andExpect(status().isCreated());
        mockMvc.perform(asking.with(user(diego, "Diego"))).andExpect(status().isCreated());

        mockMvc.perform(post(requests(plan) + "/" + lucia + "/reject").with(user(organizer, "Ana")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mockMvc.perform(asking.with(user(lucia, "Lucía"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.requestRejected"));
        mockMvc.perform(delete(requests(plan) + "/me").with(user(diego, "Diego"))).andExpect(status().isNoContent());
        mockMvc.perform(delete(requests(plan) + "/me").with(user(diego, "Diego"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.noPendingRequest"));
    }

    @Test
    void aPlanWithoutApprovalIsJoinedDirectlyAndCannotBeAskedFor() throws Exception {
        var plan = publish(false, 2);

        mockMvc.perform(post(requests(plan)).with(user(lucia, "Lucía"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.approvalNotRequired"));
        mockMvc.perform(post(PlanController.PLANS + "/" + plan + "/participants").with(user(lucia, "Lucía")))
                .andExpect(status().isOk());
    }
}

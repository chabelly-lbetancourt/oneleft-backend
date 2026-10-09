package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-040 end to end with a real PostgreSQL: the group says how it is getting there, nobody else sees it. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ArrivalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlanRepository plans;

    private static RequestPostProcessor user(UUID id, String name) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("name", name));
    }

    @Test
    void theGroupSeesHowEachOneIsGettingThereAndNobodyElseDoes() throws Exception {
        var ana = UUID.randomUUID();
        var lucia = UUID.randomUUID();
        var clock = Clock.systemUTC();
        var plan = plans.save(Plan.publish(new Organizer(ana, "Ana"), Activity.PADEL, "Padel 2 vs 2", null,
                new MeetingPoint("Courts", 40.39, -3.62), clock.instant().plus(Duration.ofHours(1)), 3, null, clock)
                .join(lucia, "Lucía", clock));
        var mine = PlanController.PLANS + ArrivalController.MY_ARRIVAL.replace("{planId}", plan.id().toString());
        var detail = PlanController.PLANS + "/" + plan.id();

        mockMvc.perform(put(mine).with(user(lucia, "Lucía")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"LATE\", \"minutesLate\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arrivals", hasSize(1)))
                .andExpect(jsonPath("$.arrivals[0].name").value("Lucía"))
                .andExpect(jsonPath("$.arrivals[0].minutesLate").value(10));
        mockMvc.perform(put(mine).with(user(ana, "Ana")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ON_THE_WAY\"}"))
                .andExpect(status().isOk());

        // The organizer sees both statuses; someone outside the group, none
        mockMvc.perform(get(detail).with(user(ana, "Ana")))
                .andExpect(jsonPath("$.arrivals", hasSize(2)));
        mockMvc.perform(get(detail).with(user(UUID.randomUUID(), "Marta")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arrivals", hasSize(0)));
        mockMvc.perform(put(mine).with(user(UUID.randomUUID(), "Marta")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ON_THE_WAY\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("plan.notParticipant"));
        mockMvc.perform(put(mine).with(user(lucia, "Lucía")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"LATE\", \"minutesLate\": 7}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("arrival.minutes"));

        mockMvc.perform(delete(mine).with(user(lucia, "Lucía")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arrivals", hasSize(1)))
                .andExpect(jsonPath("$.arrivals[0].name").value("Ana"));
    }
}

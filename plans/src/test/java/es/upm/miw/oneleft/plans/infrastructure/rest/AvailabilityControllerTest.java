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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-035 end to end with a real PostgreSQL + PostGIS: free mode, what the organizer sees and joining a plan. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AvailabilityControllerTest {

    private static final String ME = PlanController.PLANS + AvailabilityController.MY_AVAILABILITY;
    private static final String FREE_PADEL = """
            {"hours": 2, "latitude": 40.3912, "longitude": -3.6287,
             "interests": [{"activity": "PADEL", "level": "INTERMEDIATE"}]}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlanRepository plans;

    private static RequestPostProcessor user(UUID id) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("name", "Lucía"));
    }

    private Plan padelPlanOf(UUID organizer, double latitude) {
        var clock = Clock.systemUTC();
        return plans.save(Plan.publish(new Organizer(organizer, "Ana"), Activity.PADEL, "Padel match", null,
                new MeetingPoint("Courts", latitude, -3.6287), clock.instant().plus(Duration.ofHours(1)), 2, null,
                clock));
    }

    private static String freePeopleOf(Plan plan) {
        return PlanController.PLANS + AvailabilityController.FREE_PEOPLE.replace("{planId}", plan.id().toString());
    }

    @Test
    void freeModeIsTurnedOnAndOff() throws Exception {
        var lucia = UUID.randomUUID();
        mockMvc.perform(get(ME).with(user(lucia))).andExpect(status().isNoContent());

        mockMvc.perform(put(ME).with(user(lucia)).contentType(MediaType.APPLICATION_JSON).content(FREE_PADEL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(40.39))
                .andExpect(jsonPath("$.interests[0].activity").value("PADEL"));
        // Turning it on again replaces it
        mockMvc.perform(put(ME).with(user(lucia)).contentType(MediaType.APPLICATION_JSON)
                        .content(FREE_PADEL.replace("\"hours\": 2", "\"hours\": 3")))
                .andExpect(status().isOk());
        mockMvc.perform(get(ME).with(user(lucia))).andExpect(status().isOk())
                .andExpect(jsonPath("$.interests", hasSize(1)));

        mockMvc.perform(delete(ME).with(user(lucia))).andExpect(status().isNoContent());
        mockMvc.perform(get(ME).with(user(lucia))).andExpect(status().isNoContent());
        mockMvc.perform(put(ME).with(user(lucia)).contentType(MediaType.APPLICATION_JSON)
                        .content(FREE_PADEL.replace("\"hours\": 2", "\"hours\": 5")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("availability.hours"));
    }

    @Test
    void onlyTheOrganizerSeesTheFreePeopleNearAndJoiningEndsTheFreeMode() throws Exception {
        var organizer = UUID.randomUUID();
        var lucia = UUID.randomUUID();
        // A plan far from any other test, so that only Lucía is around. Her zone is rounded to 40.90, the same as the
        // plan: the distance shown is the minimum step, 500 m
        var plan = padelPlanOf(organizer, 40.9);
        mockMvc.perform(put(ME).with(user(lucia)).contentType(MediaType.APPLICATION_JSON)
                        .content(FREE_PADEL.replace("40.3912", "40.902")))
                .andExpect(status().isOk());

        mockMvc.perform(get(freePeopleOf(plan)).with(user(organizer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].distanceMeters").value(500))
                .andExpect(jsonPath("$[0].level").value("INTERMEDIATE"))
                .andExpect(jsonPath("$[0].userId").doesNotExist());
        mockMvc.perform(get(freePeopleOf(plan)).with(user(UUID.randomUUID())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post(PlanController.PLANS + "/" + plan.id() + "/participants").with(user(lucia)))
                .andExpect(status().isOk());

        mockMvc.perform(get(ME).with(user(lucia))).andExpect(status().isNoContent());
        mockMvc.perform(get(freePeopleOf(plan)).with(user(organizer)))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}

package es.upm.miw.oneleft.plans.infrastructure.rest;

import com.jayway.jsonpath.JsonPath;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.ReputationPerks;
import es.upm.miw.oneleft.plans.domain.port.out.PerksDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-063 end to end with a real PostgreSQL: the priority on the waiting list is kept with the plan, and the limit of
 * plans at once. The reputation service is simulated. The plans published are erased at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PerksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private PerksDirectory perks;

    private final UUID organizer = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();
    private final UUID marta = UUID.randomUUID();
    private final UUID javier = UUID.randomUUID();

    @BeforeEach
    void nobodyHasPerks() {
        when(perks.perksOf(any())).thenReturn(ReputationPerks.BASE);
    }

    @AfterEach
    void erase() {
        jdbc.update("DELETE FROM plan WHERE organizer_id = ?", organizer);
    }

    private static RequestPostProcessor user(UUID subject, String name) {
        return jwt().jwt(token -> token.subject(subject.toString()).claim("name", name));
    }

    private ResultActions publish() throws Exception {
        var body = """
                {"activity": "PADEL", "title": "Padel match", "spots": 1, "startsAt": "%s",
                 "meetingPoint": {"name": "Courts", "latitude": 40.39, "longitude": -3.62}}"""
                .formatted(Instant.now().plus(Duration.ofHours(1)));
        return mockMvc.perform(post(PlanController.PLANS).with(user(organizer, "Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String plan(String planId) {
        return PlanController.PLANS + "/" + planId;
    }

    @Test
    void whoHasMoreReputationWaitsAheadAndTakesTheFreedSpot() throws Exception {
        String planId = JsonPath.read(publish().andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString(), "$.id");
        mockMvc.perform(post(plan(planId) + "/participants").with(user(lucia, "Lucía"))).andExpect(status().isOk());
        when(perks.perksOf(marta)).thenReturn(new ReputationPerks(2, 8));
        when(perks.perksOf(javier)).thenReturn(new ReputationPerks(2, 8));

        mockMvc.perform(post(plan(planId) + "/waitlist").with(user(diego, "Diego"))).andExpect(status().isOk());
        mockMvc.perform(post(plan(planId) + "/waitlist").with(user(marta, "Marta"))).andExpect(status().isOk());
        mockMvc.perform(post(plan(planId) + "/waitlist").with(user(javier, "Javier")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlist[0].name").value("Marta"))
                .andExpect(jsonPath("$.waitlist[1].name").value("Javier"))
                .andExpect(jsonPath("$.waitlist[2].name").value("Diego"));

        // Read again from the database, the order is the same
        mockMvc.perform(get(plan(planId)).with(user(organizer, "Ana")))
                .andExpect(jsonPath("$.waitlist[0].name").value("Marta"))
                .andExpect(jsonPath("$.waitlist[2].name").value("Diego"));
        assertThat(jdbc.queryForObject("SELECT priority FROM plan_waitlist WHERE plan_id = ?::uuid AND user_id = ?",
                Integer.class, planId, marta)).isEqualTo(2);
        mockMvc.perform(delete(plan(planId) + "/participants/me").with(user(lucia, "Lucía")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0].name").value("Marta"))
                .andExpect(jsonPath("$.waitlist[0].name").value("Javier"))
                .andExpect(jsonPath("$.waitlist[1].name").value("Diego"));
    }

    @Test
    void theSixthPlanAtOnceNeedsTheReputationAndWithoutATokenNobodyPublishes() throws Exception {
        for (var plans = 0; plans < ReputationPerks.BASE_PLANS; plans++) {
            publish().andExpect(status().isCreated());
        }

        publish().andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan.tooManyActive"))
                .andExpect(jsonPath("$.max").value(5));

        when(perks.perksOf(organizer)).thenReturn(new ReputationPerks(1, 6));
        publish().andExpect(status().isCreated());
        publish().andExpect(status().isConflict()).andExpect(jsonPath("$.max").value(6));
        mockMvc.perform(post(PlanController.PLANS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}

package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-025 end to end with a real PostGIS: the geofence of 150 m around the meeting point, one check-in per person,
 * only the group sees who arrived and the location of the device is not stored. The plans are deleted at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CheckInControllerTest {

    private static final double LATITUDE = 40.39;
    private static final double LONGITUDE = -3.62;
    /** About 145 m and 155 m north of the meeting point (one degree of latitude is about 111 km there). */
    private static final double INSIDE = LATITUDE + 0.001306;
    private static final double OUTSIDE = LATITUDE + 0.001396;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlanRepository plans;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<UUID> created = new ArrayList<>();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    @AfterEach
    void deleteThePlans() {
        // The check-ins go with the plan (ON DELETE CASCADE)
        created.forEach(id -> jdbc.update("DELETE FROM plan WHERE id = ?", id));
    }

    private static RequestPostProcessor user(UUID id, String name) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("name", name));
    }

    private Plan planStartingIn(Duration startsIn) {
        var clock = Clock.systemUTC();
        var plan = plans.save(Plan.publish(new Organizer(this.ana, "Ana"), Activity.PADEL, "Padel 2 vs 2", null,
                new MeetingPoint("Courts", LATITUDE, LONGITUDE), clock.instant().plus(startsIn), 3, null, clock)
                .join(this.lucia, "Lucía", clock));
        this.created.add(plan.id());
        return plan;
    }

    private ResultActions checkIn(Plan plan, UUID userId, String name, String body) throws Exception {
        return mockMvc.perform(post(PlanController.PLANS + CheckInController.MY_CHECK_IN, plan.id())
                .with(user(userId, name)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String at(double latitude, double longitude) {
        return "{\"latitude\": " + latitude + ", \"longitude\": " + longitude + "}";
    }

    @Test
    void onlyWithinOneHundredAndFiftyMetersOfTheMeetingPoint() throws Exception {
        var plan = planStartingIn(Duration.ofMinutes(10));

        checkIn(plan, lucia, "Lucía", at(OUTSIDE, LONGITUDE))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("checkIn.tooFar"));
        checkIn(plan, lucia, "Lucía", at(INSIDE, LONGITUDE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(lucia.toString()))
                .andExpect(jsonPath("$.name").value("Lucía"));
    }

    @Test
    void theGroupSeesWhoArrivedOnceEachAndNobodyElseDoes() throws Exception {
        var plan = planStartingIn(Duration.ofMinutes(10));
        var first = checkIn(plan, lucia, "Lucía", at(LATITUDE, LONGITUDE)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        checkIn(plan, lucia, "Lucía", at(INSIDE, LONGITUDE)).andExpect(status().isCreated())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo(first));
        var list = PlanController.PLANS + CheckInController.CHECK_INS;

        mockMvc.perform(get(list, plan.id()).with(user(ana, "Ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Lucía"));
        mockMvc.perform(get(list, plan.id()).with(user(UUID.randomUUID(), "Marta")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("plan.notParticipant"));
        checkIn(plan, UUID.randomUUID(), "Marta", at(LATITUDE, LONGITUDE))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("plan.notParticipant"));
    }

    @Test
    void notBeforeFifteenMinutesToTheStart() throws Exception {
        var plan = planStartingIn(Duration.ofHours(1));

        checkIn(plan, ana, "Ana", at(LATITUDE, LONGITUDE))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("checkIn.notYet"));
    }

    @Test
    void theLocationIsRequiredAndValidButNotStored() throws Exception {
        var plan = planStartingIn(Duration.ofMinutes(10));

        checkIn(plan, lucia, "Lucía", at(95, LONGITUDE)).andExpect(status().isBadRequest());
        checkIn(plan, lucia, "Lucía", "{\"latitude\": 40.39}").andExpect(status().isBadRequest());
        mockMvc.perform(post(PlanController.PLANS + CheckInController.MY_CHECK_IN, UUID.randomUUID())
                        .with(user(lucia, "Lucía")).contentType(MediaType.APPLICATION_JSON)
                        .content(at(LATITUDE, LONGITUDE)))
                .andExpect(status().isNotFound());

        assertThat(jdbc.queryForList("SELECT column_name FROM information_schema.columns "
                + "WHERE table_name = 'plan_check_in'", String.class))
                .containsExactlyInAnyOrder("plan_id", "user_id", "name", "checked_in_at");
    }
}

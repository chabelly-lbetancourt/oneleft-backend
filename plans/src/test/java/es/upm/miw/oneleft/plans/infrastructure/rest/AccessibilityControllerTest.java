package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-046 with a real PostGIS: the organizer says what makes the place of their plan accessible, everyone sees it and
 * the nearby search can ask for it. The plans created are removed at the end, and their features with them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccessibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<UUID> created = new ArrayList<>();
    private final double latitude = ThreadLocalRandom.current().nextDouble(-70, -65);
    private final double longitude = ThreadLocalRandom.current().nextDouble(-60, -50);
    private final String organizer = UUID.randomUUID().toString();
    private final String lucia = UUID.randomUUID().toString();

    private static RequestPostProcessor user(String subject) {
        return jwt().jwt(token -> token.subject(subject).claim("name", "Test"));
    }

    @AfterEach
    void removePlans() {
        // Their features go with the plan (ON DELETE CASCADE)
        created.forEach(id -> jdbc.update("DELETE FROM plan WHERE id = ?", id));
    }

    private ResultActions publishing(String accessibility, double north) throws Exception {
        var body = """
                {"activity": "CINEMA", "title": "Cinema %s", "startsAt": "%s", "spots": 3,
                 "meetingPoint": {"name": "Cinema", "latitude": %s, "longitude": %s%s}}"""
                .formatted(north, Instant.now().plus(Duration.ofHours(2)), latitude + north, longitude, accessibility);
        return mockMvc.perform(post(PlanController.PLANS).with(user(organizer))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String publish(String accessibility, double north) throws Exception {
        var location = publishing(accessibility, north).andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        var id = location.substring(location.lastIndexOf('/') + 1);
        created.add(UUID.fromString(id));
        return id;
    }

    private String nearby(String query) {
        return PlanController.PLANS + PlanController.NEARBY + "?latitude=" + latitude + "&longitude=" + longitude
                + "&radius=5000" + query;
    }

    @Test
    void thePlaceOfAPlanShowsWhatItsOrganizerDeclaredInTheOrderOfTheList() throws Exception {
        var accessible = publish(", \"accessibility\": [\"ACCESSIBLE_TOILET\", \"WHEELCHAIR_ACCESSIBLE\", "
                + "\"ACCESSIBLE_TOILET\"]", 0.001);
        var unknown = publish("", 0.002);
        var nothing = publish(", \"accessibility\": []", 0.003);

        mockMvc.perform(get(PlanController.PLANS + "/" + accessible).with(user(lucia))).andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingPoint.accessibility",
                        contains("WHEELCHAIR_ACCESSIBLE", "ACCESSIBLE_TOILET")));
        mockMvc.perform(get(PlanController.PLANS + "/" + unknown).with(user(lucia)))
                .andExpect(jsonPath("$.meetingPoint.accessibility", hasSize(0)));
        mockMvc.perform(get(PlanController.PLANS + "/" + nothing).with(user(lucia)))
                .andExpect(jsonPath("$.meetingPoint.accessibility", hasSize(0)));
        // Also for who organizes it and for who opens the shared link without a session
        mockMvc.perform(get(PlanController.PLANS + PlanController.MINE).with(user(organizer)))
                .andExpect(jsonPath("$[0].meetingPoint.accessibility", hasSize(2)));
        mockMvc.perform(get(PublicPlanController.PUBLIC_PLAN, accessible)).andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingPoint.accessibility",
                        contains("WHEELCHAIR_ACCESSIBLE", "ACCESSIBLE_TOILET")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_accessibility WHERE plan_id = ?", Integer.class,
                UUID.fromString(accessible))).isEqualTo(2);
    }

    @Test
    void theNearbySearchKeepsOnlyThePlacesWithEveryFeatureAskedFor() throws Exception {
        var wheelchair = publish(", \"accessibility\": [\"WHEELCHAIR_ACCESSIBLE\"]", 0.001);
        var wheelchairAndToilet = publish(", \"accessibility\": [\"WHEELCHAIR_ACCESSIBLE\", \"ACCESSIBLE_TOILET\", "
                + "\"ACCESSIBLE_PARKING\"]", 0.002);
        publish("", 0.003);

        mockMvc.perform(get(nearby("")).with(user(lucia))).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].plan.meetingPoint.accessibility", contains("WHEELCHAIR_ACCESSIBLE")));
        mockMvc.perform(get(nearby("&accessibility=WHEELCHAIR_ACCESSIBLE")).with(user(lucia)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].plan.id").value(wheelchair))
                .andExpect(jsonPath("$[1].plan.id").value(wheelchairAndToilet));
        mockMvc.perform(get(nearby("&accessibility=WHEELCHAIR_ACCESSIBLE&accessibility=ACCESSIBLE_TOILET"))
                        .with(user(lucia)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plan.id").value(wheelchairAndToilet));
        mockMvc.perform(get(nearby("&accessibility=STEP_FREE_ACCESS")).with(user(lucia)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aFeatureThatIsNotInTheListIsRejectedWhenPublishingAndWhenSearching() throws Exception {
        for (var invalid : new String[]{"[\"BLIND_FRIENDLY\"]", "[\"WHEELCHAIR_ACCESSIBLE\", \"\"]", "[null]"}) {
            publishing(", \"accessibility\": " + invalid, 0.001).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("plan.accessibility"));
        }
        mockMvc.perform(get(nearby("&accessibility=BLIND_FRIENDLY")).with(user(lucia)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("plan.accessibility"));
        mockMvc.perform(get(nearby("&accessibility=WHEELCHAIR_ACCESSIBLE"))).andExpect(status().isUnauthorized());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan WHERE organizer_id = ?", Integer.class,
                UUID.fromString(organizer))).isZero();
    }
}

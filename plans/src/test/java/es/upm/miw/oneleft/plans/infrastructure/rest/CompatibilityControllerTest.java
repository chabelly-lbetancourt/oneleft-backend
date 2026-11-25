package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.AudienceMember;
import es.upm.miw.oneleft.plans.domain.model.AudienceUnavailableException;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.PersonGender;
import es.upm.miw.oneleft.plans.domain.port.out.AudienceDirectory;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-031 with a real PostGIS: the nearby plans and the detail of a plan say how well each plan fits who looks, and the
 * nearby plans can be sorted by it. The users service is simulated: Lucía plays padel at an intermediate level. The
 * plans published are removed at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CompatibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private AudienceDirectory directory;

    private final List<String> created = new ArrayList<>();
    private final double latitude = ThreadLocalRandom.current().nextDouble(-60, -55);
    private final double longitude = ThreadLocalRandom.current().nextDouble(-40, -30);
    private final String organizer = UUID.randomUUID().toString();
    private final String lucia = UUID.randomUUID().toString();

    private static RequestPostProcessor user(String subject) {
        return jwt().jwt(token -> token.subject(subject).claim("name", "Test"));
    }

    @BeforeEach
    void luciaPlaysPadel() {
        when(directory.memberOf(any())).thenReturn(Optional.of(
                new AudienceMember(PersonGender.WOMAN, 33, "25–34", Map.of(Activity.PADEL, Level.INTERMEDIATE))));
    }

    @AfterEach
    void removePlans() {
        created.forEach(id -> jdbc.update("DELETE FROM plan WHERE id = ?::uuid", id));
    }

    /** A plan a little more to the north each time: the nearest is the first one published */
    private String publish(String activity, String level) throws Exception {
        var body = """
                {"activity": "%s", "title": "%s plan", "startsAt": "%s", "spots": 3, "level": %s,
                 "meetingPoint": {"name": "Place", "latitude": %s, "longitude": %s}}"""
                .formatted(activity, activity, Instant.now().plus(Duration.ofHours(2)),
                        level == null ? "null" : "\"" + level + "\"", latitude + 0.001 * (created.size() + 1),
                        longitude);
        var location = mockMvc.perform(post(PlanController.PLANS).with(user(organizer))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        var id = location.substring(location.lastIndexOf('/') + 1);
        created.add(id);
        return id;
    }

    private String nearby(String more) {
        return PlanController.PLANS + PlanController.NEARBY + "?latitude=" + latitude + "&longitude=" + longitude
                + "&radius=5000" + more;
    }

    @Test
    void nearbyPlansSayHowWellTheyFitMeAndCanBeSortedByIt() throws Exception {
        var cinema = publish("CINEMA", null);
        var tennis = publish("TENNIS", "BEGINNER");
        var padelAdvanced = publish("PADEL", "ADVANCED");
        var padel = publish("PADEL", "INTERMEDIATE");

        // By distance, as always, now with the compatibility of each plan
        mockMvc.perform(get(nearby("")).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].plan.id", contains(cinema, tennis, padelAdvanced, padel)))
                .andExpect(jsonPath("$[*].plan.compatibility.score", contains(40, 70, 80, 100)))
                .andExpect(jsonPath("$[0].plan.compatibility.reasons", contains("NEW_ACTIVITY", "ANY_LEVEL")))
                .andExpect(jsonPath("$[1].plan.compatibility.reasons",
                        contains("SIMILAR_ACTIVITY", "BEGINNER_FRIENDLY")))
                .andExpect(jsonPath("$[2].plan.compatibility.reasons", contains("SAME_ACTIVITY", "CLOSE_LEVEL")))
                .andExpect(jsonPath("$[3].plan.compatibility.reasons", contains("SAME_ACTIVITY", "SAME_LEVEL")));
        mockMvc.perform(get(nearby("&sort=DISTANCE")).with(user(lucia)))
                .andExpect(jsonPath("$[*].plan.id", contains(cinema, tennis, padelAdvanced, padel)));

        mockMvc.perform(get(nearby("&sort=COMPATIBILITY")).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].plan.id", contains(padel, padelAdvanced, tennis, cinema)))
                .andExpect(jsonPath("$[*].plan.compatibility.score", contains(100, 80, 70, 40)));
        // The filters are the same in any order
        mockMvc.perform(get(nearby("&sort=COMPATIBILITY&activity=PADEL")).with(user(lucia)))
                .andExpect(jsonPath("$[*].plan.id", contains(padel, padelAdvanced)));
    }

    @Test
    void thePlansWithTheSameCompatibilityStayFromTheNearest() throws Exception {
        var near = publish("TENNIS", null);
        var middle = publish("PADEL", "INTERMEDIATE");
        var far = publish("RUNNING", "BEGINNER");

        mockMvc.perform(get(nearby("&sort=COMPATIBILITY")).with(user(lucia)))
                .andExpect(jsonPath("$[*].plan.id", contains(middle, near, far)))
                .andExpect(jsonPath("$[*].plan.compatibility.score", contains(100, 70, 70)));
    }

    @Test
    void theDetailOfAPlanSaysHowWellItFitsMeButNotItsOrganizer() throws Exception {
        var padel = publish("PADEL", "BEGINNER");

        mockMvc.perform(get(PlanController.PLANS + "/" + padel).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatibility.score").value(80))
                .andExpect(jsonPath("$.compatibility.reasons", contains("SAME_ACTIVITY", "CLOSE_LEVEL")));
        mockMvc.perform(get(PlanController.PLANS + "/" + padel).with(user(organizer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatibility").doesNotExist());
    }

    @Test
    void withoutAKnownProfileThePlansComeWithoutCompatibilityAndByDistance() throws Exception {
        var near = publish("CINEMA", null);
        var far = publish("PADEL", "INTERMEDIATE");

        // Someone who has not set their date of birth and gender, and then the users service down
        when(directory.memberOf(any())).thenReturn(Optional.empty());
        mockMvc.perform(get(nearby("&sort=COMPATIBILITY")).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].plan.id", contains(near, far)))
                .andExpect(jsonPath("$[0].plan.compatibility").doesNotExist())
                .andExpect(jsonPath("$[1].plan.compatibility").doesNotExist());
        when(directory.memberOf(any())).thenThrow(new AudienceUnavailableException("down", null));
        mockMvc.perform(get(nearby("&sort=COMPATIBILITY")).with(user(lucia)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get(PlanController.PLANS + "/" + far).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatibility").doesNotExist());
    }

    @Test
    void theCompatibilityNeedsASessionAndAKnownOrder() throws Exception {
        var padel = publish("PADEL", "INTERMEDIATE");

        mockMvc.perform(get(nearby("&sort=COMPATIBILITY"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(PlanController.PLANS + "/" + padel)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(nearby("&sort=BEST")).with(user(lucia))).andExpect(status().isBadRequest());
    }
}

package es.upm.miw.oneleft.plans.infrastructure.rest;

import com.jayway.jsonpath.JsonPath;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-024 with a real PostgreSQL: a plan opened from a shared link, without a session.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class PublicPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor ana() {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("name", "Ana"));
    }

    private String publish(String title, int spots, Instant startsAt) throws Exception {
        var body = """
                {"activity": "PADEL", "title": "%s", "spots": %d, "startsAt": "%s",
                 "meetingPoint": {"name": "Albufera courts", "latitude": 40.396417, "longitude": -3.629712}}"""
                .formatted(title, spots, startsAt);
        var response = mockMvc.perform(post(PlanController.PLANS).with(ana())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void anyoneWithTheLinkSeesThePlanWithoutPeopleAndWithAnApproximatePlace() throws Exception {
        var planId = publish("Padel match", 2, Instant.now().plus(Duration.ofHours(1)));

        mockMvc.perform(get("/api/v1/public/plans/" + planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Padel match"))
                .andExpect(jsonPath("$.freeSpots").value(2))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.meetingPoint.name").value("Albufera courts"))
                .andExpect(jsonPath("$.meetingPoint.latitude").value(40.396))
                .andExpect(jsonPath("$.meetingPoint.longitude").value(-3.63))
                .andExpect(jsonPath("$.organizerName").doesNotExist())
                .andExpect(jsonPath("$.organizerId").doesNotExist())
                .andExpect(jsonPath("$.participants").doesNotExist());

        mockMvc.perform(get("/api/v1/public/plans/" + UUID.randomUUID())).andExpect(status().isNotFound());
        // Only reading is public
        mockMvc.perform(get(PlanController.PLANS + "/" + planId)).andExpect(status().isUnauthorized());
    }

    @Test
    void theSharedLinkHasAPreviewAndLeadsToThePlan() throws Exception {
        var startsAt = Instant.now().plus(Duration.ofHours(2));
        var planId = publish("Padel <script>alert(1)</script> & friends", 1, startsAt);
        var time = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Europe/Madrid")).format(startsAt);

        var spanish = mockMvc.perform(get("/share/plans/" + planId).header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, public"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(spanish)
                .contains("<html lang=\"es\">")
                .contains("<meta property=\"og:title\" content=\"Padel &lt;script&gt;alert(1)&lt;/script&gt; &amp; friends\">")
                .contains("<meta property=\"og:description\" content=\"Pádel · a las " + time
                        + " · Falta 1 · Albufera courts\">")
                .contains("<meta property=\"og:image\" content=\"http://localhost:4200/og-image.png\">")
                .contains("url=http://localhost:4200/plans/" + planId)
                .doesNotContain("<script>");

        var english = mockMvc.perform(get("/share/plans/" + planId).header(HttpHeaders.ACCEPT_LANGUAGE, "en-GB"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(english).contains("<html lang=\"en\">").contains("Padel · at " + time + " · 1 spot left");
    }

    @Test
    void aLinkToAPlanThatIsGoneLeadsToTheHomeScreen() throws Exception {
        var page = mockMvc.perform(get("/share/plans/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(page).contains("Este plan ya no está disponible").contains("url=http://localhost:4200");
    }

    @Test
    void thePreviewTellsHowManySpotsAreLeft() throws Exception {
        var planId = publish("Football", 3, Instant.now().plus(Duration.ofHours(1)));
        var page = mockMvc.perform(get("/share/plans/" + planId).header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(page).contains("3 spots left");

        mockMvc.perform(post(PlanController.PLANS + "/" + planId + "/participants")
                .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("name", "Lucía"))));
        mockMvc.perform(post(PlanController.PLANS + "/" + planId + "/participants")
                .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("name", "Diego"))));
        mockMvc.perform(post(PlanController.PLANS + "/" + planId + "/participants")
                .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("name", "Marta"))));
        var full = mockMvc.perform(get("/share/plans/" + planId))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(full).contains("Completo");
    }

    @Test
    void thePreviewLanguageFollowsTheClientAndDefaultsToSpanish() {
        assertThat(PublicPlanController.language("en-GB,en;q=0.9,es;q=0.8").getLanguage()).isEqualTo("en");
        assertThat(PublicPlanController.language(null).getLanguage()).isEqualTo("es");
        assertThat(PublicPlanController.language("").getLanguage()).isEqualTo("es");
        assertThat(PublicPlanController.language("not a language;;q=x").getLanguage()).isEqualTo("es");
    }
}

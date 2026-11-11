package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.AudienceMember;
import es.upm.miw.oneleft.plans.domain.model.PersonGender;
import es.upm.miw.oneleft.plans.domain.port.out.AudienceDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-070 with a real PostGIS: the audience of a plan, who sees it and who can join it. The users service is simulated. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AudienceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AudienceDirectory directory;

    private final double latitude = ThreadLocalRandom.current().nextDouble(-50, -45);
    private final double longitude = ThreadLocalRandom.current().nextDouble(-40, -30);
    private final String organizer = UUID.randomUUID().toString();
    private final String lucia = UUID.randomUUID().toString();
    private final String diego = UUID.randomUUID().toString();
    private final String nobody = UUID.randomUUID().toString();

    private static RequestPostProcessor user(String subject) {
        return jwt().jwt(token -> token.subject(subject).claim("name", "Test"));
    }

    private String publish(String audience, double north) throws Exception {
        var body = """
                {"activity": "YOGA", "title": "Yoga %s", "startsAt": "%s", "spots": 3, "audience": %s,
                 "meetingPoint": {"name": "Estudio", "latitude": %s, "longitude": %s}}"""
                .formatted(north, Instant.now().plus(Duration.ofHours(2)), audience, latitude + north, longitude);
        var location = mockMvc.perform(post(PlanController.PLANS).with(user(organizer))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String nearby() {
        return PlanController.PLANS + PlanController.NEARBY + "?latitude=" + latitude + "&longitude=" + longitude
                + "&radius=5000";
    }

    @Test
    void aPlanForWomenOf25To40IsSeenAndJoinedOnlyByWhoFits() throws Exception {
        when(directory.memberOf(any())).thenReturn(Optional.empty());
        var forEveryone = publish("{\"gender\": \"ALL\"}", 0.001);
        var restricted = publish("{\"gender\": \"WOMAN\", \"minAge\": 25, \"maxAge\": 40}", 0.002);
        var viewer = user(lucia);

        mockMvc.perform(get(PlanController.PLANS + "/" + restricted).with(viewer))
                .andExpect(jsonPath("$.audience.gender").value("WOMAN"))
                .andExpect(jsonPath("$.audience.minAge").value(25)).andExpect(jsonPath("$.audience.maxAge").value(40));
        // Nobody knows who the viewer is: only the plan for everyone
        mockMvc.perform(get(nearby()).with(viewer)).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plan.id").value(forEveryone));
        mockMvc.perform(post(PlanController.PLANS + "/" + restricted + "/participants").with(user(nobody)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("demographics.required"));

        when(directory.memberOf(any())).thenReturn(Optional.of(new AudienceMember(PersonGender.WOMAN, 33)));
        mockMvc.perform(get(nearby()).with(viewer)).andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(post(PlanController.PLANS + "/" + restricted + "/participants").with(viewer))
                .andExpect(status().isOk());

        when(directory.memberOf(any())).thenReturn(Optional.of(new AudienceMember(PersonGender.MAN, 33)));
        mockMvc.perform(get(nearby()).with(user(diego))).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plan.id").value(forEveryone));
        mockMvc.perform(post(PlanController.PLANS + "/" + restricted + "/participants").with(user(diego)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("plan.audienceMismatch"));

        when(directory.memberOf(any())).thenReturn(Optional.of(new AudienceMember(PersonGender.NON_BINARY, 33)));
        mockMvc.perform(get(nearby()).with(user(nobody))).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(post(PlanController.PLANS + "/" + forEveryone + "/participants").with(user(nobody)))
                .andExpect(status().isOk());
    }

    @Test
    void anInvalidAgeRangeIsRejectedWhenPublishing() throws Exception {
        for (var audience : new String[]{"{\"minAge\": 17}", "{\"minAge\": 40, \"maxAge\": 30}"}) {
            var body = """
                    {"activity": "YOGA", "title": "Yoga", "startsAt": "%s", "spots": 3, "audience": %s,
                     "meetingPoint": {"name": "Estudio", "latitude": %s, "longitude": %s}}"""
                    .formatted(Instant.now().plus(Duration.ofHours(2)), audience, latitude, longitude);
            mockMvc.perform(post(PlanController.PLANS).with(user(organizer)).contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
    }
}

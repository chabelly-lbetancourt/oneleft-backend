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
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-004 de extremo a extremo con PostGIS y RabbitMQ reales.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class NearbyPlansControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final double latitude = ThreadLocalRandom.current().nextDouble(-60, -55);
    private final double longitude = ThreadLocalRandom.current().nextDouble(-20, -10);

    private static RequestPostProcessor user(String subject) {
        return jwt().jwt(token -> token.subject(subject).claim("name", "Ana Pruebas"));
    }

    private void publish(String organizer, String activity, double northDegrees, Duration startsIn) throws Exception {
        var body = """
                {"activity": "%s", "title": "Plan de prueba", "startsAt": "%s", "spots": 2,
                 "meetingPoint": {"name": "Punto de prueba", "latitude": %s, "longitude": %s}}"""
                .formatted(activity, Instant.now().plus(startsIn), latitude + northDegrees, longitude);
        mockMvc.perform(post(PlanController.PLANS).with(user(organizer))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private String nearbyUrl(String query) {
        return PlanController.PLANS + PlanController.NEARBY + "?latitude=" + latitude + "&longitude=" + longitude
                + query;
    }

    @Test
    void listsNearbyPlansWithTheirDistanceFromTheNearest() throws Exception {
        var organizer = UUID.randomUUID().toString();
        publish(organizer, "PADEL", 0.009, Duration.ofHours(1));
        publish(organizer, "CINE", 0, Duration.ofHours(2));
        publish(organizer, "PADEL", 0.1, Duration.ofHours(1));

        mockMvc.perform(get(nearbyUrl("&radius=2000")).with(user(UUID.randomUUID().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].plan.activity").value("CINE"))
                .andExpect(jsonPath("$[0].distanceMeters").value(0))
                .andExpect(jsonPath("$[1].plan.activity").value("PADEL"))
                .andExpect(jsonPath("$[1].distanceMeters").value(1001))
                .andExpect(jsonPath("$[1].plan.freeSpots").value(2));

        mockMvc.perform(get(nearbyUrl("&radius=2000&activity=PADEL&activity=TENIS&withinHours=1"))
                        .with(user(UUID.randomUUID().toString())))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plan.activity").value("PADEL"));

        mockMvc.perform(get(nearbyUrl("")).with(user(organizer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsSearchesOutOfRange() throws Exception {
        var subject = UUID.randomUUID().toString();
        mockMvc.perform(get(nearbyUrl("&radius=100")).with(user(subject)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El radio debe estar entre 500 m y 25 km"));
        mockMvc.perform(get(nearbyUrl("&withinHours=24")).with(user(subject)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(nearbyUrl("&activity=AJEDREZ")).with(user(subject)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(PlanController.PLANS + PlanController.NEARBY).with(user(subject)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(nearbyUrl("")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamsOnlyTheNewPlansThatMatchTheSearch() throws Exception {
        var watcher = mockMvc.perform(get(PlanController.PLANS + PlanController.NEARBY_STREAM + "?latitude="
                        + latitude + "&longitude=" + longitude + "&radius=1000&activity=PADEL")
                        .with(user(UUID.randomUUID().toString())))
                .andExpect(request().asyncStarted())
                .andReturn();
        var response = watcher.getResponse();
        assertThat(response.getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(response.getContentAsString()).contains("event:ready");

        var organizer = UUID.randomUUID().toString();
        publish(organizer, "CINE", 0, Duration.ofHours(1));          // otra actividad
        publish(organizer, "PADEL", 0.05, Duration.ofHours(1));      // a 5,5 km
        publish(organizer, "PADEL", 0.005, Duration.ofHours(1));     // a 556 m: este sí

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(response.getContentAsString()).contains("event:plan-published"));
        var stream = response.getContentAsString();
        assertThat(stream.split("event:plan-published", -1)).hasSize(2);
        assertThat(stream).contains("\"activity\":\"PADEL\"").contains("\"distanceMeters\":556");
    }
}

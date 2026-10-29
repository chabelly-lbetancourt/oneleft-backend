package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.DraftUnavailableException;
import es.upm.miw.oneleft.plans.domain.model.Place;
import es.upm.miw.oneleft.plans.domain.model.PlaceSuggestion;
import es.upm.miw.oneleft.plans.domain.model.RawDraft;
import es.upm.miw.oneleft.plans.domain.port.in.SuggestPlacesUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanInterpreter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-010 through the API: the model and the place search are replaced; nothing is published. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlanDraftControllerTest {

    private static final String BODY = """
            {"text": "Pádel hoy a las 7 en la Albufera, falta uno", "latitude": 40.39, "longitude": -3.63,
             "timeZone": "UTC"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanInterpreter interpreter;

    @MockitoBean
    private SuggestPlacesUseCase places;

    private static String inTwoHours() {
        return ZonedDateTime.now(ZoneOffset.UTC).plusHours(2).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    @Test
    void theDraftIsReturnedForReviewAndNothingIsPublished() throws Exception {
        when(interpreter.interpret(any(), any(), any())).thenReturn(new RawDraft("PADEL", "Pádel, falta uno", null,
                "la Albufera", inTwoHours(), 1, "INTERMEDIATE", null));
        when(places.suggest(any())).thenReturn(List.of(new PlaceSuggestion(
                new Place("Pistas de la Albufera", "Madrid", 40.3964, -3.6297), 700)));

        mockMvc.perform(post(PlanDraftController.DRAFT).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity").value("PADEL"))
                .andExpect(jsonPath("$.title").value("Pádel, falta uno"))
                .andExpect(jsonPath("$.meetingPoint.name").value("Pistas de la Albufera"))
                .andExpect(jsonPath("$.spots").value(1))
                .andExpect(jsonPath("$.level").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.startsAt").isNotEmpty())
                .andExpect(jsonPath("$.missing").isEmpty());
    }

    @Test
    void whatCouldNotBeUnderstoodComesBackAsMissing() throws Exception {
        when(interpreter.interpret(any(), any(), any())).thenReturn(new RawDraft("PADEL", null, null, null, null, null,
                null, null));

        mockMvc.perform(post(PlanDraftController.DRAFT).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missing[0]").value("title"))
                .andExpect(jsonPath("$.missing.length()").value(4));
    }

    @Test
    void ifTheModelIsNotAvailableTheAnswerIs503WithItsCode() throws Exception {
        when(interpreter.interpret(any(), any(), any())).thenThrow(new DraftUnavailableException("no key"));

        mockMvc.perform(post(PlanDraftController.DRAFT).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("draft.unavailable"));
    }

    @Test
    void theSentenceAndTheCoordinatesAreValidated() throws Exception {
        mockMvc.perform(post(PlanDraftController.DRAFT).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"\", \"latitude\": 40.39, \"longitude\": -3.63}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(PlanDraftController.DRAFT).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + "x".repeat(301) + "\", \"latitude\": 40.39, \"longitude\": -3.63}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(PlanDraftController.DRAFT).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Pádel hoy\", \"latitude\": 140, \"longitude\": -3.63}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void itNeedsASession() throws Exception {
        mockMvc.perform(post(PlanDraftController.DRAFT).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }
}

package es.upm.miw.oneleft.users.infrastructure.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-070 in the users service with a real PostgreSQL: date of birth and gender, with the age band and the audience. */
@SpringBootTest
@AutoConfigureMockMvc
class DemographicsControllerTest {

    private static final String BASE = UserController.USERS + "/me/demographics";

    @Autowired
    private MockMvc mockMvc;

    private final RequestPostProcessor me = jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
            .claim("name", "Ana Test").claim("email", "test@oneleft.dev"));

    private static String body(LocalDate birthDate, String gender) {
        return "{\"birthDate\": \"%s\", \"gender\": \"%s\"}".formatted(birthDate, gender);
    }

    @Test
    void nothingIsSetUntilThePersonSetsItThenTheyGetTheBandAndTheAudienceOnlyHasGenderAndAge() throws Exception {
        mockMvc.perform(get(BASE).with(me)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("demographics.notSet"));
        mockMvc.perform(get(BASE + "/audience").with(me)).andExpect(status().isNotFound());

        var born = LocalDate.now().minusYears(30);
        mockMvc.perform(put(BASE).with(me).contentType(MediaType.APPLICATION_JSON).content(body(born, "WOMAN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gender").value("WOMAN"))
                .andExpect(jsonPath("$.ageBand.label").value("25–34")).andExpect(jsonPath("$.changesLeft").value(2));
        mockMvc.perform(get(BASE + "/audience").with(me)).andExpect(status().isOk())
                .andExpect(jsonPath("$.gender").value("WOMAN")).andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$.birthDate").doesNotExist());
    }

    @Test
    void aMinorOrAMissingFieldIsRejectedAndTheDateOnlyChangesAFewTimes() throws Exception {
        mockMvc.perform(put(BASE).with(me).contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalDate.now().minusYears(15), "MAN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("demographics.minor"));
        mockMvc.perform(put(BASE).with(me).contentType(MediaType.APPLICATION_JSON).content("{\"gender\": \"MAN\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(BASE).with(me).contentType(MediaType.APPLICATION_JSON)
                .content(body(LocalDate.now().minusYears(40), "MAN"))).andExpect(status().isOk());
        for (var years : new int[]{41, 42}) {
            mockMvc.perform(put(BASE).with(me).contentType(MediaType.APPLICATION_JSON)
                    .content(body(LocalDate.now().minusYears(years), "MAN"))).andExpect(status().isOk());
        }
        mockMvc.perform(put(BASE).with(me).contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalDate.now().minusYears(43), "MAN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("demographics.birthDateLocked"));
        mockMvc.perform(put(BASE).with(me).contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalDate.now().minusYears(42), "NON_BINARY")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gender").value("NON_BINARY"))
                .andExpect(jsonPath("$.changesLeft").value(0));
    }
}

package es.upm.miw.oneleft.users.infrastructure.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    private static final String BASE = UserController.USERS;

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor user(String subject, String name) {
        return jwt().jwt(token -> token.subject(subject).claim("name", name).claim("email", "test@oneleft.dev"));
    }

    @Test
    void myProfileIsCreatedOnFirstAccess() throws Exception {
        var subject = UUID.randomUUID().toString();
        mockMvc.perform(get(BASE + ProfileController.MY_PROFILE).with(user(subject, "Ana Pruebas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(subject))
                .andExpect(jsonPath("$.displayName").value("Ana Pruebas"))
                .andExpect(jsonPath("$.hobbies", hasSize(0)));
    }

    @Test
    void myProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get(BASE + ProfileController.MY_PROFILE)).andExpect(status().isUnauthorized());
    }

    @Test
    void updateStoresAnApproximateZoneAndOthersSeeNoCoordinates() throws Exception {
        var subject = UUID.randomUUID().toString();
        mockMvc.perform(put(BASE + ProfileController.MY_PROFILE).with(user(subject, "Ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName": "Anita",
                                 "zone": {"name": "Vallecas", "latitude": 40.391234, "longitude": -3.628765},
                                 "hobbies": [{"activity": "PADEL", "level": "INTERMEDIO"},
                                             {"activity": "CINE", "level": "PRINCIPIANTE"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Anita"))
                .andExpect(jsonPath("$.zone.latitude").value(40.39))
                .andExpect(jsonPath("$.zone.longitude").value(-3.63))
                .andExpect(jsonPath("$.hobbies", hasSize(2)));

        mockMvc.perform(get(BASE + "/" + subject + "/profile").with(user(UUID.randomUUID().toString(), "Otro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Anita"))
                .andExpect(jsonPath("$.zoneName").value("Vallecas"))
                .andExpect(jsonPath("$.zone").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist());
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        var request = put(BASE + ProfileController.MY_PROFILE).with(user(UUID.randomUUID().toString(), "Ana"))
                .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(request.content("""
                        {"displayName": " ", "hobbies": []}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(BASE + ProfileController.MY_PROFILE).with(user(UUID.randomUUID().toString(), "Ana"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"displayName": "Ana", "zone": {"name": "Polo", "latitude": 95, "longitude": 0}, "hobbies": []}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(BASE + ProfileController.MY_PROFILE).with(user(UUID.randomUUID().toString(), "Ana"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"displayName": "Ana", "hobbies": [{"activity": "PADEL", "level": "INTERMEDIO"},
                                                           {"activity": "PADEL", "level": "AVANZADO"}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cada actividad solo puede aparecer una vez"));
    }

    @Test
    void unknownProfileIsNotFound() throws Exception {
        mockMvc.perform(get(BASE + "/" + UUID.randomUUID() + "/profile").with(user(UUID.randomUUID().toString(), "A")))
                .andExpect(status().isNotFound());
    }

    @Test
    void catalogListsActivitiesAndLevels() throws Exception {
        mockMvc.perform(get(BASE + ProfileController.ACTIVITIES).with(user(UUID.randomUUID().toString(), "A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activities", hasSize(10)))
                .andExpect(jsonPath("$.activities[0].code").value("PADEL"))
                .andExpect(jsonPath("$.activities[0].name").value("Pádel"))
                .andExpect(jsonPath("$.levels", hasSize(3)));
    }
}

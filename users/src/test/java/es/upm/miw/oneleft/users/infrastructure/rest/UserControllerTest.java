package es.upm.miw.oneleft.users.infrastructure.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    private static final String ME = UserController.USERS + UserController.ME;
    private static final String SUBJECT = "0b6f1c2e-0000-4000-8000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get(ME)).andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheAuthenticatedUser() throws Exception {
        mockMvc.perform(get(ME).with(jwt().jwt(token -> token
                        .subject(SUBJECT)
                        .claim("name", "Admin Pruebas")
                        .claim("email", "admin@oneleft.dev")
                        .claim("realm_access", Map.of("roles", List.of("user", "admin", "offline_access"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SUBJECT))
                .andExpect(jsonPath("$.name").value("Admin Pruebas"))
                .andExpect(jsonPath("$.email").value("admin@oneleft.dev"))
                .andExpect(jsonPath("$.roles", contains("ADMIN", "USER")));
    }
}

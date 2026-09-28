package es.upm.miw.oneleft.plans.infrastructure.config;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsArePublicAndDescribeTheService() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("OneLeft · plans"))
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8080"))
                .andExpect(jsonPath("$.components.securitySchemes.keycloak.type").value("oauth2"))
                .andExpect(jsonPath("$.components.securitySchemes.keycloak.flows.authorizationCode.authorizationUrl")
                        .value("http://localhost:8180/realms/oneleft/protocol/openid-connect/auth"));
    }
}

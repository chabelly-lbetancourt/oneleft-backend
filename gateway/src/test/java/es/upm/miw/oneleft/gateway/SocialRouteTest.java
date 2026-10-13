package es.upm.miw.oneleft.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Following people (HU-047) goes through the gateway to the social service, only with a session. The social service
 * is replaced by a stub that answers 200 and records what it was asked.
 */
@SpringBootTest(properties = "oneleft.rate-limit.enabled=false")
@AutoConfigureMockMvc
class SocialRouteTest {

    private static final AtomicReference<String> REQUESTED = new AtomicReference<>();
    private static final HttpServer SOCIAL = startSocial();

    private static HttpServer startSocial() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                REQUESTED.set(exchange.getRequestMethod() + " " + exchange.getRequestURI());
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().add("Connection", "close");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void social(DynamicPropertyRegistry registry) {
        registry.add("SOCIAL_URL", () -> "http://localhost:" + SOCIAL.getAddress().getPort());
    }

    @AfterAll
    static void stopSocial() {
        SOCIAL.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void followingReachesTheSocialService() throws Exception {
        var lucia = jwt().jwt(token -> token.subject(UUID.randomUUID().toString()));
        var ana = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/social/me/following/" + ana).with(lucia)).andExpect(status().isOk());

        assertThat(REQUESTED.get()).isEqualTo("PUT /api/v1/social/me/following/" + ana);
    }

    @Test
    void withoutASessionNobodyIsFollowed() throws Exception {
        mockMvc.perform(get("/api/v1/social/me")).andExpect(status().isUnauthorized());
    }
}

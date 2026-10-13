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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The reputation (HU-009) goes through the gateway to the reputation service, only with a session. The reputation
 * service is replaced by a stub that answers 200 and records what it was asked.
 */
@SpringBootTest(properties = "oneleft.rate-limit.enabled=false")
@AutoConfigureMockMvc
class ReputationRouteTest {

    private static final AtomicReference<String> REQUESTED = new AtomicReference<>();
    private static final HttpServer REPUTATION = startReputation();

    private static HttpServer startReputation() {
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
    static void reputation(DynamicPropertyRegistry registry) {
        registry.add("REPUTATION_URL", () -> "http://localhost:" + REPUTATION.getAddress().getPort());
    }

    @AfterAll
    static void stopReputation() {
        REPUTATION.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theReputationIsAskedToTheReputationService() throws Exception {
        var lucia = jwt().jwt(token -> token.subject(UUID.randomUUID().toString()));
        mockMvc.perform(get("/api/v1/reputation/me/ledger").param("page", "1").with(lucia))
                .andExpect(status().isOk());

        assertThat(REQUESTED.get()).isEqualTo("GET /api/v1/reputation/me/ledger?page=1");
    }

    @Test
    void withoutASessionThereIsNoReputation() throws Exception {
        mockMvc.perform(get("/api/v1/reputation/me")).andExpect(status().isUnauthorized());
    }
}

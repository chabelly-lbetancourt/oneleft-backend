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
 * Place suggestions (HU-071) go through the gateway to the plans service, only with a session. The plans service is
 * replaced by a stub that answers 200 and records what it was asked.
 */
@SpringBootTest(properties = "oneleft.rate-limit.enabled=false")
@AutoConfigureMockMvc
class PlacesRouteTest {

    private static final AtomicReference<String> REQUESTED = new AtomicReference<>();
    private static final HttpServer PLANS = startPlans();

    private static HttpServer startPlans() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                REQUESTED.set(exchange.getRequestURI().toString());
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
    static void plans(DynamicPropertyRegistry registry) {
        registry.add("PLANS_URL", () -> "http://localhost:" + PLANS.getAddress().getPort());
    }

    @AfterAll
    static void stopPlans() {
        PLANS.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void suggestionsReachThePlansServiceWithTheSearch() throws Exception {
        var organizer = jwt().jwt(token -> token.subject(UUID.randomUUID().toString()));
        mockMvc.perform(get("/api/v1/places").param("query", "padel").param("latitude", "40.39")
                        .param("longitude", "-3.63").with(organizer))
                .andExpect(status().isOk());

        assertThat(REQUESTED.get()).isEqualTo("/api/v1/places?query=padel&latitude=40.39&longitude=-3.63");
    }

    @Test
    void withoutASessionThereAreNoSuggestions() throws Exception {
        mockMvc.perform(get("/api/v1/places").param("query", "padel")).andExpect(status().isUnauthorized());
    }
}

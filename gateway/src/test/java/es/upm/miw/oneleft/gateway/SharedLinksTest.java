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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared plan links (HU-024) go through the gateway without a token; the rest of the API still needs one.
 * The plans service is replaced by a stub that answers 200 to everything and records the client that called it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SharedLinksTest {

    private static final AtomicReference<String> USER_AGENT = new AtomicReference<>();
    private static final HttpServer PLANS = startPlans();

    private static HttpServer startPlans() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                USER_AGENT.set(exchange.getRequestHeaders().getFirst("User-Agent"));
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
    void sharedLinksNeedNoSession() throws Exception {
        var planId = UUID.randomUUID();
        mockMvc.perform(get("/share/plans/" + planId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/plans/" + planId)).andExpect(status().isOk());
    }

    @Test
    void theProxyUsesApacheHttpClientInsteadOfTheJdkClient() throws Exception {
        // The JDK client failed when a service answered before it had sent the (empty) request body (#90)
        mockMvc.perform(get("/share/plans/" + UUID.randomUUID())).andExpect(status().isOk());
        assertThat(USER_AGENT.get()).startsWith("Apache-HttpClient/");
    }

    @Test
    void onlyReadingIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/public/plans/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/plans/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }
}

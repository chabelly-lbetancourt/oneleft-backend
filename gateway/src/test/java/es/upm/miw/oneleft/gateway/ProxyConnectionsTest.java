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
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Real-time streams keep their connection to a service open while the app is open (#93). The gateway must forward
 * many requests to the same service at the same time: the plans stub below only answers once all of them have
 * reached it, as if they were open streams.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProxyConnectionsTest {

    /** Twice the connections per service that Apache HttpClient allows by default. */
    private static final int OPEN_AT_ONCE = 10;
    private static final CountDownLatch ALL_ARRIVED = new CountDownLatch(OPEN_AT_ONCE);
    private static final AtomicBoolean GAVE_UP_WAITING = new AtomicBoolean();
    private static final HttpServer PLANS = startPlans();

    private static HttpServer startPlans() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                ALL_ARRIVED.countDown();
                try {
                    if (!ALL_ARRIVED.await(5, TimeUnit.SECONDS)) {
                        GAVE_UP_WAITING.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
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
    void manyRequestsToTheSameServiceAreForwardedAtTheSameTime() throws Exception {
        try (var people = Executors.newFixedThreadPool(OPEN_AT_ONCE)) {
            var requests = new ArrayList<Future<?>>();
            for (int i = 0; i < OPEN_AT_ONCE; i++) {
                requests.add(people.submit(() ->
                        mockMvc.perform(get("/share/plans/" + UUID.randomUUID())).andReturn()));
            }
            for (var request : requests) {
                request.get(30, TimeUnit.SECONDS);
            }
        }

        // With a pool of 5, only 5 requests reach the stub, which gives up waiting for the rest
        assertThat(GAVE_UP_WAITING).isFalse();
    }
}

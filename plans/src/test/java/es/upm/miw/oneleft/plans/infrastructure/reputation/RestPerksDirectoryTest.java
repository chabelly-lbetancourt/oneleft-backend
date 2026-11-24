package es.upm.miw.oneleft.plans.infrastructure.reputation;

import com.sun.net.httpserver.HttpServer;
import es.upm.miw.oneleft.plans.domain.model.ReputationPerks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-063: the perks are asked to the reputation service with the token of the person, and when they cannot be known
 * the person has what everyone has. The reputation service is a local HTTP server.
 */
class RestPerksDirectoryTest {

    private static final UUID LUCIA = UUID.randomUUID();

    private HttpServer reputation;
    private int status = 200;
    private String answer = "{}";
    private final List<String> tokens = new ArrayList<>();
    private RestPerksDirectory directory;

    @BeforeEach
    void start() throws IOException {
        reputation = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        reputation.createContext("/api/v1/reputation/me/perks", exchange -> {
            tokens.add(exchange.getRequestHeaders().getFirst("Authorization"));
            var body = answer.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        reputation.start();
        directory = new RestPerksDirectory(new ReputationConfig()
                .reputationService("http://localhost:" + reputation.getAddress().getPort()));
    }

    @AfterEach
    void stop() {
        reputation.stop(0);
        SecurityContextHolder.clearContext();
    }

    private static void signedIn() {
        var jwt = Jwt.withTokenValue("token-of-lucia").header("alg", "none").subject(LUCIA.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void thePerksOfThePersonAreAskedWithTheirOwnToken() {
        signedIn();
        answer = """
                {"level": "NEIGHBOURHOOD_REFERENCE", "waitlistPriority": 2, "noticeWeight": 1.25,
                 "maxActivePlans": 8, "maxSavedAlerts": 8, "trustedOrganizer": true, "somethingNew": 1}""";

        assertThat(directory.perksOf(LUCIA)).isEqualTo(new ReputationPerks(2, 8));
        assertThat(tokens).containsExactly("Bearer token-of-lucia");
    }

    @Test
    void withoutATokenNothingIsAskedAndThePersonHasWhatEveryoneHas() {
        assertThat(directory.perksOf(LUCIA)).isEqualTo(ReputationPerks.BASE);
        assertThat(tokens).isEmpty();
    }

    @Test
    void aReputationServiceThatFailsOrIsDownTakesNothingAwayFromAnyone() {
        signedIn();
        status = 500;
        assertThat(directory.perksOf(LUCIA)).isEqualTo(ReputationPerks.BASE);

        status = 401;
        assertThat(directory.perksOf(LUCIA)).isEqualTo(ReputationPerks.BASE);

        reputation.stop(0);
        assertThat(directory.perksOf(LUCIA)).isEqualTo(ReputationPerks.BASE);
    }
}

package es.upm.miw.oneleft.users.infrastructure.services;

import es.upm.miw.oneleft.users.domain.model.AccountException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** HU-045: each service is asked for its part with the token of the person, and no part missing means no file. */
class ServicesRestDataTest {

    private final Map<String, MockRestServiceServer> servers = new LinkedHashMap<>();
    private final Map<String, RestClient> clients = new LinkedHashMap<>();
    private final UUID userId = UUID.randomUUID();

    ServicesRestDataTest() {
        for (var name : new String[] {"plans", "social", "reputation", "notifications", "chat"}) {
            var builder = RestClient.builder().baseUrl("http://" + name);
            servers.put(name, MockRestServiceServer.bindTo(builder).build());
            clients.put(name, builder.build());
        }
    }

    @AfterEach
    void clean() {
        SecurityContextHolder.clearContext();
    }

    private void signedInWith(String token) {
        var jwt = Jwt.withTokenValue(token).header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private void everyServiceAnswers() {
        servers.forEach((name, server) -> server
                .expect(requestTo("http://" + name + ServicesPaths.of(name)))
                .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "Bearer my-token"))
                .andRespond(withSuccess("{\"" + name + "Rows\": []}", MediaType.APPLICATION_JSON)));
    }

    @Test
    void gathersThePartOfEveryServiceWithTheTokenOfThePerson() {
        signedInWith("my-token");
        everyServiceAnswers();

        var data = new ServicesRestData(clients).of(userId);

        assertThat(data).containsOnlyKeys("plans", "social", "reputation", "notifications", "chat");
        assertThat(data.get("chat")).isEqualTo(Map.of("chatRows", java.util.List.of()));
        servers.values().forEach(MockRestServiceServer::verify);
    }

    @Test
    void aServiceThatDoesNotAnswerMeansThereIsNoFile() {
        signedInWith("my-token");
        servers.get("plans").expect(requestTo("http://plans" + ServicesPaths.of("plans")))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> new ServicesRestData(clients).of(userId)).isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("reason", AccountException.Reason.DATA_UNAVAILABLE)
                .hasMessageContaining("plans");
    }

    @Test
    void withoutAKeycloakTokenNobodyIsAsked() {
        assertThatThrownBy(() -> new ServicesRestData(clients).of(userId)).isInstanceOf(IllegalStateException.class);
    }

    /** The paths the file is gathered from: the same ones the services publish as {@code me/data} */
    private static final class ServicesPaths {
        static String of(String service) {
            return "/api/v1/" + (service.equals("chat") ? "chats" : service) + "/me/data";
        }
    }
}

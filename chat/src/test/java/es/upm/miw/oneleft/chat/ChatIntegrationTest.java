package es.upm.miw.oneleft.chat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import es.upm.miw.oneleft.chat.domain.port.in.ChatUseCase;
import es.upm.miw.oneleft.chat.infrastructure.messaging.RabbitConfig;
import es.upm.miw.oneleft.chat.infrastructure.rest.ChatController;
import es.upm.miw.oneleft.chat.infrastructure.websocket.WebSocketConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU-008 end to end inside the service: real WebSocket connections, PostgreSQL and RabbitMQ in containers, and the
 * plans service simulated by WireMock. Tokens are decoded by a test decoder: {@code test.<user id>.<name>}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ChatIntegrationTest.Tokens.class})
class ChatIntegrationTest {

    @RegisterExtension
    static final WireMockExtension PLANS = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();

    @DynamicPropertySource
    static void plansService(DynamicPropertyRegistry registry) {
        registry.add("oneleft.plans.url", PLANS::baseUrl);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper json;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ChatUseCase chat;

    private UUID planId;

    /** A Bearer token only has letters, digits and a few symbols: the name travels in Base64URL. */
    private static String token(UUID userId, String name) {
        return "test." + userId + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    /** The plan as the plans service returns it: Ana organizes and Lucía takes part. */
    private void planEndingAt(Instant endsAt, String status) {
        PLANS.stubFor(get(urlPathEqualTo("/api/v1/plans/" + planId)).willReturn(okJson("""
                {"id": "%s", "organizerId": "%s", "participants": [{"userId": "%s", "name": "Lucía"}],
                 "status": "%s", "endsAt": "%s", "title": "Padel"}""".formatted(planId, ANA, LUCIA, status, endsAt))));
    }

    private Connection connect(UUID userId, String name) throws Exception {
        var headers = new WebSocketHttpHeaders();
        headers.setBearerAuth(token(userId, name));
        var connection = new Connection();
        var uri = URI.create("ws://localhost:" + port + WebSocketConfig.PATH + "?planId=" + planId);
        new StandardWebSocketClient().execute(connection, headers, uri).get(WAIT.toSeconds(), TimeUnit.SECONDS);
        return connection;
    }

    private RestClient api(UUID userId, String name) {
        return RestClient.builder().baseUrl("http://localhost:" + port)
                .defaultHeaders(headers -> headers.setBearerAuth(token(userId, name))).build();
    }

    private JsonNode history(UUID userId, String name) {
        return api(userId, name).get().uri(ChatController.CHATS + "/" + planId + "/messages").retrieve()
                .body(JsonNode.class);
    }

    private HttpStatusCode historyStatus(RestClient client) {
        return client.get().uri(ChatController.CHATS + "/" + planId + "/messages")
                .exchange((request, response) -> response.getStatusCode());
    }

    private void send(Connection connection, String text) throws Exception {
        connection.session.sendMessage(new TextMessage(json.writeValueAsString(Map.of("text", text))));
    }

    @BeforeEach
    void plan() {
        PLANS.resetAll();
        planId = UUID.randomUUID();
    }

    /** The test leaves no message behind. */
    @AfterEach
    void deleteMessages() {
        jdbc.update("delete from chat_message");
    }

    @Test
    void thePeopleOfThePlanTalkInRealTimeAndTheHistoryKeepsIt() throws Exception {
        planEndingAt(Instant.now().plus(Duration.ofHours(2)), "OPEN");
        var ana = connect(ANA, "Ana");
        var lucia = connect(LUCIA, "Lucía");
        assertThat(ana.next("ready").path("open").asBoolean()).isTrue();
        assertThat(lucia.next("ready").path("open").asBoolean()).isTrue();

        send(lucia, "  I'll bring the balls ");

        for (var connection : new Connection[]{ana, lucia}) {
            var message = connection.next("message").path("message");
            assertThat(message.path("text").asString()).isEqualTo("I'll bring the balls");
            assertThat(message.path("authorName").asString()).isEqualTo("Lucía");
            assertThat(message.path("authorId").asString()).isEqualTo(LUCIA.toString());
        }
        var history = history(ANA, "Ana");
        assertThat(history.path("open").asBoolean()).isTrue();
        assertThat(history.path("messages")).hasSize(1);
        assertThat(history.path("messages").get(0).path("text").asString()).isEqualTo("I'll bring the balls");
        // The plans service was asked with the token of whoever opened the chat
        PLANS.verify(getRequestedFor(urlPathEqualTo("/api/v1/plans/" + planId))
                .withHeader("Authorization", equalTo("Bearer " + token(LUCIA, "Lucía"))));
        ana.close();
        lucia.close();
    }

    @Test
    void onlyThePeopleOfThePlanConnectAndReadIt() {
        planEndingAt(Instant.now().plus(Duration.ofHours(2)), "OPEN");

        assertThatThrownBy(() -> connect(DIEGO, "Diego")).isInstanceOf(ExecutionException.class)
                .hasMessageContaining("403");
        assertThat(historyStatus(api(DIEGO, "Diego")).value()).isEqualTo(403);
        assertThat(historyStatus(RestClient.create("http://localhost:" + port)).value()).isEqualTo(401);

        PLANS.stubFor(get(urlPathEqualTo("/api/v1/plans/" + planId)).willReturn(aResponse().withStatus(404)));
        assertThat(historyStatus(api(ANA, "Ana")).value()).isEqualTo(404);
        PLANS.stubFor(get(urlPathEqualTo("/api/v1/plans/" + planId)).willReturn(aResponse().withStatus(500)));
        assertThat(historyStatus(api(ANA, "Ana")).value()).isEqualTo(503);
        assertThatThrownBy(() -> connect(ANA, "Ana")).hasMessageContaining("503");
    }

    @Test
    void theChatOfAPlanThatIsOverIsReadOnly() throws Exception {
        planEndingAt(Instant.now().minusSeconds(60), "FINISHED");
        var ana = connect(ANA, "Ana");
        assertThat(ana.next("ready").path("open").asBoolean()).isFalse();

        send(ana, "Anyone still there?");

        assertThat(ana.next("error").path("code").asString()).isEqualTo("chat.closed");
        assertThat(history(ANA, "Ana").path("open").asBoolean()).isFalse();
        ana.close();
    }

    @Test
    void emptyMessagesAndFloodsComeBackAsErrors() throws Exception {
        planEndingAt(Instant.now().plus(Duration.ofHours(2)), "OPEN");
        var ana = connect(ANA, "Ana");
        ana.next("ready");

        send(ana, "   ");
        assertThat(ana.next("error").path("code").asString()).isEqualTo("chat.text");
        ana.session.sendMessage(new TextMessage("not json"));
        assertThat(ana.next("error").path("code").asString()).isEqualTo("chat.text");

        // Two messages were already counted: eight more are accepted, the next one is too fast
        for (var i = 0; i < 9; i++) {
            send(ana, "Message " + i);
        }
        assertThat(ana.next("error").path("code").asString()).isEqualTo("chat.tooFast");
        ana.close();
    }

    @Test
    void whoeverLeavesIsDisconnectedAndACancelledPlanClosesTheChat() throws Exception {
        planEndingAt(Instant.now().plus(Duration.ofHours(2)), "OPEN");
        var ana = connect(ANA, "Ana");
        var lucia = connect(LUCIA, "Lucía");
        ana.next("ready");
        lucia.next("ready");

        rabbit.convertAndSend(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_LEFT,
                Map.of("planId", planId, "participantId", LUCIA, "participantName", "Lucía"));
        assertThat(lucia.closed.get(WAIT.toSeconds(), TimeUnit.SECONDS).getCode()).isEqualTo(4403);
        assertThat(ana.session.isOpen()).isTrue();

        rabbit.convertAndSend(RabbitConfig.PLANS_EXCHANGE, RabbitConfig.PLAN_CANCELLED,
                Map.of("planId", planId, "reason", "MINIMUM_NOT_REACHED"));
        assertThat(ana.closed.get(WAIT.toSeconds(), TimeUnit.SECONDS).getCode()).isEqualTo(4409);
    }

    @Test
    void theMessagesAreDeletedADayAfterThePlanEnds() {
        var now = Instant.now();
        var insert = "insert into chat_message (id, plan_id, author_id, author_name, text, sent_at, plan_ends_at) "
                + "values (?, ?, ?, 'Ana', ?, ?, ?)";
        jdbc.update(insert, UUID.randomUUID(), planId, ANA, "Old", Timestamp.from(now.minus(Duration.ofHours(28))),
                Timestamp.from(now.minus(Duration.ofHours(25))));
        jdbc.update(insert, UUID.randomUUID(), planId, ANA, "Recent", Timestamp.from(now.minus(Duration.ofHours(26))),
                Timestamp.from(now.minus(Duration.ofHours(23))));

        assertThat(chat.purge()).isEqualTo(1);

        assertThat(jdbc.queryForList("select text from chat_message where plan_id = ?", String.class, planId))
                .containsExactly("Recent");
    }

    /** Test tokens instead of Keycloak: {@code test.<user id>.<name in Base64URL>}. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Tokens {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                var parts = token.split("\\.", 3);
                var name = new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8);
                return Jwt.withTokenValue(token).header("alg", "none").subject(parts[1]).claim("name", name)
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            };
        }
    }

    /** A WebSocket client that keeps what it receives and how it was closed. */
    private final class Connection extends TextWebSocketHandler {

        private final BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
        private final CompletableFuture<CloseStatus> closed = new CompletableFuture<>();
        private WebSocketSession session;

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            this.session = session;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            received.add(json.readTree(message.getPayload()));
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            closed.complete(status);
        }

        /** The next payload of that type, skipping the others. */
        JsonNode next(String type) throws InterruptedException {
            var deadline = Instant.now().plus(WAIT);
            while (Instant.now().isBefore(deadline)) {
                var payload = received.poll(200, TimeUnit.MILLISECONDS);
                if (payload != null && type.equals(payload.path("type").asString())) {
                    return payload;
                }
            }
            throw new AssertionError("No " + type + " received");
        }

        void close() throws Exception {
            session.close();
        }
    }
}

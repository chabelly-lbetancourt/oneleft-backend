package es.upm.miw.oneleft.gateway.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-008: the relay between the WebSocket of the app and the one of the chat service. */
class ChatRelayHandlerTest {

    private final WebSocketClient client = mock(WebSocketClient.class);
    private final ChatRelayHandler relay = new ChatRelayHandler(client, "http://chat:8084");
    private final WebSocketSession app = mock(WebSocketSession.class);
    private final WebSocketSession chat = mock(WebSocketSession.class);
    private final CompletableFuture<WebSocketSession> connection = new CompletableFuture<>();
    private final Map<String, Object> attributes = new HashMap<>();

    @BeforeEach
    void appConnects() {
        attributes.put(ChatRelayHandshake.TOKEN, "the.token");
        when(app.getAttributes()).thenReturn(attributes);
        when(app.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/chat?planId=plan-1"));
        when(app.getId()).thenReturn("app");
        when(app.isOpen()).thenReturn(true);
        when(chat.isOpen()).thenReturn(true);
        when(client.execute(any(WebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(connection);
    }

    private WebSocketHandler connectToChat() {
        relay.afterConnectionEstablished(app);
        var handler = ArgumentCaptor.forClass(WebSocketHandler.class);
        var headers = ArgumentCaptor.forClass(WebSocketHttpHeaders.class);
        var target = ArgumentCaptor.forClass(URI.class);
        verify(client).execute(handler.capture(), headers.capture(), target.capture());
        assertThat(headers.getValue().getFirst("Authorization")).isEqualTo("Bearer the.token");
        assertThat(target.getValue()).hasToString("ws://chat:8084/ws/chat?planId=plan-1");
        return handler.getValue();
    }

    @Test
    void itAnswersWithTheSubprotocolNeverWithTheToken() {
        assertThat(relay.getSubProtocols()).containsExactly("oneleft.bearer");
    }

    @Test
    void messagesGoBothWaysWithTheTokenOfThePerson() throws Exception {
        var fromChat = connectToChat();
        connection.complete(chat);

        relay.handleMessage(app, new TextMessage("{\"text\":\"Hi\"}"));
        verify(chat).sendMessage(new TextMessage("{\"text\":\"Hi\"}"));

        fromChat.handleMessage(chat, new TextMessage("{\"type\":\"message\"}"));
        verify(app).sendMessage(new TextMessage("{\"type\":\"message\"}"));
    }

    @Test
    void whoeverClosesFirstClosesTheOtherWithItsCode() throws Exception {
        var fromChat = connectToChat();
        connection.complete(chat);

        fromChat.afterConnectionClosed(chat, new CloseStatus(4403, "chat.notMember"));
        verify(app).close(new CloseStatus(4403, "chat.notMember"));

        relay.afterConnectionClosed(app, CloseStatus.NORMAL);
        verify(chat).close(CloseStatus.NORMAL);
    }

    @Test
    void aRejectedHandshakeReachesTheAppAsACloseCode() throws Exception {
        connectToChat();

        connection.completeExceptionally(new IllegalStateException(
                "The HTTP response from the server [403] did not permit the HTTP upgrade to WebSocket"));

        verify(app).close(new CloseStatus(4403, "chat.notMember"));
    }

    @Test
    void eachRejectionHasItsCode() {
        assertThat(ChatRelayHandler.closeStatusOf(new RuntimeException("x", new RuntimeException("code [404]"))))
                .isEqualTo(new CloseStatus(4404, "plan.notFound"));
        assertThat(ChatRelayHandler.closeStatusOf(new RuntimeException("[409]")))
                .isEqualTo(new CloseStatus(4409, "chat.closed"));
        assertThat(ChatRelayHandler.closeStatusOf(new RuntimeException("[503]")).getCode()).isEqualTo(4503);
        assertThat(ChatRelayHandler.closeStatusOf(new RuntimeException("Connection refused")).getCode())
                .isEqualTo(4503);
        assertThat(ChatRelayHandler.closeStatusOf(new RuntimeException((String) null)).getCode()).isEqualTo(4503);
    }
}

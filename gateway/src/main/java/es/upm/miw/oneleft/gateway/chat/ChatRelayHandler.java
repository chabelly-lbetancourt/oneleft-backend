package es.upm.miw.oneleft.gateway.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * For each connection of the app, a connection to the chat service with the same token; text messages go from one to
 * the other, and whoever closes first closes the other, with its code (for example 4403 when the person leaves the
 * plan). If the chat service rejects the handshake, the app gets the reason as a close code.
 */
@Component
public class ChatRelayHandler extends TextWebSocketHandler implements SubProtocolCapable {

    static final String CHAT_SESSION = "oneleft.chatSession";
    /** Close codes for the HTTP statuses of a rejected handshake: 4000 + status, as the chat service does. */
    private static final Pattern HTTP_STATUS = Pattern.compile("\\[(\\d{3})]");
    private static final CloseStatus UNAVAILABLE = new CloseStatus(4503, "chat.unavailable");
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 64 * 1024;
    private static final Logger log = LoggerFactory.getLogger(ChatRelayHandler.class);

    private final WebSocketClient client;
    private final URI chatService;

    @Autowired
    public ChatRelayHandler(@Value("${oneleft.chat.url:http://localhost:8084}") String chatUrl) {
        this(new StandardWebSocketClient(), chatUrl);
    }

    ChatRelayHandler(WebSocketClient client, String chatUrl) {
        this.client = client;
        this.chatService = URI.create(chatUrl.replaceFirst("^http", "ws") + ChatRelayConfig.PATH);
    }

    /** The close code of a rejected handshake, from the status in the message of the WebSocket client. */
    static CloseStatus closeStatusOf(Throwable error) {
        var cause = error;
        while (cause != null) {
            var status = cause.getMessage() == null ? null : HTTP_STATUS.matcher(cause.getMessage());
            if (status != null && status.find()) {
                var code = Integer.parseInt(status.group(1));
                return switch (code) {
                    case 403 -> new CloseStatus(4403, "chat.notMember");
                    case 404 -> new CloseStatus(4404, "plan.notFound");
                    case 409 -> new CloseStatus(4409, "chat.closed");
                    default -> UNAVAILABLE;
                };
            }
            cause = cause.getCause();
        }
        return UNAVAILABLE;
    }

    private static void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException error) {
            log.debug("Chat session {} already closed: {}", session.getId(), error.toString());
        }
    }

    /** Answers with {@code oneleft.bearer}, never with the token that follows it. */
    @Override
    public List<String> getSubProtocols() {
        return List.of(ChatBearerTokenResolver.PROTOCOL);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession app) {
        var appSafe = new ConcurrentWebSocketSessionDecorator(app, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        var planId = UriComponentsBuilder.fromUri(app.getUri()).build().getQueryParams().getFirst("planId");
        var target = UriComponentsBuilder.fromUri(chatService).queryParam("planId", planId).build().toUri();
        var headers = new WebSocketHttpHeaders();
        headers.setBearerAuth((String) app.getAttributes().get(ChatRelayHandshake.TOKEN));
        client.execute(new FromChat(appSafe), headers, target).whenComplete((chat, error) -> {
            if (error != null) {
                close(appSafe, closeStatusOf(error));
            } else if (!app.isOpen()) {
                close(chat, CloseStatus.NORMAL);
            } else {
                app.getAttributes().put(CHAT_SESSION, chat);
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession app, TextMessage message) throws IOException {
        var chat = (WebSocketSession) app.getAttributes().get(CHAT_SESSION);
        if (chat != null && chat.isOpen()) {
            chat.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession app, CloseStatus status) {
        var chat = (WebSocketSession) app.getAttributes().get(CHAT_SESSION);
        if (chat != null) {
            close(chat, CloseStatus.NORMAL);
        }
    }

    /** The messages of the chat service, to the app. */
    private static final class FromChat extends TextWebSocketHandler {

        private final WebSocketSession app;

        private FromChat(WebSocketSession app) {
            this.app = app;
        }

        @Override
        protected void handleTextMessage(WebSocketSession chat, TextMessage message) throws IOException {
            app.sendMessage(message);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession chat, CloseStatus status) {
            close(app, status);
        }
    }
}

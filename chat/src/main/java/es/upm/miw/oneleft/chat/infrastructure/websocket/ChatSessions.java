package es.upm.miw.oneleft.chat.infrastructure.websocket;

import es.upm.miw.oneleft.chat.domain.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * The chats open in this replica, by plan. Sessions are wrapped so that messages from several threads (the person
 * writing and the RabbitMQ listener) are sent one after the other.
 */
@Component
public class ChatSessions {

    /** Closed because the person is no longer in the plan. */
    public static final CloseStatus NOT_MEMBER = new CloseStatus(4403, "chat.notMember");
    /** Closed because the plan was cancelled: the chat is read-only. */
    public static final CloseStatus CLOSED = new CloseStatus(4409, "chat.closed");

    private static final Logger log = LoggerFactory.getLogger(ChatSessions.class);
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 64 * 1024;

    private final Map<UUID, Map<String, WebSocketSession>> byPlan = new ConcurrentHashMap<>();
    private final JsonMapper json;

    public ChatSessions(JsonMapper json) {
        this.json = json;
    }

    WebSocketSession add(WebSocketSession session) {
        var concurrent = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        byPlan.computeIfAbsent(ChatAttributes.planId(session), plan -> new ConcurrentHashMap<>())
                .put(session.getId(), concurrent);
        return concurrent;
    }

    void remove(WebSocketSession session) {
        byPlan.computeIfPresent(ChatAttributes.planId(session), (plan, sessions) -> {
            sessions.remove(session.getId());
            return sessions.isEmpty() ? null : sessions;
        });
    }

    /** Number of people connected to the chat of a plan in this replica. */
    public int connected(UUID planId) {
        return byPlan.getOrDefault(planId, Map.of()).size();
    }

    /** A new message, to everyone connected to its chat here. */
    public void deliver(ChatMessage message) {
        var payload = ChatPayloads.message(message);
        sessionsOf(message.planId(), session -> true).forEach(session -> send(session, payload));
    }

    /** {@code userId} left the plan: their connections to its chat are closed. */
    public void leave(UUID planId, UUID userId) {
        sessionsOf(planId, session -> userId.equals(ChatAttributes.userId(session)))
                .forEach(session -> close(session, NOT_MEMBER));
    }

    /** The plan was cancelled: everyone's connection is closed and the chat stays read-only. */
    public void close(UUID planId) {
        sessionsOf(planId, session -> true).forEach(session -> close(session, CLOSED));
    }

    void send(WebSocketSession session, Object payload) {
        try {
            session.sendMessage(new TextMessage(json.writeValueAsString(payload)));
        } catch (IOException | IllegalStateException error) {
            log.debug("Chat message not sent to session {}: {}", session.getId(), error.toString());
        }
    }

    private Set<WebSocketSession> sessionsOf(UUID planId, Predicate<WebSocketSession> filter) {
        return Set.copyOf(byPlan.getOrDefault(planId, Map.of()).values().stream().filter(filter).toList());
    }

    private static void close(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException error) {
            log.debug("Chat session {} already closed: {}", session.getId(), error.toString());
        }
    }
}

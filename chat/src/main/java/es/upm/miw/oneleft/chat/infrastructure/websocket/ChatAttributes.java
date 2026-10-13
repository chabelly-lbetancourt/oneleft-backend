package es.upm.miw.oneleft.chat.infrastructure.websocket;

import es.upm.miw.oneleft.chat.domain.model.PlanChat;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;

/** What the handshake learnt about a connection: the chat it opened, who opened it and with which name. */
final class ChatAttributes {

    static final String CHAT = "oneleft.chat";
    static final String USER_ID = "oneleft.userId";
    static final String NAME = "oneleft.name";
    static final String RATE = "oneleft.rate";
    /** The same connection, safe to send to from several threads. */
    static final String CONNECTED = "oneleft.connected";

    private ChatAttributes() {
    }

    static void put(Map<String, Object> attributes, PlanChat chat, UUID userId, String name) {
        attributes.put(CHAT, chat);
        attributes.put(USER_ID, userId);
        attributes.put(NAME, name);
    }

    static PlanChat chat(WebSocketSession session) {
        return (PlanChat) session.getAttributes().get(CHAT);
    }

    static UUID planId(WebSocketSession session) {
        return chat(session).planId();
    }

    static UUID userId(WebSocketSession session) {
        return (UUID) session.getAttributes().get(USER_ID);
    }

    static String name(WebSocketSession session) {
        return (String) session.getAttributes().get(NAME);
    }

    static WebSocketSession connected(WebSocketSession session) {
        return (WebSocketSession) session.getAttributes().getOrDefault(CONNECTED, session);
    }
}

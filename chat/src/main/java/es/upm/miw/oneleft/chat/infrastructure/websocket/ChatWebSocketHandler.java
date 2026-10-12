package es.upm.miw.oneleft.chat.infrastructure.websocket;

import es.upm.miw.oneleft.chat.domain.model.ChatException;
import es.upm.miw.oneleft.chat.domain.model.ValidationException;
import es.upm.miw.oneleft.chat.domain.port.in.ChatUseCase;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

/**
 * A connection to the chat of a plan (HU-008). On connecting it gets whether the chat can be written in; each
 * {@code {"text": "..."}} it sends is saved and reaches everyone in the chat, or comes back as an error with a code.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    static final String TOO_FAST = "chat.tooFast";

    private final ChatUseCase chat;
    private final ChatSessions sessions;
    private final JsonMapper json;
    private final Clock clock;

    public ChatWebSocketHandler(ChatUseCase chat, ChatSessions sessions, JsonMapper json, Clock clock) {
        this.chat = chat;
        this.sessions = sessions;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.getAttributes().put(ChatAttributes.RATE, new MessageRate());
        var connected = sessions.add(session);
        session.getAttributes().put(ChatAttributes.CONNECTED, connected);
        var planChat = ChatAttributes.chat(session);
        sessions.send(connected, ChatPayloads.ready(planChat.isOpen(clock.instant()), planChat.endsAt()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        var connected = ChatAttributes.connected(session);
        var rate = (MessageRate) session.getAttributes().get(ChatAttributes.RATE);
        if (!rate.tryAcquire(clock.instant())) {
            sessions.send(connected, ChatPayloads.problem(TOO_FAST));
            return;
        }
        try {
            var incoming = json.readValue(message.getPayload(), Incoming.class);
            chat.send(ChatAttributes.chat(session), ChatAttributes.userId(session), ChatAttributes.name(session),
                    incoming.text());
        } catch (ValidationException error) {
            sessions.send(connected, ChatPayloads.problem(error.code()));
        } catch (ChatException error) {
            sessions.send(connected, ChatPayloads.problem(error.code()));
        } catch (JacksonException error) {
            sessions.send(connected, ChatPayloads.problem("chat.text"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /** What the client sends: the text of a message. */
    record Incoming(String text) {
    }
}

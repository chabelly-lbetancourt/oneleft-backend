package es.upm.miw.oneleft.chat.infrastructure.websocket;

import es.upm.miw.oneleft.chat.domain.model.ChatMessage;

import java.time.Instant;
import java.util.UUID;

/** What the chat sends through the WebSocket, as JSON with a {@code type}. */
final class ChatPayloads {

    private ChatPayloads() {
    }

    /** Right after connecting: whether the chat can be written in, and until when. */
    record Ready(String type, boolean open, Instant endsAt) {
    }

    record Message(String type, MessageDto message) {
    }

    record MessageDto(UUID id, UUID planId, UUID authorId, String authorName, String text, Instant sentAt) {
    }

    /** A message that was not sent, with the code of the reason (for example {@code chat.closed}). */
    record Problem(String type, String code) {
    }

    static Ready ready(boolean open, Instant endsAt) {
        return new Ready("ready", open, endsAt);
    }

    static Message message(ChatMessage message) {
        return new Message("message", new MessageDto(message.id(), message.planId(), message.authorId(),
                message.authorName(), message.text(), message.sentAt()));
    }

    static Problem problem(String code) {
        return new Problem("error", code);
    }
}

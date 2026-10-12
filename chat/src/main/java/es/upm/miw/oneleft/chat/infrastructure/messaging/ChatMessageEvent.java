package es.upm.miw.oneleft.chat.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.upm.miw.oneleft.chat.domain.model.ChatMessage;

import java.time.Instant;
import java.util.UUID;

/** A new message of a chat, as it travels between the replicas. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessageEvent(UUID id, UUID planId, UUID authorId, String authorName, String text, Instant sentAt,
                               Instant planEndsAt) {

    static ChatMessageEvent of(ChatMessage message) {
        return new ChatMessageEvent(message.id(), message.planId(), message.authorId(), message.authorName(),
                message.text(), message.sentAt(), message.planEndsAt());
    }

    ChatMessage toDomain() {
        return new ChatMessage(id, planId, authorId, authorName, text, sentAt, planEndsAt);
    }
}

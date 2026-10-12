package es.upm.miw.oneleft.chat.infrastructure.persistence;

import es.upm.miw.oneleft.chat.domain.model.ChatMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_message")
class ChatMessageEntity {

    @Id
    private UUID id;
    @Column(name = "plan_id")
    private UUID planId;
    @Column(name = "author_id")
    private UUID authorId;
    @Column(name = "author_name")
    private String authorName;
    private String text;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "plan_ends_at")
    private Instant planEndsAt;

    protected ChatMessageEntity() {
    }

    ChatMessageEntity(ChatMessage message) {
        this.id = message.id();
        this.planId = message.planId();
        this.authorId = message.authorId();
        this.authorName = message.authorName();
        this.text = message.text();
        this.sentAt = message.sentAt();
        this.planEndsAt = message.planEndsAt();
    }

    ChatMessage toDomain() {
        return new ChatMessage(id, planId, authorId, authorName, text, sentAt, planEndsAt);
    }
}

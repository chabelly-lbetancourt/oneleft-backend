package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sent_notice")
class SentNoticeEntity {

    @EmbeddedId
    private Key id;
    @Column(name = "sent_at")
    private Instant sentAt;

    protected SentNoticeEntity() {
    }

    SentNoticeEntity(UUID userId, UUID planId, Instant sentAt) {
        this.id = new Key(userId, planId);
        this.sentAt = sentAt;
    }

    @Embeddable
    record Key(@Column(name = "user_id") UUID userId, @Column(name = "plan_id") UUID planId) implements Serializable {
    }
}

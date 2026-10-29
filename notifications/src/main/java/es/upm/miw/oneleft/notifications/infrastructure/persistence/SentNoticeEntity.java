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
    @Column(name = "ranking")
    private String ranking;
    @Column(name = "batch")
    private int batch;

    protected SentNoticeEntity() {
    }

    SentNoticeEntity(UUID userId, UUID planId, Instant sentAt, String ranking, int batch) {
        this.id = new Key(userId, planId);
        this.sentAt = sentAt;
        this.ranking = ranking;
        this.batch = batch;
    }

    @Embeddable
    record Key(@Column(name = "user_id") UUID userId, @Column(name = "plan_id") UUID planId) implements Serializable {
    }
}

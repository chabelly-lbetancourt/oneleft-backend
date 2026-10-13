package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.Badge;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** A badge earned, read only: badges are inserted with {@code INSERT ... ON CONFLICT DO NOTHING}. */
@Entity
@Immutable
@Table(name = "badge")
public class BadgeEntity {

    @EmbeddedId
    private Key id;

    @Column(name = "earned_at", nullable = false)
    private Instant earnedAt;

    protected BadgeEntity() {
    }

    Badge badge() {
        return id.badge;
    }

    Instant earnedAt() {
        return earnedAt;
    }

    /** Who earned which badge. */
    @Embeddable
    public record Key(@Column(name = "user_id") UUID userId,
                      @Enumerated(EnumType.STRING) @Column(length = 30) Badge badge) implements Serializable {
    }
}

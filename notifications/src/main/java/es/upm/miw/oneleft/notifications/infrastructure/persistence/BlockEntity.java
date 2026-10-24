package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** A block of the social service (HU-033), copied. Written with a native insert that ignores a repeated one. */
@Entity
@Table(name = "block")
public class BlockEntity {

    @EmbeddedId
    private Key id;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    protected BlockEntity() {
    }

    /** Who ({@code blockerId}) blocks whom ({@code blockedId}). */
    @Embeddable
    public record Key(@Column(name = "blocker_id") UUID blockerId,
                      @Column(name = "blocked_id") UUID blockedId) implements Serializable {
    }
}

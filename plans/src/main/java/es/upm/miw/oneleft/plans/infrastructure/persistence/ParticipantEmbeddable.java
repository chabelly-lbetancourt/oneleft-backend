package es.upm.miw.oneleft.plans.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.UUID;

@Embeddable
public class ParticipantEmbeddable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected ParticipantEmbeddable() {
    }

    ParticipantEmbeddable(UUID userId, String name, Instant joinedAt) {
        this.userId = userId;
        this.name = name;
        this.joinedAt = joinedAt;
    }

    UUID getUserId() { return userId; }
    String getName() { return name; }
    Instant getJoinedAt() { return joinedAt; }
}

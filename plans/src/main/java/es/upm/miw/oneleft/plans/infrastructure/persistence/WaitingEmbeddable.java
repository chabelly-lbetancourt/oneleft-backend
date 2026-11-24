package es.upm.miw.oneleft.plans.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.UUID;

/** Someone on the waiting list of a plan, with the priority their reputation gave them (HU-063). */
@Embeddable
public class WaitingEmbeddable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(nullable = false)
    private int priority;

    protected WaitingEmbeddable() {
    }

    WaitingEmbeddable(UUID userId, String name, Instant joinedAt, int priority) {
        this.userId = userId;
        this.name = name;
        this.joinedAt = joinedAt;
        this.priority = priority;
    }

    UUID getUserId() { return userId; }
    String getName() { return name; }
    Instant getJoinedAt() { return joinedAt; }
    int getPriority() { return priority; }
}

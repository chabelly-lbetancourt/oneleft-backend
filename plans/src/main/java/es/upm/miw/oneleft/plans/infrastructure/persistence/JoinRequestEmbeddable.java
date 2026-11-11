package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.JoinRequest;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.util.UUID;

@Embeddable
public class JoinRequestEmbeddable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 140)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "age_band", length = 10)
    private String ageBand;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Level level;

    protected JoinRequestEmbeddable() {
    }

    JoinRequestEmbeddable(JoinRequest request) {
        this.userId = request.userId();
        this.name = request.name();
        this.message = request.message();
        this.status = request.status();
        this.requestedAt = request.requestedAt();
        this.decidedAt = request.decidedAt();
        this.ageBand = request.ageBand();
        this.level = request.level();
    }

    JoinRequest toDomain() {
        return new JoinRequest(userId, name, message, status, requestedAt, decidedAt, ageBand, level);
    }
}

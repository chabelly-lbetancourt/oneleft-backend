package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.TrustedLink;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plan_trusted_link")
class TrustedLinkEntity {

    @Id
    private UUID id;
    @Column(name = "plan_id", nullable = false)
    private UUID planId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected TrustedLinkEntity() {
    }

    TrustedLinkEntity(TrustedLink link) {
        this.id = link.id();
        this.planId = link.planId();
        this.userId = link.userId();
        this.tokenHash = link.tokenHash();
        this.createdAt = link.createdAt();
        this.expiresAt = link.expiresAt();
    }

    TrustedLink toDomain() {
        return new TrustedLink(id, planId, userId, tokenHash, createdAt, expiresAt);
    }
}

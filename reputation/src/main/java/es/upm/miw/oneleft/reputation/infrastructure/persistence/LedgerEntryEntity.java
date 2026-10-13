package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/** An entry of the ledger, read only: entries are inserted with {@code INSERT ... ON CONFLICT DO NOTHING}. */
@Entity
@Immutable
@Table(name = "reputation_entry")
public class LedgerEntryEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Reason reason;

    @Column(nullable = false)
    private int points;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "plan_title", length = 80)
    private String planTitle;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected LedgerEntryEntity() {
    }

    LedgerEntry toDomain() {
        return new LedgerEntry(userId, reason, points, sourceId, planTitle, occurredAt);
    }
}

package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface SpringDataLedgerRepository extends JpaRepository<LedgerEntryEntity, Long> {

    /** @return 1 if the entry is new, 0 if it was already there */
    @Modifying
    @NativeQuery("""
            INSERT INTO reputation_entry (user_id, reason, points, source_id, plan_title, occurred_at)
            VALUES (:userId, :reason, :points, :sourceId, :planTitle, :occurredAt)
            ON CONFLICT (user_id, reason, source_id) DO NOTHING""")
    int append(@Param("userId") UUID userId, @Param("reason") String reason, @Param("points") int points,
               @Param("sourceId") UUID sourceId, @Param("planTitle") String planTitle,
               @Param("occurredAt") Instant occurredAt);

    Page<LedgerEntryEntity> findByUserIdOrderByOccurredAtDescIdDesc(UUID userId, Pageable page);
}

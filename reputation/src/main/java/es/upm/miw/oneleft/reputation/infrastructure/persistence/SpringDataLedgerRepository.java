package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.Reason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataLedgerRepository extends JpaRepository<LedgerEntryEntity, Long> {

    /** @return 1 if the entry is new, 0 if it was already there */
    @Modifying
    @NativeQuery("""
            INSERT INTO reputation_entry (user_id, reason, points, source_id, plan_title, occurred_at, limited_by)
            VALUES (:userId, :reason, :points, :sourceId, :planTitle, :occurredAt, :limitedBy)
            ON CONFLICT (user_id, reason, source_id) DO NOTHING""")
    int append(@Param("userId") UUID userId, @Param("reason") String reason, @Param("points") int points,
               @Param("sourceId") UUID sourceId, @Param("planTitle") String planTitle,
               @Param("occurredAt") Instant occurredAt, @Param("limitedBy") String limitedBy);

    /** The entries of a person and reason in an interval, for the daily caps (HU-066) */
    @Query("SELECT COUNT(e) FROM LedgerEntryEntity e WHERE e.userId = :userId AND e.reason = :reason "
            + "AND e.occurredAt >= :from AND e.occurredAt < :to")
    long count(@Param("userId") UUID userId, @Param("reason") Reason reason, @Param("from") Instant from,
               @Param("to") Instant to);

    Page<LedgerEntryEntity> findByUserIdOrderByOccurredAtDescIdDesc(UUID userId, Pageable page);

    /** The title of a plan, as the entries it caused keep it (HU-060) */
    @Query("SELECT e.planTitle FROM LedgerEntryEntity e WHERE e.sourceId = :planId AND e.planTitle IS NOT NULL "
            + "ORDER BY e.id")
    List<String> titlesOf(@Param("planId") UUID planId, Pageable page);
}

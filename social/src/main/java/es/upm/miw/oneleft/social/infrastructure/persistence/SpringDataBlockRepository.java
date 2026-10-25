package es.upm.miw.oneleft.social.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface SpringDataBlockRepository extends JpaRepository<BlockEntity, BlockEntity.Key> {

    /** @return 1 if it was not blocked yet, 0 if it already was */
    @Modifying
    @NativeQuery("""
            INSERT INTO block (blocker_id, blocked_id, blocked_at) VALUES (:blockerId, :blockedId, :at)
            ON CONFLICT (blocker_id, blocked_id) DO NOTHING""")
    int block(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId, @Param("at") Instant at);

    @Modifying
    @Query("DELETE FROM BlockEntity b WHERE b.id.blockerId = :blockerId AND b.id.blockedId = :blockedId")
    int unblock(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);

    /** The people someone blocked, with their names, in one query. */
    @Query(value = """
            SELECT new es.upm.miw.oneleft.social.infrastructure.persistence.BlockedRow(p.userId, p.name, b.blockedAt)
            FROM BlockEntity b JOIN PersonEntity p ON p.userId = b.id.blockedId
            WHERE b.id.blockerId = :blockerId
            ORDER BY b.blockedAt DESC, p.name""",
            countQuery = "SELECT COUNT(b) FROM BlockEntity b WHERE b.id.blockerId = :blockerId")
    Page<BlockedRow> blockedBy(@Param("blockerId") UUID blockerId, Pageable page);
}

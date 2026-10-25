package es.upm.miw.oneleft.plans.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface SpringDataBlockRepository extends JpaRepository<BlockEntity, BlockEntity.Key> {

    @Modifying
    @NativeQuery("""
            INSERT INTO block (blocker_id, blocked_id, blocked_at) VALUES (:blockerId, :blockedId, :at)
            ON CONFLICT (blocker_id, blocked_id) DO NOTHING""")
    int block(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId, @Param("at") Instant at);

    @Modifying
    @Query("DELETE FROM BlockEntity b WHERE b.id.blockerId = :blockerId AND b.id.blockedId = :blockedId")
    int unblock(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);

    @Query("""
            SELECT COUNT(b) > 0 FROM BlockEntity b
            WHERE (b.id.blockerId = :userId AND b.id.blockedId = :otherId)
               OR (b.id.blockerId = :otherId AND b.id.blockedId = :userId)""")
    boolean between(@Param("userId") UUID userId, @Param("otherId") UUID otherId);
}

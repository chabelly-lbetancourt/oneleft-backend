package es.upm.miw.oneleft.plans.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataTrustedLinkRepository extends JpaRepository<TrustedLinkEntity, UUID> {

    /** Locks the row of the plan until the end of the transaction: one link at a time is created for it */
    @NativeQuery("SELECT 1 FROM plan WHERE id = :planId FOR UPDATE")
    Optional<Integer> lockPlan(@Param("planId") UUID planId);

    Optional<TrustedLinkEntity> findByTokenHash(String tokenHash);

    List<TrustedLinkEntity> findByPlanIdAndUserIdAndExpiresAtAfterOrderByCreatedAtAscIdAsc(UUID planId, UUID userId,
                                                                                          Instant now);

    @Modifying
    @Query("DELETE FROM TrustedLinkEntity l WHERE l.id = :id AND l.planId = :planId AND l.userId = :userId")
    int deleteOwn(@Param("id") UUID id, @Param("planId") UUID planId, @Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM TrustedLinkEntity l WHERE l.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}

package es.upm.miw.oneleft.plans.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

interface SpringDataMemoryRepository extends JpaRepository<MemoryEntity, UUID> {

    /** Locks the row of the plan until the end of the transaction: one photo at a time is added to its album */
    @NativeQuery("SELECT 1 FROM plan WHERE id = :planId FOR UPDATE")
    Optional<Integer> lockPlan(@Param("planId") UUID planId);

    long countByPlanId(UUID planId);

    long countByPlanIdAndUserId(UUID planId, UUID userId);

    Optional<MemoryEntity> findByObjectKey(String objectKey);

    Optional<MemoryEntity> findByIdAndPlanId(UUID id, UUID planId);

    Page<MemoryEntity> findByPlanIdOrderByUploadedAtDescIdDesc(UUID planId, Pageable page);

    @Query("SELECT m.objectKey FROM MemoryEntity m WHERE m.planId = :planId AND m.userId = :userId")
    Set<String> keysOf(@Param("planId") UUID planId, @Param("userId") UUID userId);
}

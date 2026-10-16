package es.upm.miw.oneleft.social.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface SpringDataHiddenPlanRepository extends JpaRepository<HiddenPlanEntity, HiddenPlanEntity.Key> {

    /** @return 1 if it was not hidden yet, 0 if it already was */
    @Modifying
    @NativeQuery("""
            INSERT INTO hidden_plan (user_id, plan_id, hidden_at) VALUES (:userId, :planId, :at)
            ON CONFLICT (user_id, plan_id) DO NOTHING""")
    int hide(@Param("userId") UUID userId, @Param("planId") UUID planId, @Param("at") Instant at);

    @Modifying
    @Query("DELETE FROM HiddenPlanEntity h WHERE h.id.userId = :userId AND h.id.planId = :planId")
    int show(@Param("userId") UUID userId, @Param("planId") UUID planId);
}

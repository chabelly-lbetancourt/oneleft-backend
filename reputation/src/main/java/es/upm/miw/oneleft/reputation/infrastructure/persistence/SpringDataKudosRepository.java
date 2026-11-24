package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataKudosRepository extends JpaRepository<KudosEntity, KudosEntity.Key> {

    /** @return 1 if it is new, 0 if the giver had already given one to that person in that plan */
    @Modifying
    @NativeQuery("""
            INSERT INTO kudos (giver_id, receiver_id, plan_id, kind, given_at)
            VALUES (:giverId, :receiverId, :planId, :kind, :givenAt)
            ON CONFLICT (giver_id, receiver_id, plan_id) DO NOTHING""")
    int give(@Param("giverId") UUID giverId, @Param("receiverId") UUID receiverId, @Param("planId") UUID planId,
             @Param("kind") String kind, @Param("givenAt") Instant givenAt);

    List<KudosEntity> findByIdGiverIdAndIdPlanId(UUID giverId, UUID planId);

    /** The kudos someone received, by kind, in one query */
    @Query("SELECT k.kind AS kind, COUNT(k) AS total FROM KudosEntity k WHERE k.id.receiverId = :receiverId "
            + "GROUP BY k.kind")
    List<KindCount> countsOf(@Param("receiverId") UUID receiverId);

    /** The kudos from a giver to a receiver in an interval, both ends included (HU-066) */
    @Query("SELECT COUNT(k) FROM KudosEntity k WHERE k.id.giverId = :giverId AND k.id.receiverId = :receiverId "
            + "AND k.givenAt >= :from AND k.givenAt <= :to")
    long countGiven(@Param("giverId") UUID giverId, @Param("receiverId") UUID receiverId,
                    @Param("from") Instant from, @Param("to") Instant to);

    interface KindCount {
        KudosKind getKind();

        long getTotal();
    }
}

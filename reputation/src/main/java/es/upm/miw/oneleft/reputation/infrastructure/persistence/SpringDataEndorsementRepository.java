package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataEndorsementRepository extends JpaRepository<EndorsementEntity, EndorsementEntity.Key> {

    /** @return 1 if it is new, 0 if the endorser had already endorsed that person in that activity */
    @Modifying
    @NativeQuery("""
            INSERT INTO endorsement (endorser_id, receiver_id, activity, level, given_at)
            VALUES (:endorserId, :receiverId, :activity, :level, :givenAt)
            ON CONFLICT (endorser_id, receiver_id, activity) DO NOTHING""")
    int give(@Param("endorserId") UUID endorserId, @Param("receiverId") UUID receiverId,
             @Param("activity") String activity, @Param("level") String level, @Param("givenAt") Instant givenAt);

    List<EndorsementEntity> findByIdEndorserIdAndIdReceiverId(UUID endorserId, UUID receiverId);

    /** How many people vouch for each level of each activity of someone, in one query */
    @Query("SELECT e.id.activity AS activity, e.level AS vouched, COUNT(e) AS total FROM EndorsementEntity e "
            + "WHERE e.id.receiverId = :receiverId GROUP BY e.id.activity, e.level ORDER BY e.id.activity, e.level")
    List<LevelCount> countsOf(@Param("receiverId") UUID receiverId);

    /** The endorsements from someone to someone else in an interval, both ends included (HU-066) */
    @Query("SELECT COUNT(e) FROM EndorsementEntity e WHERE e.id.endorserId = :endorserId "
            + "AND e.id.receiverId = :receiverId AND e.givenAt >= :from AND e.givenAt <= :to")
    long countGiven(@Param("endorserId") UUID endorserId, @Param("receiverId") UUID receiverId,
                    @Param("from") Instant from, @Param("to") Instant to);

    interface LevelCount {

        String getActivity();

        SkillLevel getVouched();

        long getTotal();
    }
}

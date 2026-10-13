package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataBadgeRepository extends JpaRepository<BadgeEntity, BadgeEntity.Key> {

    /** @return 1 if the badge is new for the person, 0 if they had it */
    @Modifying
    @NativeQuery("""
            INSERT INTO badge (user_id, badge, earned_at) VALUES (:userId, :badge, :earnedAt)
            ON CONFLICT (user_id, badge) DO NOTHING""")
    int award(@Param("userId") UUID userId, @Param("badge") String badge, @Param("earnedAt") Instant earnedAt);

    List<BadgeEntity> findByIdUserId(UUID userId);

    @Modifying
    @NativeQuery("""
            INSERT INTO track_record (user_id, plan_id, activity, organizer, arrived, held, closed_at)
            VALUES (:userId, :planId, :activity, :organizer, :arrived, :held, :closedAt)
            ON CONFLICT (user_id, plan_id) DO NOTHING""")
    void recordAttendance(@Param("userId") UUID userId, @Param("planId") UUID planId,
                          @Param("activity") String activity, @Param("organizer") boolean organizer,
                          @Param("arrived") boolean arrived, @Param("held") boolean held,
                          @Param("closedAt") Instant closedAt);

    /**
     * The track record of someone in one query: plans organized that were held, plans arrived at, their different
     * activities, the plans arrived at since the last no-show to a plan held, and the kudos of the ledger (HU-060).
     */
    @NativeQuery("""
            SELECT
              (SELECT COUNT(*) FROM track_record WHERE user_id = :userId AND organizer AND held) AS organized_held,
              (SELECT COUNT(*) FROM track_record WHERE user_id = :userId AND arrived) AS completed,
              (SELECT COUNT(DISTINCT activity) FROM track_record WHERE user_id = :userId AND arrived) AS activities,
              (SELECT COUNT(*) FROM track_record t WHERE t.user_id = :userId AND t.arrived
                 AND t.closed_at > COALESCE((SELECT MAX(m.closed_at) FROM track_record m
                                             WHERE m.user_id = :userId AND m.held AND NOT m.arrived),
                                            '-infinity'::timestamptz)) AS streak,
              (SELECT COUNT(*) FROM reputation_entry WHERE user_id = :userId AND reason = 'KUDOS') AS kudos""")
    Record trackRecordOf(@Param("userId") UUID userId);

    interface Record {
        long getOrganizedHeld();

        long getCompleted();

        long getActivities();

        long getStreak();

        long getKudos();
    }
}

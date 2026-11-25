package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataTrackRecordRepository extends JpaRepository<TrackRecordEntity, TrackRecordEntity.Key> {

    List<TrackRecordEntity> findByIdPlanId(UUID planId);

    /** The plans of someone else that a person arrived at (HU-043) */
    @NativeQuery("SELECT COUNT(*) FROM track_record WHERE user_id = :userId AND arrived AND NOT organizer")
    long joinedOf(@Param("userId") UUID userId);

    /** The activities a person arrived at the most (HU-043); among equals, the one done the latest */
    @NativeQuery("""
            SELECT activity AS activity, COUNT(*) AS plans FROM track_record
            WHERE user_id = :userId AND arrived
            GROUP BY activity ORDER BY COUNT(*) DESC, MAX(closed_at) DESC, activity
            LIMIT :limit""")
    List<ActivityCount> topActivitiesOf(@Param("userId") UUID userId, @Param("limit") int limit);

    /**
     * The plans of a person by month from an instant on (HU-043), the month in the time of a zone: the plans they
     * arrived at, those they organized that were held and those of someone else they arrived at.
     */
    @NativeQuery("""
            SELECT line.month AS month,
                   COUNT(*) FILTER (WHERE line.arrived) AS completed,
                   COUNT(*) FILTER (WHERE line.organizer AND line.held) AS organized,
                   COUNT(*) FILTER (WHERE line.arrived AND NOT line.organizer) AS joined
            FROM (SELECT to_char(closed_at AT TIME ZONE CAST(:zone AS text), 'YYYY-MM') AS month, arrived,
                         organizer, held
                  FROM track_record WHERE user_id = :userId AND closed_at >= :since) line
            GROUP BY line.month""")
    List<MonthCount> monthsOf(@Param("userId") UUID userId, @Param("since") Instant since,
                              @Param("zone") String zone);

    interface ActivityCount {

        String getActivity();

        long getPlans();
    }

    interface MonthCount {

        /** The year and the month, as in 2026-11 */
        String getMonth();

        long getCompleted();

        long getOrganized();

        long getJoined();
    }
}

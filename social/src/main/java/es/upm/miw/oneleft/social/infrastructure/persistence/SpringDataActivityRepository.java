package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.ActivityType;
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

interface SpringDataActivityRepository extends JpaRepository<ActivityEntity, Long> {

    /** @return 1 if it is new, 0 if the event had already been received */
    @Modifying
    @NativeQuery("""
            INSERT INTO activity (actor_id, type, plan_id, plan_title, plan_activity, starts_at, occurred_at)
            VALUES (:actorId, :type, :planId, :planTitle, :planActivity, :startsAt, :occurredAt)
            ON CONFLICT (actor_id, type, plan_id) DO NOTHING""")
    int insert(@Param("actorId") UUID actorId, @Param("type") String type, @Param("planId") UUID planId,
               @Param("planTitle") String planTitle, @Param("planActivity") String planActivity,
               @Param("startsAt") Instant startsAt, @Param("occurredAt") Instant occurredAt);

    @Modifying
    @Query("DELETE FROM ActivityEntity a WHERE a.actorId = :actorId AND a.type = :type AND a.planId = :planId")
    int delete(@Param("actorId") UUID actorId, @Param("type") ActivityType type, @Param("planId") UUID planId);

    @Query("SELECT DISTINCT a.actorId FROM ActivityEntity a WHERE a.planId = :planId")
    List<UUID> actorsOf(@Param("planId") UUID planId);

    @Modifying
    @Query("DELETE FROM ActivityEntity a WHERE a.planId = :planId")
    int deletePlan(@Param("planId") UUID planId);

    /**
     * The feed of someone, computed when it is read (fan-out on read): the activity of the people they follow, once
     * accepted, with their names, the latest first. The index on (actor_id, occurred_at) serves each person followed.
     */
    @Query(value = """
            SELECT new es.upm.miw.oneleft.social.infrastructure.persistence.FeedRow(
                a.actorId, p.name, a.type, a.planId, a.planTitle, a.planActivity, a.startsAt, a.occurredAt)
            FROM ActivityEntity a
            JOIN FollowEntity f ON f.id.followeeId = a.actorId
            JOIN PersonEntity p ON p.userId = a.actorId
            WHERE f.id.followerId = :viewer
              AND f.status = es.upm.miw.oneleft.social.domain.model.FollowStatus.ACCEPTED
            ORDER BY a.occurredAt DESC, a.id DESC""",
            countQuery = """
                    SELECT COUNT(a) FROM ActivityEntity a
                    JOIN FollowEntity f ON f.id.followeeId = a.actorId
                    JOIN PersonEntity p ON p.userId = a.actorId
                    WHERE f.id.followerId = :viewer
                      AND f.status = es.upm.miw.oneleft.social.domain.model.FollowStatus.ACCEPTED""")
    Page<FeedRow> feedOf(@Param("viewer") UUID viewer, Pageable page);
}

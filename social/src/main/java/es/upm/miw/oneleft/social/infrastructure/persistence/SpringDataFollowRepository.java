package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface SpringDataFollowRepository extends JpaRepository<FollowEntity, FollowEntity.Key> {

    /** The followers of someone (or the people asking to), with their names, in one query. */
    @Query(value = """
            SELECT new es.upm.miw.oneleft.social.infrastructure.persistence.ConnectionRow(
                p.userId, p.name, f.status, f.requestedAt, f.acceptedAt)
            FROM FollowEntity f JOIN PersonEntity p ON p.userId = f.id.followerId
            WHERE f.id.followeeId = :userId AND f.status = :status
            ORDER BY COALESCE(f.acceptedAt, f.requestedAt) DESC, p.name""",
            countQuery = "SELECT COUNT(f) FROM FollowEntity f WHERE f.id.followeeId = :userId AND f.status = :status")
    Page<ConnectionRow> followersOf(@Param("userId") UUID userId, @Param("status") FollowStatus status,
                                    Pageable page);

    /** The people someone follows (or asks to follow), with their names, in one query. */
    @Query(value = """
            SELECT new es.upm.miw.oneleft.social.infrastructure.persistence.ConnectionRow(
                p.userId, p.name, f.status, f.requestedAt, f.acceptedAt)
            FROM FollowEntity f JOIN PersonEntity p ON p.userId = f.id.followeeId
            WHERE f.id.followerId = :userId AND f.status = :status
            ORDER BY COALESCE(f.acceptedAt, f.requestedAt) DESC, p.name""",
            countQuery = "SELECT COUNT(f) FROM FollowEntity f WHERE f.id.followerId = :userId AND f.status = :status")
    Page<ConnectionRow> followedBy(@Param("userId") UUID userId, @Param("status") FollowStatus status,
                                   Pageable page);

    @Modifying
    @Query("""
            UPDATE FollowEntity f SET f.status = es.upm.miw.oneleft.social.domain.model.FollowStatus.ACCEPTED,
                f.acceptedAt = :at
            WHERE f.id.followeeId = :userId
              AND f.status = es.upm.miw.oneleft.social.domain.model.FollowStatus.PENDING""")
    int acceptAll(@Param("userId") UUID userId, @Param("at") Instant at);

    /** The four counts of someone in a single pass over their follows. */
    @NativeQuery("""
            SELECT COUNT(*) FILTER (WHERE followee_id = :userId AND status = 'ACCEPTED') AS followers,
                   COUNT(*) FILTER (WHERE follower_id = :userId AND status = 'ACCEPTED') AS following,
                   COUNT(*) FILTER (WHERE followee_id = :userId AND status = 'PENDING')  AS requests,
                   COUNT(*) FILTER (WHERE follower_id = :userId AND status = 'PENDING')  AS sent
            FROM follow WHERE follower_id = :userId OR followee_id = :userId""")
    Counts counts(@Param("userId") UUID userId);

    interface Counts {
        long getFollowers();

        long getFollowing();

        long getRequests();

        long getSent();
    }
}

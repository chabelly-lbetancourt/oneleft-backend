package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SpringDataPlanRepository extends JpaRepository<PlanEntity, UUID> {

    /**
     * Whether a point is within some metres of the meeting point of a plan (HU-025), with PostGIS on {@code geography}
     * like the proximity search. Null when the plan does not exist.
     */
    @NativeQuery("""
            SELECT ST_DWithin(p.location::geography,
                              ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :meters)
            FROM plan p WHERE p.id = :id""")
    Boolean isWithin(@Param("id") UUID id, @Param("latitude") double latitude, @Param("longitude") double longitude,
                     @Param("meters") double meters);

    List<PlanEntity> findByOrganizerIdAndStartsAtAfterAndStatusNotOrderByStartsAt(UUID organizerId, Instant from,
                                                                               PlanStatus excluded);

    /**
     * Plans with a lifecycle step due (HU-007): open or full ones that start (or are about to, without a reminder yet)
     * or whose minimum reaches its deadline (HU-039), and those in progress that have ended. The {@code plan_lifecycle}
     * and {@code plan_minimum_pending} indexes keep it cheap every minute.
     */
    @NativeQuery("""
            SELECT p.id FROM plan p
            WHERE (p.status IN ('OPEN', 'FULL')
                   AND (p.starts_at <= :now OR (p.reminded_at IS NULL AND p.starts_at <= :remindUntil)
                        OR (p.min_participants IS NOT NULL AND p.confirmed_at IS NULL
                            AND p.minimum_deadline <= :now)))
               OR (p.status = 'IN_PROGRESS' AND p.starts_at <= :endedBefore)""")
    List<UUID> findDueForLifecycle(@Param("now") Instant now, @Param("remindUntil") Instant remindUntil,
                                   @Param("endedBefore") Instant endedBefore);

    /**
     * Proximity search with PostGIS. {@code ST_DWithin} on {@code geography} measures in metres and uses the
     * {@code plan_location_geography} index; the {@code <->} operator sorts from the nearest to the farthest. The plans
     * of someone blocked by the requester, or who blocked them, are left out (HU-033).
     */
    @NativeQuery("""
            SELECT p.* FROM plan p
            WHERE p.status = 'OPEN'
              AND p.occupied < p.spots
              AND p.starts_at > :now AND p.starts_at <= :until
              AND p.activity IN (:activities)
              AND p.organizer_id <> :requester
              AND NOT EXISTS (SELECT 1 FROM block b
                              WHERE (b.blocker_id = p.organizer_id AND b.blocked_id = :requester)
                                 OR (b.blocker_id = :requester AND b.blocked_id = p.organizer_id))
              AND ST_DWithin(p.location::geography,
                             ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radius)
            ORDER BY p.location::geography <-> ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
            LIMIT :limit""")
    @SuppressWarnings("java:S107")
    List<PlanEntity> findOpenNearby(@Param("latitude") double latitude, @Param("longitude") double longitude,
                                    @Param("radius") double radiusMeters,
                                    @Param("activities") Collection<String> activities,
                                    @Param("requester") UUID requester, @Param("now") Instant now,
                                    @Param("until") Instant until, @Param("limit") int limit);

    /** Plans not cancelled whose check-in has closed and whose attendance is still open, the oldest first. */
    @NativeQuery("""
            SELECT p.id FROM plan p
            WHERE p.attendance_closed_at IS NULL AND p.status <> 'CANCELLED' AND p.starts_at <= :startedBefore
            ORDER BY p.starts_at LIMIT :limit""")
    List<UUID> findAttendanceDue(@Param("startedBefore") Instant startedBefore, @Param("limit") int limit);

    @Modifying
    @NativeQuery("UPDATE plan SET attendance_closed_at = :at WHERE id = :id")
    int markAttendanceClosed(@Param("id") UUID id, @Param("at") Instant at);

    /**
     * The latest plans held that someone organized or arrived at (HU-048): finished, with a check-in of theirs or, if
     * they organized it, of anybody. Only the columns the profile shows.
     */
    @NativeQuery("""
            SELECT p.id AS id, p.title AS title, p.activity AS activity, p.starts_at AS startsAt,
                   p.organizer_id AS organizerId
            FROM plan p
            WHERE p.status = 'FINISHED'
              AND EXISTS (SELECT 1 FROM plan_check_in c
                          WHERE c.plan_id = p.id AND (c.user_id = :userId OR p.organizer_id = :userId))
            ORDER BY p.starts_at DESC
            LIMIT :limit""")
    List<RecentRow> findRecentlyHeldWith(@Param("userId") UUID userId, @Param("limit") int limit);

    interface RecentRow {
        UUID getId();

        String getTitle();

        String getActivity();

        Instant getStartsAt();

        UUID getOrganizerId();
    }
}

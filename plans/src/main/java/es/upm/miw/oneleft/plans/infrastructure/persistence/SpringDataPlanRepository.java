package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SpringDataPlanRepository extends JpaRepository<PlanEntity, UUID> {

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
     * {@code plan_location_geography} index; the {@code <->} operator sorts from the nearest to the farthest.
     */
    @NativeQuery("""
            SELECT p.* FROM plan p
            WHERE p.status = 'OPEN'
              AND p.occupied < p.spots
              AND p.starts_at > :now AND p.starts_at <= :until
              AND p.activity IN (:activities)
              AND p.organizer_id <> :requester
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
}

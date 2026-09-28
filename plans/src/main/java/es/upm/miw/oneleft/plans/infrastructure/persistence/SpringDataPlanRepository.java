package es.upm.miw.oneleft.plans.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SpringDataPlanRepository extends JpaRepository<PlanEntity, UUID> {

    List<PlanEntity> findByOrganizerIdAndStartsAtAfterOrderByStartsAt(UUID organizerId, Instant from);

    /**
     * Búsqueda por proximidad con PostGIS. {@code ST_DWithin} sobre {@code geography} mide en metros y usa el
     * índice {@code plan_location_geography}; el operador {@code <->} ordena del más cercano al más lejano.
     */
    @NativeQuery("""
            SELECT p.* FROM plan p
            WHERE p.status = 'ABIERTO'
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

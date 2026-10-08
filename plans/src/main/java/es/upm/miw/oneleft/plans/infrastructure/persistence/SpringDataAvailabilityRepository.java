package es.upm.miw.oneleft.plans.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataAvailabilityRepository extends JpaRepository<AvailabilityEntity, UUID> {

    /** Same PostGIS search as the nearby plans (HU-004), with its own geography index. */
    @NativeQuery("""
            SELECT a.* FROM availability a
            WHERE a.until > :now
              AND ST_DWithin(a.location::geography,
                             ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radius)""")
    List<AvailabilityEntity> findActiveNear(@Param("latitude") double latitude,
                                            @Param("longitude") double longitude,
                                            @Param("radius") double radiusMeters, @Param("now") Instant now);

    @Modifying
    @Query("delete from AvailabilityEntity a where a.until <= :now")
    int deleteExpired(@Param("now") Instant now);
}

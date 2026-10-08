package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.Availability;
import es.upm.miw.oneleft.plans.domain.port.out.AvailabilityRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaAvailabilityRepository implements AvailabilityRepository {

    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(),
            JpaPlanRepository.WGS84);

    private final SpringDataAvailabilityRepository jpa;
    private final Clock clock;

    JpaAvailabilityRepository(SpringDataAvailabilityRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public Availability save(Availability availability) {
        // One per person: turning free mode on again replaces the previous one
        jpa.findById(availability.userId()).ifPresent(previous -> {
            jpa.delete(previous);
            jpa.flush();
        });
        var location = GEOMETRY.createPoint(new Coordinate(availability.longitude(), availability.latitude()));
        return jpa.save(new AvailabilityEntity(availability, location, clock.instant())).toDomain();
    }

    @Override
    public Optional<Availability> findByUser(UUID userId) {
        return jpa.findById(userId).map(AvailabilityEntity::toDomain);
    }

    @Override
    public void delete(UUID userId) {
        jpa.deleteById(userId);
    }

    @Override
    public List<Availability> findActiveNear(double latitude, double longitude, double radiusMeters, Instant now) {
        return jpa.findActiveNear(latitude, longitude, radiusMeters, now).stream()
                .map(AvailabilityEntity::toDomain).toList();
    }

    @Override
    public int deleteExpired(Instant now) {
        return jpa.deleteExpired(now);
    }
}

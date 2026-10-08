package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.Availability;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** People in free mode (HU-035): one availability per person at most. */
public interface AvailabilityRepository {

    Availability save(Availability availability);

    Optional<Availability> findByUser(UUID userId);

    void delete(UUID userId);

    /** Active availabilities whose zone is within {@code radiusMeters} of the point. */
    List<Availability> findActiveNear(double latitude, double longitude, double radiusMeters, Instant now);

    /** @return how many expired availabilities were removed */
    int deleteExpired(Instant now);
}

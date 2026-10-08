package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Availability;
import es.upm.miw.oneleft.plans.domain.model.GeoDistance;
import es.upm.miw.oneleft.plans.domain.port.out.AvailabilityRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory free modes for the tests of the application layer (HU-035). */
class Availabilities implements AvailabilityRepository {

    final Map<UUID, Availability> data = new LinkedHashMap<>();

    @Override
    public Availability save(Availability availability) {
        data.put(availability.userId(), availability);
        return availability;
    }

    @Override
    public Optional<Availability> findByUser(UUID userId) {
        return Optional.ofNullable(data.get(userId));
    }

    @Override
    public void delete(UUID userId) {
        data.remove(userId);
    }

    @Override
    public List<Availability> findActiveNear(double latitude, double longitude, double radiusMeters, Instant now) {
        return data.values().stream().filter(availability -> availability.activeAt(now)
                && GeoDistance.meters(latitude, longitude, availability.latitude(), availability.longitude())
                <= radiusMeters).toList();
    }

    @Override
    public int deleteExpired(Instant now) {
        var expired = data.values().stream().filter(availability -> !availability.activeAt(now))
                .map(Availability::userId).toList();
        expired.forEach(data::remove);
        return expired.size();
    }
}

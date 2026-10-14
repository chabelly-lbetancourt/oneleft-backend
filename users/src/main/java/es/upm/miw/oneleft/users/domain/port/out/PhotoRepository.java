package es.upm.miw.oneleft.users.domain.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Which photo each person has: the key of the object in the storage. */
public interface PhotoRepository {

    Optional<String> keyOf(UUID userId);

    /** The keys of the people who have a photo, in one query */
    Map<UUID, String> keysOf(Collection<UUID> userIds);

    void save(UUID userId, String key, Instant updatedAt);

    void delete(UUID userId);
}

package es.upm.miw.oneleft.users.infrastructure.persistence;

import es.upm.miw.oneleft.users.domain.port.out.PhotoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Output adapter: which photo each person has, in PostgreSQL.
 */
@Repository
public class JpaPhotoRepository implements PhotoRepository {

    private final SpringDataPhotoRepository jpa;

    public JpaPhotoRepository(SpringDataPhotoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<String> keyOf(UUID userId) {
        return jpa.findById(userId).map(PhotoEntity::getObjectKey);
    }

    @Override
    public Map<UUID, String> keysOf(Collection<UUID> userIds) {
        return jpa.findAllById(userIds).stream()
                .collect(Collectors.toMap(PhotoEntity::getUserId, PhotoEntity::getObjectKey));
    }

    @Override
    public void save(UUID userId, String key, Instant updatedAt) {
        var entity = jpa.findById(userId).orElseGet(() -> new PhotoEntity(userId, key, updatedAt));
        entity.replace(key, updatedAt);
        jpa.save(entity);
    }

    @Override
    public void delete(UUID userId) {
        jpa.deleteById(userId);
    }
}

package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;
import es.upm.miw.oneleft.notifications.domain.port.out.AlertRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaAlertRepository implements AlertRepository {

    private final SpringDataAlertRepository repository;
    private final Clock clock;

    JpaAlertRepository(SpringDataAlertRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public List<SavedAlert> findByUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAt(userId).stream().map(AlertEntity::toDomain).toList();
    }

    @Override
    public Optional<SavedAlert> findById(UUID alertId) {
        return repository.findById(alertId).map(AlertEntity::toDomain);
    }

    @Override
    public long countByUser(UUID userId) {
        return repository.countByUserId(userId);
    }

    @Override
    public SavedAlert save(SavedAlert alert) {
        var entity = repository.findById(alert.id())
                .map(existing -> {
                    existing.update(alert);
                    return existing;
                })
                .orElseGet(() -> AlertEntity.of(alert, clock.instant()));
        return repository.save(entity).toDomain();
    }

    @Override
    public void delete(UUID alertId) {
        repository.deleteById(alertId);
    }

    @Override
    public List<SavedAlert> findFor(Activity activity) {
        return repository.findFor(activity).stream().map(AlertEntity::toDomain).toList();
    }
}

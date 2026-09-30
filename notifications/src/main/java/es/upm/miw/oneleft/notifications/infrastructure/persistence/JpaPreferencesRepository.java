package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.port.out.PreferencesRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaPreferencesRepository implements PreferencesRepository {

    private final SpringDataPreferencesRepository repository;
    private final Clock clock;

    JpaPreferencesRepository(SpringDataPreferencesRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Optional<NotificationPreferences> findByUser(UUID userId) {
        return repository.findById(userId).map(PreferencesEntity::toDomain);
    }

    @Override
    public NotificationPreferences save(NotificationPreferences preferences) {
        var entity = repository.findById(preferences.userId())
                .map(existing -> {
                    existing.update(preferences, clock.instant());
                    return existing;
                })
                .orElseGet(() -> PreferencesEntity.of(preferences, clock.instant()));
        return repository.save(entity).toDomain();
    }

    @Override
    public List<NotificationPreferences> findEnabledFor(Activity activity) {
        return repository.findEnabledFor(activity).stream().map(PreferencesEntity::toDomain).toList();
    }
}

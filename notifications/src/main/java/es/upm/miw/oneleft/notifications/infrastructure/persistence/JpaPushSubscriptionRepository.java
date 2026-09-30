package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSubscriptionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Repository
class JpaPushSubscriptionRepository implements PushSubscriptionRepository {

    private final SpringDataPushSubscriptionRepository repository;
    private final Clock clock;

    JpaPushSubscriptionRepository(SpringDataPushSubscriptionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void save(PushSubscription subscription) {
        repository.save(PushSubscriptionEntity.of(subscription, clock.instant()));
    }

    @Override
    public List<PushSubscription> findByUser(UUID userId) {
        return repository.findByUserId(userId).stream().map(PushSubscriptionEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(String endpoint) {
        repository.deleteById(endpoint);
    }

    @Override
    @Transactional
    public void delete(UUID userId, String endpoint) {
        repository.deleteByUserAndEndpoint(userId, endpoint);
    }
}

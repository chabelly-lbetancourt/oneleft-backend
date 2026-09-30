package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;

import java.util.List;
import java.util.UUID;

public interface PushSubscriptionRepository {

    /** Saves the subscription; a browser that subscribes again replaces its previous keys. */
    void save(PushSubscription subscription);

    List<PushSubscription> findByUser(UUID userId);

    void delete(String endpoint);

    void delete(UUID userId, String endpoint);
}

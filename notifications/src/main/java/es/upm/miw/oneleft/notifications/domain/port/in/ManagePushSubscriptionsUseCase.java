package es.upm.miw.oneleft.notifications.domain.port.in;

import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;

import java.util.UUID;

/**
 * Browsers that receive the notices as system notifications (Web Push).
 */
public interface ManagePushSubscriptionsUseCase {

    void subscribe(PushSubscription subscription);

    void unsubscribe(UUID userId, String endpoint);
}

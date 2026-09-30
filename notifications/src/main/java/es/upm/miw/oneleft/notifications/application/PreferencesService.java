package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;
import es.upm.miw.oneleft.notifications.domain.port.in.ManagePreferencesUseCase;
import es.upm.miw.oneleft.notifications.domain.port.in.ManagePushSubscriptionsUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.PreferencesRepository;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PreferencesService implements ManagePreferencesUseCase, ManagePushSubscriptionsUseCase {

    private final PreferencesRepository preferences;
    private final PushSubscriptionRepository subscriptions;

    public PreferencesService(PreferencesRepository preferences, PushSubscriptionRepository subscriptions) {
        this.preferences = preferences;
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferences preferencesOf(UUID userId) {
        return preferences.findByUser(userId).orElseGet(() -> NotificationPreferences.defaults(userId));
    }

    @Override
    @Transactional
    public NotificationPreferences update(NotificationPreferences updated) {
        return preferences.save(updated);
    }

    @Override
    @Transactional
    public void subscribe(PushSubscription subscription) {
        subscriptions.save(subscription);
    }

    @Override
    @Transactional
    public void unsubscribe(UUID userId, String endpoint) {
        subscriptions.delete(userId, endpoint);
    }
}

package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreferencesRepository {

    Optional<NotificationPreferences> findByUser(UUID userId);

    NotificationPreferences save(NotificationPreferences preferences);

    /** People with notices on who want to hear about the activity (or about any activity). */
    List<NotificationPreferences> findEnabledFor(Activity activity);
}

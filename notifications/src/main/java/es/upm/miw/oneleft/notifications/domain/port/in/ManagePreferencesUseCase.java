package es.upm.miw.oneleft.notifications.domain.port.in;

import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;

import java.util.UUID;

/**
 * Each person's notification preferences (HU-006).
 */
public interface ManagePreferencesUseCase {

    /** The saved preferences, or the defaults (notices off) if the person never chose. */
    NotificationPreferences preferencesOf(UUID userId);

    NotificationPreferences update(NotificationPreferences preferences);
}

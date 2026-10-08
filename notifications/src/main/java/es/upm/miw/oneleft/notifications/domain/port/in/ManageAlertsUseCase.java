package es.upm.miw.oneleft.notifications.domain.port.in;

import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;

import java.util.List;
import java.util.UUID;

/**
 * Each person's saved alerts (HU-036). Only the owner sees or changes an alert; the data arrives as a
 * {@link SavedAlert} whose id and owner are ignored.
 */
public interface ManageAlertsUseCase {

    /** Most alerts a person can keep. It could grow with the reputation of HU-009. */
    int MAX_ALERTS = 5;

    List<SavedAlert> alertsOf(UUID userId);

    SavedAlert create(UUID userId, SavedAlert data);

    SavedAlert update(UUID userId, UUID alertId, SavedAlert data);

    void delete(UUID userId, UUID alertId);
}

package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Saved alerts (HU-036). */
public interface AlertRepository {

    List<SavedAlert> findByUser(UUID userId);

    Optional<SavedAlert> findById(UUID alertId);

    long countByUser(UUID userId);

    SavedAlert save(SavedAlert alert);

    void delete(UUID alertId);

    /** Alerts that look for the activity (or for any activity): the candidates for a newly published plan. */
    List<SavedAlert> findFor(Activity activity);
}

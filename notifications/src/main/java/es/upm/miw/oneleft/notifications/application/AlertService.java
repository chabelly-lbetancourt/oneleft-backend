package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.AlertLimitException;
import es.upm.miw.oneleft.notifications.domain.model.AlertNotFoundException;
import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;
import es.upm.miw.oneleft.notifications.domain.port.in.ManageAlertsUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Saved alerts of each person (HU-036), up to {@link #MAX_ALERTS}. */
@Service
public class AlertService implements ManageAlertsUseCase {

    private final AlertRepository alerts;

    public AlertService(AlertRepository alerts) {
        this.alerts = alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedAlert> alertsOf(UUID userId) {
        return alerts.findByUser(userId);
    }

    @Override
    @Transactional
    public SavedAlert create(UUID userId, SavedAlert data) {
        if (alerts.countByUser(userId) >= MAX_ALERTS) {
            throw new AlertLimitException(MAX_ALERTS);
        }
        return alerts.save(new SavedAlert(UUID.randomUUID(), userId, data.name(), data.activities(), data.level(),
                data.latitude(), data.longitude(), data.radiusMeters(), data.days(), data.from(), data.to()));
    }

    @Override
    @Transactional
    public SavedAlert update(UUID userId, UUID alertId, SavedAlert data) {
        return alerts.save(owned(userId, alertId).edit(data));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID alertId) {
        alerts.delete(owned(userId, alertId).id());
    }

    private SavedAlert owned(UUID userId, UUID alertId) {
        return alerts.findById(alertId).filter(alert -> alert.userId().equals(userId))
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }
}

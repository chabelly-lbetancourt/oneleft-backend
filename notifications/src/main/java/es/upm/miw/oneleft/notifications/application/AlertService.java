package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.AlertLimitException;
import es.upm.miw.oneleft.notifications.domain.model.AlertNotFoundException;
import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;
import es.upm.miw.oneleft.notifications.domain.port.in.ManageAlertsUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.AlertRepository;
import es.upm.miw.oneleft.notifications.domain.port.out.PerkCopies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Saved alerts of each person (HU-036), up to {@link #MAX_ALERTS}, or up to what their reputation gives them (HU-063):
 * the copy of their perks is only looked at when they reach the limit of everyone.
 */
@Service
public class AlertService implements ManageAlertsUseCase {

    private final AlertRepository alerts;
    private final PerkCopies perks;

    public AlertService(AlertRepository alerts) {
        this(alerts, PerkCopies.NONE);
    }

    @Autowired
    public AlertService(AlertRepository alerts, PerkCopies perks) {
        this.alerts = alerts;
        this.perks = perks;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedAlert> alertsOf(UUID userId) {
        return alerts.findByUser(userId);
    }

    @Override
    @Transactional
    public SavedAlert create(UUID userId, SavedAlert data) {
        var kept = alerts.countByUser(userId);
        if (kept >= MAX_ALERTS) {
            var max = maxAlertsOf(userId);
            if (kept >= max) {
                throw new AlertLimitException(max);
            }
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

    /** Never fewer than everyone: a perk only adds */
    private int maxAlertsOf(UUID userId) {
        var known = perks.findByUsers(List.of(userId)).get(userId);
        return known == null ? MAX_ALERTS : Math.max(MAX_ALERTS, known.maxSavedAlerts());
    }

    private SavedAlert owned(UUID userId, UUID alertId) {
        return alerts.findById(alertId).filter(alert -> alert.userId().equals(userId))
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }
}

package es.upm.miw.oneleft.notifications.domain.port.in;

import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;

/**
 * Lets the people who want it know that a plan has been published near them (HU-006).
 */
public interface NotifyNearbyPlanUseCase {

    /** @return how many people were notified */
    int notifyNearby(PublishedPlan plan);
}

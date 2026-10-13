package es.upm.miw.oneleft.notifications.domain.port.in;

import es.upm.miw.oneleft.notifications.domain.model.PlanReminder;

/**
 * Reminds everyone in a plan that it is about to start (HU-007), as a system notification of their browsers.
 */
public interface RemindPlanUseCase {

    /** @return how many notifications were sent */
    int remind(PlanReminder reminder);
}

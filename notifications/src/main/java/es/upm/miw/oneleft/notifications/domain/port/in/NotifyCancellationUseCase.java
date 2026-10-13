package es.upm.miw.oneleft.notifications.domain.port.in;

import es.upm.miw.oneleft.notifications.domain.model.PlanCancellation;

/**
 * Tells everyone in a plan that it has been cancelled (HU-039), as a system notification of their browsers.
 */
public interface NotifyCancellationUseCase {

    /** @return how many notifications were sent */
    int notifyCancellation(PlanCancellation cancellation);
}

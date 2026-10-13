package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.PlanCancellation;
import es.upm.miw.oneleft.notifications.domain.model.PlanReminder;
import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;

/**
 * Sends a notice as a system notification of the browser (Web Push), even with the app closed.
 */
public interface PushSender {

    enum Result { SENT, GONE, FAILED }

    /** Whether Web Push is configured (server keys). Without it, only the in-app notice is sent. */
    boolean enabled();

    /** @return {@link Result#GONE} when the browser dropped the subscription, which can then be deleted */
    Result send(PushSubscription subscription, NearbyPlanNotice notice);

    /** The reminder of a plan that is about to start (HU-007). */
    Result send(PushSubscription subscription, PlanReminder reminder);

    /** The cancellation of a plan that did not reach its minimum of participants (HU-039). */
    Result send(PushSubscription subscription, PlanCancellation cancellation);
}

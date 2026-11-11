package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.PlanArrival;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.JoinRequested;
import es.upm.miw.oneleft.plans.domain.model.PlanCheckedIn;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import es.upm.miw.oneleft.plans.domain.model.RequestDecided;

/**
 * Output port to messaging: the domain does not know that RabbitMQ is behind it.
 */
public interface PlanEventPublisher {

    void publish(PlanPublished event);

    void publish(PlanJoined event);

    void publish(PlanLeftEvent event);

    void publish(PlanReminder event);

    void publish(PlanCancelled event);

    void publish(PlanArrival event);

    void publish(PlanCheckedIn event);

    /** Someone asked for a spot (HU-069). Nothing by default: only the messaging adapter announces it. */
    default void publish(JoinRequested event) {
    }

    /** The organizer decided on a request, or the plan filled up first (HU-069). */
    default void publish(RequestDecided event) {
    }
}

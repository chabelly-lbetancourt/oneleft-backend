package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;

/**
 * Output port to messaging: the domain does not know that RabbitMQ is behind it.
 */
public interface PlanEventPublisher {

    void publish(PlanPublished event);

    void publish(PlanJoined event);

    void publish(PlanLeftEvent event);

    void publish(PlanReminder event);

    void publish(PlanCancelled event);
}

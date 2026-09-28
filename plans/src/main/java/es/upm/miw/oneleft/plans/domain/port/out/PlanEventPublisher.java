package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.PlanPublished;

/**
 * Output port to messaging: the domain does not know that RabbitMQ is behind it.
 */
public interface PlanEventPublisher {

    void publish(PlanPublished event);
}

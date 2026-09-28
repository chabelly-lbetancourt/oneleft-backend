package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.PlanPublished;

/**
 * Puerto de salida hacia la mensajería: el dominio no sabe que detrás hay RabbitMQ.
 */
public interface PlanEventPublisher {

    void publish(PlanPublished event);
}

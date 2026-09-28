package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Plan;

/**
 * HU-003: publish a plan for the next few hours with free spots.
 */
public interface PublishPlanUseCase {

    Plan publish(PublishPlanCommand command);
}

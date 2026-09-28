package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Plan;

/**
 * HU-003: publicar un plan para las próximas horas con plazas libres.
 */
public interface PublishPlanUseCase {

    Plan publish(PublishPlanCommand command);
}

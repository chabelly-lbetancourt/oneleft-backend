package es.upm.miw.oneleft.plans.domain.model;

import java.util.Optional;

/**
 * Result of the organizer cancelling their plan: the cancelled plan and the event to publish, which is missing when
 * the plan was already cancelled and there is nobody left to tell.
 */
public record CancelledPlan(Plan plan, Optional<PlanCancelled> event) {
}

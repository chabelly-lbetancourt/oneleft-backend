package es.upm.miw.oneleft.plans.domain.model;

/**
 * Result of reminding a plan: the plan that records it and the event to publish.
 */
public record PlanReminded(Plan plan, PlanReminder event) {
}

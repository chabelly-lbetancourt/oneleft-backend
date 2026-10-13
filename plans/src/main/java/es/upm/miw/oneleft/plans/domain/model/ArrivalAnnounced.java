package es.upm.miw.oneleft.plans.domain.model;

/** Result of announcing an arrival: the plan that records it and the event to publish. */
public record ArrivalAnnounced(Plan plan, PlanArrival event) {
}

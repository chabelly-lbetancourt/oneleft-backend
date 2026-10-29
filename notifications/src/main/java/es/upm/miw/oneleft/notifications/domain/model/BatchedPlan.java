package es.upm.miw.oneleft.notifications.domain.model;

/** A plan with people still waiting to be notified: the plan, how it was ranked and the number of the next batch. */
public record BatchedPlan(PublishedPlan plan, String ranking, int nextBatch) {
}

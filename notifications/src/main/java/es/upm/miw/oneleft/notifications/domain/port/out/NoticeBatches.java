package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.BatchedPlan;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.RankedCandidate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The people still to be notified of each plan, in order, and when the next batch is due (HU-012).
 */
public interface NoticeBatches {

    /** Keeps the people that did not fit in the first batch; the next batch is the number 1. */
    void schedule(PublishedPlan plan, String ranking, List<RankedCandidate> rest, Instant nextBatchAt);

    /** Plans whose next batch is due. */
    List<BatchedPlan> due(Instant now);

    /** Takes (and removes) the next people of the plan, the best first. */
    List<UUID> takeNext(UUID planId, int limit);

    /** Plans the next batch for later, or finishes the plan if nobody is left. */
    void rescheduleOrFinish(UUID planId, Instant nextBatchAt);

    /** The plan has no more free spots, was cancelled or is about to start: nobody else is notified. */
    void close(UUID planId);
}

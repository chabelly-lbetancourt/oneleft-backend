package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: someone has left a plan (HU-023). The organizer is told who left and who came in from the waiting
 * list; the person who came in ({@code promotedId}) is told that they now have a spot. A plan that reopens shows up
 * again in the nearby searches. The activity and the start of the plan let the social feed show who came in (HU-051).
 */
public record PlanLeftEvent(UUID planId, UUID organizerId, String title, Activity activity, Instant startsAt,
                            UUID participantId, String participantName, UUID promotedId, String promotedName,
                            int freeSpots, boolean full, Instant occurredAt) {
}

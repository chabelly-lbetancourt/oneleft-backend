package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event: a plan has been cancelled, either because the minimum of participants was not reached by the deadline
 * (HU-039) or because its organizer cancelled it. Everyone in {@code recipientIds} (the participants and the waiting
 * list, and the organizer too when it was not their decision) is told in the app and with a notification of the
 * browser. The reputation service reads {@code reason}: only a cancellation by the organizer can cost points.
 */
public record PlanCancelled(UUID planId, UUID organizerId, String title, String placeName, Instant startsAt,
                            Reason reason, List<UUID> recipientIds, Instant occurredAt) {

    public enum Reason { MINIMUM_NOT_REACHED, ORGANIZER }

    public PlanCancelled {
        recipientIds = List.copyOf(recipientIds);
    }
}

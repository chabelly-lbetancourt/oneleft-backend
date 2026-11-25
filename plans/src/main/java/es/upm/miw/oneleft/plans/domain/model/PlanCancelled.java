package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event: a plan has been cancelled, either because the minimum of participants was not reached by the deadline
 * (HU-039), because its organizer cancelled it or because its organizer deleted their account (HU-045). Everyone in
 * {@code recipientIds} (the participants and the waiting list, and the organizer too when the minimum was not reached)
 * is told in the app and with a notification of the browser. The reputation service reads {@code reason}: only a
 * cancellation by the organizer ({@code ORGANIZER}) can cost points, never the one of a deleted account.
 */
public record PlanCancelled(UUID planId, UUID organizerId, String title, String placeName, Instant startsAt,
                            Reason reason, List<UUID> recipientIds, Instant occurredAt) {

    public enum Reason { MINIMUM_NOT_REACHED, ORGANIZER, ACCOUNT_DELETED }

    public PlanCancelled {
        recipientIds = List.copyOf(recipientIds);
    }
}

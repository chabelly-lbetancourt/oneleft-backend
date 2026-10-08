package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event: a plan has been cancelled (HU-039, the minimum of participants was not reached by the deadline). The
 * organizer, the participants and the waiting list ({@code recipientIds}) are told in the app and with a notification
 * of the browser.
 */
public record PlanCancelled(UUID planId, String title, String placeName, Instant startsAt, Reason reason,
                            List<UUID> recipientIds, Instant occurredAt) {

    public enum Reason { MINIMUM_NOT_REACHED }

    public PlanCancelled {
        recipientIds = List.copyOf(recipientIds);
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event: a plan is about to start (HU-007). The organizer and the participants ({@code recipientIds}) are
 * reminded in the app and with a notification of the browser.
 */
public record PlanReminder(UUID planId, String title, String placeName, Instant startsAt, List<UUID> recipientIds,
                           Instant occurredAt) {

    public PlanReminder {
        recipientIds = List.copyOf(recipientIds);
    }
}

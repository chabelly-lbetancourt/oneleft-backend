package es.upm.miw.oneleft.notifications.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A plan is about to start (HU-007): the plans service tells who is in it, so that everyone gets a reminder.
 */
public record PlanReminder(UUID planId, String title, String placeName, Instant startsAt, List<UUID> recipientIds) {

    public PlanReminder {
        recipientIds = recipientIds == null ? List.of() : List.copyOf(recipientIds);
    }
}

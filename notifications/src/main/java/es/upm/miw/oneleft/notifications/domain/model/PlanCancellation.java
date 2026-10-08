package es.upm.miw.oneleft.notifications.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A plan has been cancelled because it did not reach its minimum of participants (HU-039): the plans service tells
 * who was in it or waiting for it, so that everyone finds out.
 */
public record PlanCancellation(UUID planId, String title, String placeName, Instant startsAt,
                               List<UUID> recipientIds) {

    public PlanCancellation {
        recipientIds = recipientIds == null ? List.of() : List.copyOf(recipientIds);
    }
}

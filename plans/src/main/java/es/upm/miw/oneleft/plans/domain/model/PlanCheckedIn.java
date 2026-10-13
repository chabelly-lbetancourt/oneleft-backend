package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event: someone arrived at the meeting point (HU-025). The rest of the group is told in real time, and the
 * reputation (HU-009) will count it as attendance.
 *
 * @param recipientIds the rest of the group: the organizer and the participants except whoever checked in
 */
public record PlanCheckedIn(UUID planId, String title, UUID userId, String name, List<UUID> recipientIds,
                            Instant occurredAt) {

    public PlanCheckedIn {
        recipientIds = List.copyOf(recipientIds);
    }
}

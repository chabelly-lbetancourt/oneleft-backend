package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event: someone of the group says how they are getting to the plan (HU-040). The rest of the group
 * ({@code recipientIds}: organizer and participants, not the person) sees it in real time.
 */
public record PlanArrival(UUID planId, String title, UUID userId, String name, ArrivalStatus status,
                          Integer minutesLate, List<UUID> recipientIds, Instant occurredAt) {

    public PlanArrival {
        recipientIds = List.copyOf(recipientIds);
    }
}

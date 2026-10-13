package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The check-in of a plan closed: its activity, who of the group arrived and who did not, and whether every spot was
 * taken.
 */
public record AttendanceFact(UUID planId, String title, String activity, UUID organizerId, List<UUID> attendeeIds,
                             List<UUID> absenteeIds, boolean full, Instant occurredAt) {

    public AttendanceFact {
        attendeeIds = attendeeIds == null ? List.of() : List.copyOf(attendeeIds);
        absenteeIds = absenteeIds == null ? List.of() : List.copyOf(absenteeIds);
    }

    /** Whether the plan was held: someone of the group checked in */
    public boolean held() {
        return !attendeeIds.isEmpty();
    }
}

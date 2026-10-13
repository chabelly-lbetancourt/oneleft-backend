package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event: the check-in of a plan has closed (HU-025), and with it who arrived and who did not. The reputation
 * (HU-009) counts the attendance, the no-shows and whether the organizer filled and held the plan, and the badges
 * (HU-044) the plans held and their activities.
 *
 * @param attendeeIds the people of the group (organizer included) who checked in
 * @param absenteeIds the people of the group who did not
 * @param full        whether every spot was taken when the plan started
 */
public record PlanAttendanceClosed(UUID planId, String title, Activity activity, UUID organizerId,
                                   List<UUID> attendeeIds, List<UUID> absenteeIds, boolean full, Instant startsAt,
                                   Instant occurredAt) {

    public PlanAttendanceClosed {
        attendeeIds = List.copyOf(attendeeIds);
        absenteeIds = List.copyOf(absenteeIds);
    }
}

package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Someone in a plan whose check-in closed: its activity, whether they organized it, whether they arrived and whether
 * the plan was held at all. The track record of each person, for the badges (HU-044).
 */
public record Attendance(UUID userId, UUID planId, String activity, boolean organizer, boolean arrived, boolean held,
                         Instant closedAt) {
}

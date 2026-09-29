package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Someone has left a plan (HU-023): the plan as it is now, who left and, if somebody was waiting, who took the spot.
 *
 * @param promoted the first person of the waiting list, now a participant; {@code null} if nobody was waiting
 */
public record PlanLeft(Plan plan, Participant leaving, Participant promoted, Instant occurredAt) {

    /** Event for the organizer and for whoever took the spot. */
    public PlanLeftEvent event() {
        return new PlanLeftEvent(plan.id(), plan.organizer().id(), plan.title(), leaving.userId(), leaving.name(),
                promoted == null ? null : promoted.userId(), promoted == null ? null : promoted.name(),
                plan.freeSpots(), plan.status() == PlanStatus.FULL, occurredAt);
    }
}

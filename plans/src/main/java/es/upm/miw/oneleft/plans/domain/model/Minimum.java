package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;

/**
 * Minimum of participants of a plan (HU-039): if fewer people than {@code participants} have joined by the
 * {@code deadline}, the plan is cancelled; otherwise it is confirmed ({@code confirmedAt}) and goes ahead even if
 * someone leaves later. The organizer is not counted, like in the spots.
 */
public record Minimum(int participants, Instant deadline, Instant confirmedAt) {

    public Minimum {
        if (deadline == null) {
            throw new ValidationException("plan.minimumDeadline", "The minimum needs a deadline");
        }
    }

    /** Waiting for the deadline. */
    public boolean pending() {
        return confirmedAt == null;
    }

    Minimum confirm(Instant now) {
        return new Minimum(participants, deadline, now);
    }
}

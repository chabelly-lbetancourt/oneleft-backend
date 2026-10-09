package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * «On my way» or «running late (N min)» of someone of the group of a plan (HU-040). It disappears when the plan
 * starts.
 */
public record Arrival(UUID userId, String name, ArrivalStatus status, Integer minutesLate, Instant at) {

    /** Delays a person can announce, in minutes. */
    public static final Set<Integer> LATE_OPTIONS = Set.of(5, 10, 15, 30);

    public Arrival {
        if (userId == null || status == null || at == null) {
            throw new ValidationException("arrival.missingData", "Required arrival data is missing");
        }
        if (status == ArrivalStatus.LATE && (minutesLate == null || !LATE_OPTIONS.contains(minutesLate))) {
            throw new ValidationException("arrival.minutes", "Running late by 5, 10, 15 or 30 minutes");
        }
        if (status == ArrivalStatus.ON_THE_WAY) {
            minutesLate = null;
        }
    }
}

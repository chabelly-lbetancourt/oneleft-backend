package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Someone who has taken a spot in a plan. */
public record Participant(UUID userId, String name, Instant joinedAt) {

    public Participant {
        if (userId == null || joinedAt == null) {
            throw new ValidationException("participant.missingData", "The participant needs a user and a join time");
        }
        name = name == null || name.isBlank() ? "Participant" : name.strip();
    }
}

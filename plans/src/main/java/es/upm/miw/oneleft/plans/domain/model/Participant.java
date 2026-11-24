package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Someone who has taken a spot in a plan, or waits for one.
 *
 * @param priority on a waiting list, what the reputation of the person gave them when they joined it (HU-063): the
 *                 higher goes first. 0 for everyone else
 */
public record Participant(UUID userId, String name, Instant joinedAt, int priority) {

    public Participant(UUID userId, String name, Instant joinedAt) {
        this(userId, name, joinedAt, 0);
    }

    public Participant {
        if (userId == null || joinedAt == null) {
            throw new ValidationException("participant.missingData", "The participant needs a user and a join time");
        }
        name = name == null || name.isBlank() ? "Participant" : name.strip();
    }
}

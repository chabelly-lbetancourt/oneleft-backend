package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Instant;

/** A badge of someone: how far they are, and when they earned it (null while not earned). */
public record BadgeProgress(Badge badge, int progress, int goal, Instant earnedAt) {

    public boolean earned() {
        return earnedAt != null;
    }
}

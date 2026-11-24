package es.upm.miw.oneleft.reputation.domain.model;

import java.util.Optional;
import java.util.UUID;

/** The score of someone: the sum of their entries, 0 without any. */
public record Reputation(UUID userId, int score) {

    /** The level the score reaches (HU-062) */
    public Level level() {
        return Level.of(score);
    }

    /** The score at which the next level starts; none at the last level */
    public Optional<Integer> nextLevelAt() {
        return level().next().map(Level::minScore);
    }
}

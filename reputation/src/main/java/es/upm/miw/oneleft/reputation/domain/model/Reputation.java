package es.upm.miw.oneleft.reputation.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * The score of someone: the sum of their entries, 0 without any.
 *
 * @param organizedHeld the plans they organized that were held, for the seal of trusted organizer (HU-063)
 */
public record Reputation(UUID userId, int score, int organizedHeld) {

    public Reputation(UUID userId, int score) {
        this(userId, score, 0);
    }

    /** The level the score reaches (HU-062) */
    public Level level() {
        return Level.of(score);
    }

    /** The score at which the next level starts; none at the last level */
    public Optional<Integer> nextLevelAt() {
        return level().next().map(Level::minScore);
    }

    /** What the level is worth (HU-063) */
    public Perks perks() {
        return Perks.of(level());
    }

    /** Whether they have the seal «Organizador de confianza» (HU-063) */
    public boolean trustedOrganizer() {
        return Perks.trustedOrganizer(level(), organizedHeld);
    }
}

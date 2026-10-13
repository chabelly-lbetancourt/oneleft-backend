package es.upm.miw.oneleft.reputation.domain.model;

import java.util.UUID;

/** The score of someone: the sum of their entries, 0 without any. */
public record Reputation(UUID userId, int score) {
}

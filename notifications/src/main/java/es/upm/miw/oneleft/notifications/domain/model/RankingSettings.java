package es.upm.miw.oneleft.notifications.domain.model;

import java.time.Duration;

/**
 * How the people of a plan are notified (HU-012): the best first, {@code batchSize} at a time, a new batch every
 * {@code interval} while the plan has free spots and is not about to start ({@code cutoff} before the start).
 *
 * @param byModel {@code true} orders by the acceptance model, {@code false} by distance (the rule it is compared with)
 */
public record RankingSettings(boolean byModel, int batchSize, Duration interval, Duration cutoff) {

    public RankingSettings {
        if (batchSize < 1) {
            throw new IllegalArgumentException("A batch has at least one person");
        }
    }

    /** What notices did before HU-012: everyone at once, nearest first. */
    public static RankingSettings everyoneAtOnce() {
        return new RankingSettings(false, Integer.MAX_VALUE, Duration.ofMinutes(3), Duration.ofMinutes(15));
    }
}

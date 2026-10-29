package es.upm.miw.oneleft.notifications.infrastructure.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * How notices are ranked and batched (HU-012), from {@code oneleft.notices.ranking.*}.
 *
 * @param strategy  {@code model} orders by the acceptance model, {@code distance} by distance (the rule it is compared with)
 * @param url       where the ranker service is; without it the nearest go first
 * @param batchSize people notified at once
 * @param interval  time between batches
 * @param cutoff    no batch is sent in the last minutes before the plan starts
 */
@ConfigurationProperties("oneleft.notices.ranking")
public record RankingProperties(String strategy, String url, int batchSize, Duration interval, Duration cutoff,
                                boolean schedulerEnabled) {

    public RankingProperties {
        strategy = strategy == null || strategy.isBlank() ? "model" : strategy;
        batchSize = batchSize < 1 ? 5 : batchSize;
        interval = interval == null ? Duration.ofMinutes(3) : interval;
        cutoff = cutoff == null ? Duration.ofMinutes(15) : cutoff;
    }

    public boolean byModel() {
        return "model".equalsIgnoreCase(strategy);
    }
}

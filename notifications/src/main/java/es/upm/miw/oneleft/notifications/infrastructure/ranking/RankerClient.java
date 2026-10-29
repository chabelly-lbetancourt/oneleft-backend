package es.upm.miw.oneleft.notifications.infrastructure.ranking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.upm.miw.oneleft.notifications.domain.model.Candidate;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.port.out.AcceptanceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter: asks the ranker service (Python, scikit-learn) how likely each candidate is to take a spot (HU-012).
 * Any failure gives an empty answer, so notices never depend on it: the nearest go first.
 */
@Component
class RankerClient implements AcceptanceModel {

    private static final Logger log = LoggerFactory.getLogger(RankerClient.class);

    private final RestClient client;
    private final RankingProperties properties;

    RankerClient(RestClient rankerClient, RankingProperties properties) {
        this.client = rankerClient;
        this.properties = properties;
    }

    record Features(String id, double distanceKm, double distanceRatio, long minutesToStart, int localHour,
                    int weekday, int explicitInterest, int viaAlert, double pastRate, long pastSent, long sentToday,
                    int freeSpots) {
    }

    record Request(List<Features> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Score(String id, double probability) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(String modelVersion, List<Score> scores) {
    }

    @Override
    public Optional<Map<UUID, Double>> score(PublishedPlan plan, List<Candidate> candidates, ZonedDateTime now) {
        if (properties.url() == null || properties.url().isBlank()) {
            return Optional.empty();
        }
        try {
            var response = client.post().uri("/v1/score").body(new Request(
                    candidates.stream().map(c -> features(plan, c, now)).toList())).retrieve().body(Response.class);
            if (response == null || response.scores() == null) {
                return Optional.empty();
            }
            var scores = new HashMap<UUID, Double>();
            response.scores().forEach(s -> scores.put(UUID.fromString(s.id()), s.probability()));
            return Optional.of(scores);
        } catch (RuntimeException e) {
            log.warn("The ranker did not answer: {}", e.toString());
            return Optional.empty();
        }
    }

    static Features features(PublishedPlan plan, Candidate c, ZonedDateTime now) {
        var minutes = Math.max(0, Duration.between(now.toInstant(), plan.startsAt()).toMinutes());
        return new Features(c.userId().toString(), c.distanceMeters() / 1000, c.distanceRatio(), minutes,
                now.getHour(), now.getDayOfWeek().getValue() - 1, c.explicitInterest() ? 1 : 0, 0,
                c.history().rate(), c.history().sent(), c.history().sentToday(), plan.freeSpots());
    }
}

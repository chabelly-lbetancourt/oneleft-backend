package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.Candidate;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Predicts how likely each candidate is to take a spot in a plan (HU-012).
 */
public interface AcceptanceModel {

    /** @return the probability of each candidate, or empty if the model is not available (then distance decides) */
    Optional<Map<UUID, Double>> score(PublishedPlan plan, List<Candidate> candidates, ZonedDateTime now);
}

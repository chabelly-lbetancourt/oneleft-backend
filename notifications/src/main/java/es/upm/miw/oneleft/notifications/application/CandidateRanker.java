package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.Candidate;
import es.upm.miw.oneleft.notifications.domain.model.MemberPerks;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.RankedCandidate;
import es.upm.miw.oneleft.notifications.domain.model.RankingSettings;
import es.upm.miw.oneleft.notifications.domain.port.out.AcceptanceModel;
import es.upm.miw.oneleft.notifications.domain.port.out.PerkCopies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orders the people to notify of a plan, the likeliest to take a spot first (HU-012). With the distance rule, or when
 * the model is not available, the nearest go first, which is what notices did before. The probability of the model is
 * multiplied by the weight that the reputation of each person gives them (HU-063), so among people about as likely the
 * most reliable hear first; the distance rule, kept to compare with, has no weights.
 */
@Component
public class CandidateRanker {

    private static final Logger log = LoggerFactory.getLogger(CandidateRanker.class);

    public static final String MODEL = "model";
    public static final String DISTANCE = "distance";
    public static final String FALLBACK = "fallback";

    /** The people in order, and how they were ordered ({@link #MODEL}, {@link #DISTANCE} or {@link #FALLBACK}). */
    public record Ranking(String how, List<RankedCandidate> ordered) {
    }

    private final AcceptanceModel model;
    private final RankingSettings settings;
    private final PerkCopies perks;

    public CandidateRanker(AcceptanceModel model, RankingSettings settings) {
        this(model, settings, PerkCopies.NONE);
    }

    @Autowired
    public CandidateRanker(AcceptanceModel model, RankingSettings settings, PerkCopies perks) {
        this.model = model;
        this.settings = settings;
        this.perks = perks;
    }

    public Ranking rank(PublishedPlan plan, List<Candidate> candidates, ZonedDateTime now) {
        if (settings.byModel() && !candidates.isEmpty()) {
            var scores = model.score(plan, candidates, now);
            if (scores.isPresent()) {
                var weights = weightsOf(candidates);
                var ordered = candidates.stream()
                        .map(c -> new RankedCandidate(c, scores.get().getOrDefault(c.userId(), 0.0)
                                * weights.getOrDefault(c.userId(), MemberPerks.DEFAULT_WEIGHT)))
                        .sorted(Comparator.comparingDouble(RankedCandidate::score).reversed()
                                .thenComparingDouble(r -> r.candidate().distanceMeters()))
                        .toList();
                return new Ranking(MODEL, ordered);
            }
            log.warn("Plan {}: the acceptance model is not available, the nearest people go first", plan.planId());
            return new Ranking(FALLBACK, byDistance(candidates));
        }
        return new Ranking(DISTANCE, byDistance(candidates));
    }

    /** The weight of each candidate whose perks are known */
    private Map<UUID, Double> weightsOf(List<Candidate> candidates) {
        return perks.findByUsers(candidates.stream().map(Candidate::userId).toList()).values().stream()
                .collect(Collectors.toMap(MemberPerks::userId, MemberPerks::noticeWeight));
    }

    private static List<RankedCandidate> byDistance(List<Candidate> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(Candidate::distanceMeters))
                .map(c -> new RankedCandidate(c, 1 / (1 + c.distanceMeters() / 1000)))
                .toList();
    }
}

package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.AlertLimitException;
import es.upm.miw.oneleft.notifications.domain.model.Candidate;
import es.upm.miw.oneleft.notifications.domain.model.MemberPerks;
import es.upm.miw.oneleft.notifications.domain.model.NoticeHistory;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.RankedCandidate;
import es.upm.miw.oneleft.notifications.domain.model.RankingSettings;
import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;
import es.upm.miw.oneleft.notifications.domain.port.in.ManageAlertsUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.PerkCopies;
import es.upm.miw.oneleft.notifications.infrastructure.messaging.PerksListener;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-063: the reputation of someone weighs in the order of the notices and lets them keep more alerts. */
class PerksTest {

    private static final Instant NOW = Instant.parse("2026-11-24T16:00:00Z");
    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();

    /** The copy in memory, with the rule of the SQL: an older event never replaces a newer one */
    static final class Copies implements PerkCopies {
        final Map<UUID, MemberPerks> all = new HashMap<>();

        @Override
        public Map<UUID, MemberPerks> findByUsers(Collection<UUID> userIds) {
            return all.values().stream().filter(perks -> userIds.contains(perks.userId()))
                    .collect(Collectors.toMap(MemberPerks::userId, perks -> perks));
        }

        @Override
        public void save(MemberPerks perks) {
            all.merge(perks.userId(), perks,
                    (kept, changed) -> changed.occurredAt().isBefore(kept.occurredAt()) ? kept : changed);
        }
    }

    private final Copies copies = new Copies();
    private final PerkCopyService copy = new PerkCopyService(copies);
    private final PerksListener listener = new PerksListener(copy);

    private void level(UUID userId, String level, double weight, int alerts) {
        listener.onChanged(new PerksListener.PerksChanged(userId, level, weight, alerts, NOW));
    }

    private static Candidate candidate(UUID userId, double meters) {
        return new Candidate(userId, meters, 3_000, false, NoticeHistory.NONE);
    }

    private List<UUID> ranked(RankingSettings settings, Optional<Map<UUID, Double>> probabilities) {
        var plan = new PublishedPlan(UUID.randomUUID(), UUID.randomUUID(), Activity.PADEL, "Pádel", "Pistas", 40.39,
                -3.63, NOW.plus(Duration.ofHours(2)), 1);
        var ranker = new CandidateRanker((published, candidates, at) -> probabilities, settings, copies);
        var people = List.of(candidate(LUCIA, 2_000), candidate(DIEGO, 1_000), candidate(MARTA, 500));
        return ranker.rank(plan, people, ZonedDateTime.ofInstant(NOW, ZoneId.of("Europe/Madrid"))).ordered().stream()
                .map(RankedCandidate::candidate).map(Candidate::userId).toList();
    }

    private static RankingSettings byModel() {
        return new RankingSettings(true, 5, Duration.ofMinutes(3), Duration.ofMinutes(15));
    }

    @Test
    void amongPeopleAboutAsLikelyTheOneWithMoreReputationHearsFirst() {
        var probabilities = Optional.of(Map.of(LUCIA, 0.50, DIEGO, 0.55, MARTA, 0.20));
        assertThat(ranked(byModel(), probabilities)).containsExactly(DIEGO, LUCIA, MARTA);

        level(LUCIA, "NEIGHBOURHOOD_REFERENCE", 1.25, 8);

        // 0.50 × 1.25 = 0.625 goes ahead of 0.55; someone far less likely stays behind whatever their level
        assertThat(ranked(byModel(), probabilities)).containsExactly(LUCIA, DIEGO, MARTA);
        level(MARTA, "LEGEND", 1.5, 10);
        assertThat(ranked(byModel(), probabilities)).containsExactly(LUCIA, DIEGO, MARTA);
    }

    @Test
    void theDistanceRuleAndTheFallbackHaveNoWeights() {
        level(LUCIA, "LEGEND", 1.5, 10);

        assertThat(ranked(RankingSettings.everyoneAtOnce(), Optional.empty())).containsExactly(MARTA, DIEGO, LUCIA);
        assertThat(ranked(byModel(), Optional.empty())).containsExactly(MARTA, DIEGO, LUCIA);
    }

    @Test
    void anOlderEventNeverReplacesANewerOne() {
        level(LUCIA, "REGULAR", 1.1, 6);
        listener.onChanged(new PerksListener.PerksChanged(LUCIA, "NEW", 1.0, 5, NOW.minusSeconds(60)));

        assertThat(copies.all.get(LUCIA).level()).isEqualTo("REGULAR");

        listener.onChanged(new PerksListener.PerksChanged(LUCIA, "NEW", 1.0, 5, NOW.plusSeconds(60)));
        assertThat(copies.all.get(LUCIA).level()).isEqualTo("NEW");
    }

    private static SavedAlert alert(String name) {
        return new SavedAlert(UUID.randomUUID(), DIEGO, name, Set.of(), null, 40.39, -3.63, 2_000, Set.of(), null,
                null);
    }

    @Test
    void whoHasTheReputationKeepsMoreAlertsAndTheRestTheUsualOnes() {
        var service = new AlertService(new NearbyPlanNotifierTest.Alerts(), copies);
        level(LUCIA, "REGULAR", 1.1, ManageAlertsUseCase.MAX_ALERTS + 1);
        for (var index = 0; index < ManageAlertsUseCase.MAX_ALERTS; index++) {
            service.create(LUCIA, alert("Alerta " + index));
            service.create(DIEGO, alert("Alerta " + index));
        }

        service.create(LUCIA, alert("La de su nivel"));

        assertThat(service.alertsOf(LUCIA)).hasSize(ManageAlertsUseCase.MAX_ALERTS + 1);
        assertThatThrownBy(() -> service.create(LUCIA, alert("Una más")))
                .isInstanceOf(AlertLimitException.class).hasMessageContaining("up to 6 alerts");
        assertThatThrownBy(() -> service.create(DIEGO, alert("Una más")))
                .isInstanceOf(AlertLimitException.class).hasMessageContaining("up to 5 alerts");
    }

    @Test
    void aCopyBelowTheLimitOfEveryoneTakesNothingAway() {
        var service = new AlertService(new NearbyPlanNotifierTest.Alerts(), copies);
        level(LUCIA, "NEW", 1.0, 2);

        for (var index = 0; index < ManageAlertsUseCase.MAX_ALERTS; index++) {
            service.create(LUCIA, alert("Alerta " + index));
        }

        assertThat(service.alertsOf(LUCIA)).hasSize(ManageAlertsUseCase.MAX_ALERTS);
    }

    @Test
    void withoutACopyNobodyHasPerks() {
        assertThat(PerkCopies.NONE.findByUsers(List.of(LUCIA))).isEmpty();
        PerkCopies.NONE.save(new MemberPerks(LUCIA, "LEGEND", 1.5, 10, NOW));
        assertThat(PerkCopies.NONE.findByUsers(List.of(LUCIA))).isEmpty();
    }
}

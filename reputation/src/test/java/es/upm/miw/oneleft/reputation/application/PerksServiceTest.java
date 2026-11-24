package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.InMemoryReputation;
import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.CheckInFact;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
import es.upm.miw.oneleft.reputation.domain.model.Level;
import es.upm.miw.oneleft.reputation.domain.model.Perks;
import es.upm.miw.oneleft.reputation.domain.model.ReportFact;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-063: the other services are told what a level is worth when it changes, and the seal of trusted organizer. */
class PerksServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T16:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private record Told(UUID userId, Perks perks, Instant at) {
    }

    private final InMemoryReputation store = new InMemoryReputation();
    private final List<Told> told = new ArrayList<>();
    private final ReputationService service = new ReputationService(store, store, store, store,
            (userId, badge, at) -> { }, (userId, perks, at) -> told.add(new Told(userId, perks, at)), CLOCK);
    private final KudosService kudos = new KudosService(store, store, store, store, (given, giverName) -> { }, store,
            (userId, badge, at) -> { }, (userId, perks, at) -> told.add(new Told(userId, perks, at)), CLOCK);
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    private void checkIn(UUID userId) {
        service.record(new CheckInFact(UUID.randomUUID(), "Padel", userId, NOW));
    }

    private void organizedHeld(UUID organizer, int plans) {
        store.record(IntStream.range(0, plans)
                .mapToObj(plan -> new Attendance(organizer, UUID.randomUUID(), "PADEL", true, true, true, NOW))
                .toList());
    }

    @Test
    void pointsThatLeaveTheLevelAsItWasTellNothing() {
        checkIn(lucia);

        assertThat(service.reputationOf(lucia).level()).isEqualTo(Level.NEW);
        assertThat(told).isEmpty();
    }

    @Test
    void reachingALevelTellsWhatItIsWorthOnce() {
        store.add(lucia, 90);

        checkIn(lucia);
        checkIn(lucia);

        assertThat(service.reputationOf(lucia).level()).isEqualTo(Level.REGULAR);
        assertThat(told).containsExactly(new Told(lucia, Perks.of(Level.REGULAR), NOW));
    }

    @Test
    void losingALevelTellsTheOneThatIsLeft() {
        store.add(lucia, 110);

        service.record(new ReportFact(UUID.randomUUID(), lucia, NOW));

        assertThat(service.reputationOf(lucia).level()).isEqualTo(Level.NEW);
        assertThat(told).containsExactly(new Told(lucia, Perks.of(Level.NEW), NOW));
    }

    @Test
    void aKudosThatReachesALevelTellsItToo() {
        var plan = UUID.randomUUID();
        store.record(List.of(new Attendance(ana, plan, "PADEL", true, true, true, NOW.minusSeconds(3600)),
                new Attendance(lucia, plan, "PADEL", false, true, true, NOW.minusSeconds(3600))));
        store.add(lucia, 499);

        kudos.give(ana, "Ana", plan, lucia, KudosKind.PUNCTUAL);

        assertThat(told).containsExactly(new Told(lucia, Perks.of(Level.NEIGHBOURHOOD_REFERENCE), NOW));
    }

    @Test
    void rebuildingTheScoresTellsThePerksOfEveryone() {
        checkIn(lucia);
        store.scores.put(lucia, 2000);

        service.rebuildScores();

        assertThat(told).containsExactly(new Told(lucia, Perks.of(Level.NEW), NOW));
    }

    @Test
    void theSealNeedsTheLevelAndFivePlansHeldAndComesWithTheReputationOfSeveralPeople() {
        store.add(ana, 150);
        store.add(lucia, 150);
        organizedHeld(ana, Perks.TRUSTED_ORGANIZER_PLANS);
        organizedHeld(lucia, Perks.TRUSTED_ORGANIZER_PLANS - 1);
        var diego = UUID.randomUUID();
        organizedHeld(diego, Perks.TRUSTED_ORGANIZER_PLANS);

        var reputations = service.reputationsOf(List.of(ana, lucia, diego));

        assertThat(reputations).extracting(reputation -> reputation.trustedOrganizer())
                .containsExactly(true, false, false);
        assertThat(service.reputationOf(ana).perks()).isEqualTo(Perks.of(Level.REGULAR));
    }
}

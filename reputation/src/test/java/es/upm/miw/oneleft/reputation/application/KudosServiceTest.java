package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.InMemoryReputation;
import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosException;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PlanKudos;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-060: kudos between the people who arrived at a plan, in the ledger, with their notice. */
class KudosServiceTest {

    private static final Instant CLOSED = Instant.parse("2026-10-18T12:00:00Z");
    private static final Instant NOW = CLOSED.plus(Duration.ofHours(2));
    private final InMemoryReputation store = new InMemoryReputation();
    private final List<String> notices = new ArrayList<>();
    private final List<String> badgeNotices = new ArrayList<>();
    private final KudosService service = new KudosService(store, store, store, store,
            (kudos, giverName) -> notices.add(giverName + ">" + kudos.receiverId() + ":" + kudos.kind()), store,
            (userId, badge, at) -> badgeNotices.add(userId + ":" + badge), Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID plan = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();

    private void attended(UUID userId, boolean organizer, boolean arrived) {
        store.record(List.of(new Attendance(userId, plan, "PADEL", organizer, arrived, true, CLOSED)));
    }

    @BeforeEach
    void plan() {
        attended(ana, true, true);
        attended(lucia, false, true);
        attended(diego, false, false);
        store.append(LedgerEntry.of(ana, Reason.ATTENDED, plan, "Padel 2 vs 2", CLOSED));
    }

    @Test
    void aKudosAddsFiveToWhoeverReceivesItAndIsToldToThem() {
        var kudos = service.give(lucia, "Lucía", plan, ana, KudosKind.GREAT_ORGANIZER);

        assertThat(kudos).isEqualTo(new Kudos(lucia, ana, plan, KudosKind.GREAT_ORGANIZER, NOW));
        assertThat(store.scores).containsEntry(ana, 5);
        assertThat(store.entries).contains(new LedgerEntry(ana, Reason.KUDOS, 5, kudos.id(), "Padel 2 vs 2", NOW));
        assertThat(notices).containsExactly("Lucía>" + ana + ":GREAT_ORGANIZER");
        assertThat(service.receivedBy(ana)).containsEntry(KudosKind.GREAT_ORGANIZER, 1L)
                .containsEntry(KudosKind.PUNCTUAL, 0L).hasSize(KudosKind.values().length);
    }

    @Test
    void oneKudosPerPersonAndPlan() {
        service.give(lucia, "Lucía", plan, ana, KudosKind.PUNCTUAL);

        assertThatThrownBy(() -> service.give(lucia, "Lucía", plan, ana, KudosKind.GOOD_VIBES))
                .isInstanceOfSatisfying(KudosException.class,
                        error -> assertThat(error.code()).isEqualTo("kudos.alreadyGiven"));
        assertThat(store.scores).containsEntry(ana, 5);
        assertThat(notices).hasSize(1);
        // The other way round is another kudos
        service.give(ana, "Ana", plan, lucia, KudosKind.GOOD_VIBES);
        assertThat(store.scores).containsEntry(lucia, 5);
    }

    @Test
    void whoeverDidNotArriveNeitherGivesNorReceivesKudos() {
        assertThatThrownBy(() -> service.give(diego, "Diego", plan, ana, KudosKind.PUNCTUAL))
                .isInstanceOf(KudosException.class);
        assertThatThrownBy(() -> service.give(lucia, "Lucía", plan, diego, KudosKind.PUNCTUAL))
                .hasFieldOrPropertyWithValue("code", "kudos.receiverNotAttended");
        assertThat(store.kudos).isEmpty();
        assertThat(notices).isEmpty();
    }

    @Test
    void theKudosOfAPlanShowWhoCanStillReceiveOne() {
        service.give(lucia, "Lucía", plan, ana, KudosKind.GREAT_LEVEL);

        assertThat(service.kudosOf(lucia, plan)).isEqualTo(new PlanKudos(true, CLOSED.plus(Duration.ofDays(7)),
                List.of(new PlanKudos.Person(ana, true, KudosKind.GREAT_LEVEL))));
        assertThat(service.kudosOf(ana, plan).people())
                .containsExactly(new PlanKudos.Person(lucia, false, null));
        // Whoever did not arrive has nothing to give
        assertThat(service.kudosOf(diego, plan)).isEqualTo(new PlanKudos(false, null, List.of()));
    }

    @Test
    void fiftyKudosBringTheBadgeOfTheNeighbourhood() {
        IntStream.range(0, 50).forEach(index -> {
            var other = UUID.randomUUID();
            store.record(List.of(new Attendance(other, plan, "PADEL", false, true, true, CLOSED)));
            service.give(other, "Someone", plan, ana, KudosKind.GOOD_VIBES);
        });

        assertThat(store.earnedBy(ana)).containsKey(Badge.NEIGHBOURHOOD_SOUL);
        assertThat(badgeNotices).containsOnlyOnce(ana + ":NEIGHBOURHOOD_SOUL");
    }
}

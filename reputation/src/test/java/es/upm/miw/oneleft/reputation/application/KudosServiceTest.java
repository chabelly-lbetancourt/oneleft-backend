package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.InMemoryReputation;
import es.upm.miw.oneleft.reputation.domain.model.AntiCheatRules;
import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosException;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PlanKudos;
import es.upm.miw.oneleft.reputation.domain.model.PointsLimit;
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
import static org.assertj.core.api.Assertions.tuple;

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

    /** The service with the clock some days after {@link #NOW}, for what depends on the day (HU-066) */
    private KudosService daysLater(int days) {
        return new KudosService(store, store, store, store, (kudos, giverName) -> notices.add(giverName), store,
                (userId, badge, at) -> badgeNotices.add(userId + ":" + badge),
                Clock.fixed(NOW.plus(Duration.ofDays(days)), ZoneOffset.UTC));
    }

    private UUID someoneWhoArrived(UUID planId) {
        var other = UUID.randomUUID();
        store.record(List.of(new Attendance(other, planId, "PADEL", false, true, true, CLOSED)));
        return other;
    }

    @Test
    void fiftyKudosThatGavePointsBringTheBadgeOfTheNeighbourhood() {
        // Ten a day: the daily cap leaves the rest of a day at nothing, and those do not count for the badge
        IntStream.range(0, 49).forEach(index ->
                daysLater(index / AntiCheatRules.MAX_KUDOS_PER_DAY)
                        .give(someoneWhoArrived(plan), "Someone", plan, ana, KudosKind.GOOD_VIBES));
        service.give(someoneWhoArrived(plan), "Someone", plan, ana, KudosKind.GOOD_VIBES);
        assertThat(store.earnedBy(ana)).doesNotContainKey(Badge.NEIGHBOURHOOD_SOUL);

        daysLater(5).give(someoneWhoArrived(plan), "Someone", plan, ana, KudosKind.GOOD_VIBES);

        assertThat(store.earnedBy(ana)).containsKey(Badge.NEIGHBOURHOOD_SOUL);
        assertThat(badgeNotices).containsOnlyOnce(ana + ":NEIGHBOURHOOD_SOUL");
    }

    private UUID planTogether(UUID organizer, UUID other) {
        var planId = UUID.randomUUID();
        store.record(List.of(new Attendance(organizer, planId, "PADEL", true, true, true, CLOSED),
                new Attendance(other, planId, "PADEL", false, true, true, CLOSED)));
        return planId;
    }

    private List<LedgerEntry> kudosOf(UUID receiver) {
        return store.entries.stream().filter(entry -> entry.userId().equals(receiver)
                && entry.reason() == Reason.KUDOS).toList();
    }

    @Test
    void eachKudosFromTheSamePersonIsWorthHalfTheOneBeforeButStaysInTheLedger() {
        IntStream.range(0, 4).forEach(index -> service.give(lucia, "Lucía", planTogether(ana, lucia), ana,
                KudosKind.GOOD_VIBES));
        // Someone else still gives the five of always
        service.give(diego, "Diego", planTogether(ana, diego), ana, KudosKind.GOOD_VIBES);

        assertThat(kudosOf(ana)).extracting(LedgerEntry::points, LedgerEntry::limitedBy)
                .containsExactly(tuple(5, null), tuple(2, PointsLimit.SAME_PERSON), tuple(1, PointsLimit.SAME_PERSON),
                        tuple(0, PointsLimit.SAME_PERSON), tuple(5, null));
        assertThat(store.scores).containsEntry(ana, 13);
        // Every kudos is still told and counted by kind
        assertThat(service.receivedBy(ana)).containsEntry(KudosKind.GOOD_VIBES, 5L);
    }

    @Test
    void aPairThatKeepsRatingEachOtherGetsNothingFromItsKudos() {
        IntStream.range(0, AntiCheatRules.MUTUAL_PAIR_KUDOS).forEach(index -> {
            var together = planTogether(ana, lucia);
            service.give(lucia, "Lucía", together, ana, KudosKind.GOOD_VIBES);
            service.give(ana, "Ana", together, lucia, KudosKind.GOOD_VIBES);
        });

        assertThat(kudosOf(ana)).extracting(LedgerEntry::points).containsExactly(5, 2, 1);
        // The third one back makes them a pair
        assertThat(kudosOf(lucia)).extracting(LedgerEntry::points, LedgerEntry::limitedBy)
                .containsExactly(tuple(5, null), tuple(2, PointsLimit.SAME_PERSON), tuple(0, PointsLimit.MUTUAL_PAIR));
        var fourth = planTogether(ana, lucia);
        service.give(lucia, "Lucía", fourth, ana, KudosKind.PUNCTUAL);
        assertThat(kudosOf(ana).getLast().limitedBy()).isEqualTo(PointsLimit.MUTUAL_PAIR);
    }

    @Test
    void theKudosOfLongAgoDoNotMakeTheSamePersonNorAPair() {
        var aMonthLater = new KudosService(store, store, store, store, (kudos, giverName) -> notices.add(giverName),
                store, (userId, badge, at) -> badgeNotices.add(userId + ":" + badge),
                Clock.fixed(NOW.plus(AntiCheatRules.SAME_PERSON_WINDOW).plusSeconds(1), ZoneOffset.UTC));
        service.give(lucia, "Lucía", planTogether(ana, lucia), ana, KudosKind.GOOD_VIBES);
        var later = UUID.randomUUID();
        store.record(List.of(
                new Attendance(ana, later, "PADEL", true, true, true, NOW.plus(AntiCheatRules.SAME_PERSON_WINDOW)),
                new Attendance(lucia, later, "PADEL", false, true, true, NOW.plus(AntiCheatRules.SAME_PERSON_WINDOW))));

        aMonthLater.give(lucia, "Lucía", later, ana, KudosKind.GOOD_VIBES);

        assertThat(kudosOf(ana)).extracting(LedgerEntry::points).containsExactly(5, 5);
    }

    @Test
    void fromTheDailyCapOnAKudosGivesNothingThatDay() {
        IntStream.rangeClosed(0, AntiCheatRules.MAX_KUDOS_PER_DAY)
                .forEach(index -> service.give(someoneWhoArrived(plan), "Someone", plan, ana, KudosKind.GOOD_VIBES));

        assertThat(store.scores).containsEntry(ana, AntiCheatRules.MAX_KUDOS_PER_DAY * 5);
        assertThat(kudosOf(ana).getLast()).extracting(LedgerEntry::points, LedgerEntry::limitedBy)
                .containsExactly(0, PointsLimit.DAILY_CAP);
        // The next day they count again
        daysLater(1).give(someoneWhoArrived(plan), "Someone", plan, ana, KudosKind.GOOD_VIBES);
        assertThat(store.scores).containsEntry(ana, AntiCheatRules.MAX_KUDOS_PER_DAY * 5 + 5);
    }
}

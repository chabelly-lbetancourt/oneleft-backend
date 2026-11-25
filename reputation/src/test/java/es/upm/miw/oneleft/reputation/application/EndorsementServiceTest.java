package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.InMemoryReputation;
import es.upm.miw.oneleft.reputation.domain.model.AntiCheatRules;
import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.Endorsement;
import es.upm.miw.oneleft.reputation.domain.model.EndorsementCount;
import es.upm.miw.oneleft.reputation.domain.model.EndorsementException;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PersonEndorsements;
import es.upm.miw.oneleft.reputation.domain.model.PointsLimit;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.model.SkillLevel;
import es.upm.miw.oneleft.reputation.domain.model.ValidationException;
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

/** HU-061: endorsements of a level between people who played together, in the ledger, with the limits of HU-066. */
class EndorsementServiceTest {

    private static final Instant PLAYED = Instant.parse("2026-11-20T18:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");
    private final InMemoryReputation store = new InMemoryReputation();
    private final List<String> perkNotices = new ArrayList<>();
    private final EndorsementService service = serviceAt(NOW);
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();

    private EndorsementService serviceAt(Instant now) {
        return new EndorsementService(store, store, store, store,
                (userId, perks, at) -> perkNotices.add(userId + ":" + perks.level()),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    /** A plan of an activity that Ana organized: who of the others arrived */
    private void played(String activity, UUID other, boolean otherArrived) {
        var plan = UUID.randomUUID();
        store.record(List.of(new Attendance(ana, plan, activity, true, true, true, PLAYED),
                new Attendance(other, plan, activity, false, otherArrived, true, PLAYED)));
    }

    private List<LedgerEntry> endorsementsOf(UUID receiver) {
        return store.entries.stream().filter(entry -> entry.userId().equals(receiver)
                && entry.reason() == Reason.ENDORSEMENT).toList();
    }

    @BeforeEach
    void plans() {
        played("PADEL", lucia, true);
        played("RUNNING", lucia, true);
        played("PADEL", diego, false);
    }

    @Test
    void anEndorsementAddsEightToWhoeverReceivesIt() {
        var endorsement = service.endorse(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE);

        assertThat(endorsement).isEqualTo(new Endorsement(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE, NOW));
        assertThat(store.scores).containsEntry(ana, 8);
        assertThat(store.entries).contains(new LedgerEntry(ana, Reason.ENDORSEMENT, 8, endorsement.id(), null, NOW));
        assertThat(store.scores).doesNotContainKey(lucia);
    }

    @Test
    void oneEndorsementPerPersonAndActivityWhateverTheLevel() {
        service.endorse(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE);

        assertThatThrownBy(() -> service.endorse(lucia, ana, "PADEL", SkillLevel.ADVANCED))
                .isInstanceOfSatisfying(EndorsementException.class,
                        error -> assertThat(error.code()).isEqualTo("endorsement.alreadyGiven"));
        assertThat(store.scores).containsEntry(ana, 8);
        assertThat(store.endorsements).hasSize(1);
    }

    @Test
    void onlyWhoeverArrivedAtAPlanOfThatActivityWithThemEndorses() {
        // Diego did not arrive at the plan they shared, and Lucía never played chess with Ana
        assertThatThrownBy(() -> service.endorse(diego, ana, "PADEL", SkillLevel.INTERMEDIATE))
                .isInstanceOfSatisfying(EndorsementException.class,
                        error -> assertThat(error.code()).isEqualTo("endorsement.notPlayedTogether"));
        assertThatThrownBy(() -> service.endorse(ana, diego, "PADEL", SkillLevel.INTERMEDIATE))
                .isInstanceOf(EndorsementException.class);
        assertThatThrownBy(() -> service.endorse(lucia, ana, "CHESS", SkillLevel.INTERMEDIATE))
                .isInstanceOf(EndorsementException.class);
        // Someone who has never been in a plan with them
        assertThatThrownBy(() -> service.endorse(UUID.randomUUID(), ana, "PADEL", SkillLevel.INTERMEDIATE))
                .isInstanceOf(EndorsementException.class);
        assertThat(store.endorsements).isEmpty();
        assertThat(store.entries).isEmpty();
    }

    @Test
    void nobodyEndorsesThemselves() {
        assertThatThrownBy(() -> service.endorse(ana, ana, "PADEL", SkillLevel.ADVANCED))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("endorsement.self"));
        assertThat(store.endorsements).isEmpty();
    }

    @Test
    void eachEndorsementFromTheSamePersonIsWorthHalfTheOneBeforeButStillCounts() {
        played("CHESS", lucia, true);
        played("PADEL", diego, true);

        service.endorse(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE);
        service.endorse(lucia, ana, "RUNNING", SkillLevel.BEGINNER);
        service.endorse(lucia, ana, "CHESS", SkillLevel.BEGINNER);
        // Someone else still gives the eight of always
        service.endorse(diego, ana, "PADEL", SkillLevel.INTERMEDIATE);

        assertThat(endorsementsOf(ana)).extracting(LedgerEntry::points, LedgerEntry::limitedBy)
                .containsExactly(tuple(8, null), tuple(4, PointsLimit.SAME_PERSON), tuple(2, PointsLimit.SAME_PERSON),
                        tuple(8, null));
        assertThat(store.scores).containsEntry(ana, 22);
        assertThat(service.endorsementsOf(diego, ana).counts())
                .contains(new EndorsementCount("PADEL", SkillLevel.INTERMEDIATE, 2));
    }

    @Test
    void aPairThatKeepsEndorsingEachOtherGetsNothingFromIt() {
        service.endorse(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE);
        service.endorse(ana, lucia, "PADEL", SkillLevel.INTERMEDIATE);
        service.endorse(lucia, ana, "RUNNING", SkillLevel.BEGINNER);

        // The second one back makes them a pair
        service.endorse(ana, lucia, "RUNNING", SkillLevel.BEGINNER);

        assertThat(endorsementsOf(ana)).extracting(LedgerEntry::points).containsExactly(8, 4);
        assertThat(endorsementsOf(lucia)).extracting(LedgerEntry::points, LedgerEntry::limitedBy)
                .containsExactly(tuple(8, null), tuple(0, PointsLimit.MUTUAL_PAIR));
        // The endorsement itself is kept: it is the points that the rules cut
        assertThat(store.endorsements).hasSize(4);
    }

    @Test
    void theEndorsementsOfLongAgoDoNotMakeTheSamePerson() {
        service.endorse(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE);

        serviceAt(NOW.plus(AntiCheatRules.SAME_PERSON_WINDOW).plusSeconds(1))
                .endorse(lucia, ana, "RUNNING", SkillLevel.BEGINNER);

        assertThat(endorsementsOf(ana)).extracting(LedgerEntry::points).containsExactly(8, 8);
    }

    @Test
    void fromTheDailyCapOnAnEndorsementGivesNothingThatDay() {
        IntStream.rangeClosed(0, AntiCheatRules.MAX_ENDORSEMENTS_PER_DAY).forEach(index -> {
            var someone = UUID.randomUUID();
            played("PADEL", someone, true);
            service.endorse(someone, ana, "PADEL", SkillLevel.INTERMEDIATE);
        });

        assertThat(store.scores).containsEntry(ana, AntiCheatRules.MAX_ENDORSEMENTS_PER_DAY * 8);
        assertThat(endorsementsOf(ana).getLast()).extracting(LedgerEntry::points, LedgerEntry::limitedBy)
                .containsExactly(0, PointsLimit.DAILY_CAP);
        // The next day they count again
        var tomorrow = UUID.randomUUID();
        played("PADEL", tomorrow, true);
        serviceAt(NOW.plus(Duration.ofDays(1))).endorse(tomorrow, ana, "PADEL", SkillLevel.INTERMEDIATE);
        assertThat(store.scores).containsEntry(ana, AntiCheatRules.MAX_ENDORSEMENTS_PER_DAY * 8 + 8);
    }

    @Test
    void pointsThatReachALevelTellWhatItIsWorth() {
        store.add(ana, 95);

        service.endorse(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE);

        assertThat(perkNotices).containsExactly(ana + ":REGULAR");
    }

    @Test
    void theEndorsementsOfSomeoneShowTheCountsMineAndWhatICanStillEndorse() {
        played("PADEL", diego, true);
        service.endorse(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE);
        service.endorse(diego, ana, "PADEL", SkillLevel.ADVANCED);

        var counts = List.of(new EndorsementCount("PADEL", SkillLevel.INTERMEDIATE, 1),
                new EndorsementCount("PADEL", SkillLevel.ADVANCED, 1));
        assertThat(service.endorsementsOf(lucia, ana)).isEqualTo(new PersonEndorsements(counts,
                List.of(new Endorsement(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE, NOW)), List.of("RUNNING")));
        // For myself: the counts, and nothing to give
        assertThat(service.endorsementsOf(ana, ana)).isEqualTo(new PersonEndorsements(counts, List.of(), List.of()));
        // Someone who never played with them sees the counts and can endorse nothing
        assertThat(service.endorsementsOf(UUID.randomUUID(), ana))
                .isEqualTo(new PersonEndorsements(counts, List.of(), List.of()));
    }
}

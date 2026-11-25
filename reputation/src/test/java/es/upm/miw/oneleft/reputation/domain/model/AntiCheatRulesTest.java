package es.upm.miw.oneleft.reputation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** HU-066: the daily caps, the kudos from the same person and the pairs that keep rating each other. */
class AntiCheatRulesTest {

    private static final Instant NOW = Instant.parse("2026-10-13T16:30:00Z");
    private final UUID lucia = UUID.randomUUID();

    private LedgerEntry entry(Reason reason) {
        return LedgerEntry.of(lucia, reason, UUID.randomUUID(), "Padel", NOW);
    }

    @Test
    void underTheDailyCapAnEntryGivesItsPointsAndFromTheCapOnNothing() {
        var attended = entry(Reason.ATTENDED);

        assertThat(AntiCheatRules.dailyCapped(attended, AntiCheatRules.MAX_ATTENDED_PER_DAY - 1)).isEqualTo(attended);
        assertThat(AntiCheatRules.dailyCapped(attended, AntiCheatRules.MAX_ATTENDED_PER_DAY))
                .extracting(LedgerEntry::points, LedgerEntry::limitedBy, LedgerEntry::sourceId)
                .containsExactly(0, PointsLimit.DAILY_CAP, attended.sourceId());
        assertThat(AntiCheatRules.dailyCapped(entry(Reason.ORGANIZED_HELD), AntiCheatRules.MAX_ORGANIZED_PER_DAY)
                .points()).isZero();
        assertThat(AntiCheatRules.dailyCapped(entry(Reason.KUDOS), AntiCheatRules.MAX_KUDOS_PER_DAY).points())
                .isZero();
    }

    @Test
    void whatTakesPointsAwayIsNeverCapped() {
        IntStream.of(0, 100).forEach(earlier -> {
            assertThat(AntiCheatRules.dailyCapped(entry(Reason.NO_SHOW), earlier).points()).isEqualTo(-15);
            assertThat(AntiCheatRules.dailyCapped(entry(Reason.CONFIRMED_REPORT), earlier).points()).isEqualTo(-50);
            assertThat(AntiCheatRules.dailyCapped(entry(Reason.LATE_CANCELLATION), earlier).limitedBy()).isNull();
        });
        assertThat(AntiCheatRules.capped(entry(Reason.NO_SHOW))).isFalse();
        assertThat(AntiCheatRules.capped(entry(Reason.ATTENDED))).isTrue();
        assertThat(AntiCheatRules.dailyCap(Reason.CONFIRMED_REPORT)).isEmpty();
        assertThat(AntiCheatRules.dailyCap(Reason.KUDOS)).contains(AntiCheatRules.MAX_KUDOS_PER_DAY);
    }

    @Test
    void aDayStartsAtMidnightInSpain() {
        // 00:30 of 14 October in Madrid (summer time, UTC+2)
        var afterMidnight = Instant.parse("2026-10-13T22:30:00Z");

        assertThat(AntiCheatRules.startOfDay(afterMidnight)).isEqualTo(Instant.parse("2026-10-13T22:00:00Z"));
        assertThat(AntiCheatRules.endOfDay(afterMidnight)).isEqualTo(Instant.parse("2026-10-14T22:00:00Z"));
        // Winter time (UTC+1) after the last Sunday of October
        assertThat(AntiCheatRules.startOfDay(Instant.parse("2026-11-02T10:00:00Z")))
                .isEqualTo(Instant.parse("2026-11-01T23:00:00Z"));
    }

    @Test
    void eachKudosFromTheSamePersonIsWorthHalfTheOneBefore() {
        var kudos = entry(Reason.KUDOS);

        assertThat(IntStream.rangeClosed(1, 5).mapToObj(given -> AntiCheatRules.kudos(kudos, given, 0)))
                .extracting(LedgerEntry::points, LedgerEntry::limitedBy)
                .containsExactly(tuple(5, null), tuple(2, PointsLimit.SAME_PERSON), tuple(1, PointsLimit.SAME_PERSON),
                        tuple(0, PointsLimit.SAME_PERSON), tuple(0, PointsLimit.SAME_PERSON));
        assertThat(AntiCheatRules.kudos(kudos, 1000, 0).points()).isZero();
    }

    @Test
    void aPairThatKeepsRatingEachOtherGetsNothingFromItsKudos() {
        var kudos = entry(Reason.KUDOS);
        var pair = AntiCheatRules.MUTUAL_PAIR_KUDOS;

        assertThat(AntiCheatRules.kudos(kudos, pair, pair))
                .extracting(LedgerEntry::points, LedgerEntry::limitedBy).containsExactly(0, PointsLimit.MUTUAL_PAIR);
        // Only one way round is not a pair yet
        assertThat(AntiCheatRules.kudos(kudos, pair, pair - 1).limitedBy()).isEqualTo(PointsLimit.SAME_PERSON);
        assertThat(AntiCheatRules.kudos(kudos, 1, pair + 5).points()).isEqualTo(5);
    }

    @Test
    void theEndorsementsOfALevelFollowTheSameRulesBetweenTwoPeople() {
        var endorsement = entry(Reason.ENDORSEMENT);
        var pair = AntiCheatRules.MUTUAL_PAIR_ENDORSEMENTS;

        assertThat(IntStream.rangeClosed(1, 5).mapToObj(given -> AntiCheatRules.endorsement(endorsement, given, 0)))
                .extracting(LedgerEntry::points, LedgerEntry::limitedBy)
                .containsExactly(tuple(8, null), tuple(4, PointsLimit.SAME_PERSON), tuple(2, PointsLimit.SAME_PERSON),
                        tuple(1, PointsLimit.SAME_PERSON), tuple(0, PointsLimit.SAME_PERSON));
        assertThat(AntiCheatRules.endorsement(endorsement, pair, pair))
                .extracting(LedgerEntry::points, LedgerEntry::limitedBy).containsExactly(0, PointsLimit.MUTUAL_PAIR);
        // One each way is two friends, not a pair that keeps endorsing each other
        assertThat(AntiCheatRules.endorsement(endorsement, 1, 1).points()).isEqualTo(8);
        assertThat(AntiCheatRules.endorsement(endorsement, pair, pair - 1).limitedBy())
                .isEqualTo(PointsLimit.SAME_PERSON);
        assertThat(AntiCheatRules.dailyCapped(endorsement, AntiCheatRules.MAX_ENDORSEMENTS_PER_DAY))
                .extracting(LedgerEntry::points, LedgerEntry::limitedBy).containsExactly(0, PointsLimit.DAILY_CAP);
        assertThat(AntiCheatRules.dailyCapped(endorsement, AntiCheatRules.MAX_ENDORSEMENTS_PER_DAY - 1))
                .isEqualTo(endorsement);
    }

    @Test
    void theLastLimitThatCutAnEntryExplainsItsPoints() {
        var halved = AntiCheatRules.kudos(entry(Reason.KUDOS), 2, 0);

        assertThat(AntiCheatRules.dailyCapped(halved, AntiCheatRules.MAX_KUDOS_PER_DAY))
                .extracting(LedgerEntry::points, LedgerEntry::limitedBy).containsExactly(0, PointsLimit.DAILY_CAP);
        // A limit that would not cut it changes nothing
        assertThat(halved.limited(halved.points(), PointsLimit.MUTUAL_PAIR)).isEqualTo(halved);
    }
}

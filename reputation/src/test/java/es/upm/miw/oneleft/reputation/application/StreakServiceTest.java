package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.InMemoryReputation;
import es.upm.miw.oneleft.reputation.domain.model.CheckInFact;
import es.upm.miw.oneleft.reputation.domain.model.Level;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.model.Streak;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-062: the weekly streak from the check-ins, and the level of the score. */
class StreakServiceTest {

    /** Tuesday 13 October 2026 */
    private static final Instant NOW = Instant.parse("2026-10-13T16:30:00Z");
    private final InMemoryReputation store = new InMemoryReputation();
    private final ReputationService service = new ReputationService(store, store, store, store,
            (userId, badge, at) -> { }, Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID lucia = UUID.randomUUID();

    private void checkInWeeksAgo(int weeks) {
        service.record(new CheckInFact(UUID.randomUUID(), "Padel", lucia, NOW.minus(Duration.ofDays(7L * weeks))));
    }

    private List<Integer> bonuses() {
        return store.entries.stream().filter(entry -> entry.reason() == Reason.WEEKLY_STREAK)
                .sorted((one, other) -> one.occurredAt().compareTo(other.occurredAt()))
                .map(LedgerEntry::points).toList();
    }

    @Test
    void eachWeekInARowWithAPlanGivesFiveMoreUpToTheCap() {
        for (var weeks = 6; weeks >= 0; weeks--) {
            checkInWeeksAgo(weeks);
        }

        assertThat(bonuses()).containsExactly(5, 10, 15, 20, 25, 25, 25);
        assertThat(service.streakOf(lucia)).isEqualTo(new Streak(7, true));
    }

    @Test
    void aSecondPlanInTheSameWeekOrTheSameCheckInTwiceGiveNoMoreBonus() {
        var checkIn = new CheckInFact(UUID.randomUUID(), "Padel", lucia, NOW);
        service.record(checkIn);
        service.record(checkIn);
        service.record(new CheckInFact(UUID.randomUUID(), "Running", lucia, NOW.plusSeconds(3600)));

        assertThat(bonuses()).containsExactly(5);
        assertThat(service.reputationOf(lucia).score()).isEqualTo(10 + 10 + 5);
    }

    @Test
    void aWeekWithoutPlansStartsTheStreakAgain() {
        checkInWeeksAgo(3);
        checkInWeeksAgo(2);
        checkInWeeksAgo(0);

        assertThat(bonuses()).containsExactly(5, 10, 5);
        assertThat(service.streakOf(lucia)).isEqualTo(new Streak(1, true));
    }

    @Test
    void aStreakWithoutAPlanThisWeekYetIsStillAlive() {
        checkInWeeksAgo(2);
        checkInWeeksAgo(1);

        assertThat(service.streakOf(lucia)).isEqualTo(new Streak(2, false));
        assertThat(service.streakOf(lucia).nextBonus()).isEqualTo(15);
        assertThat(service.streakOf(UUID.randomUUID())).isEqualTo(Streak.NONE);
    }

    @Test
    void theScoreGivesTheLevel() {
        for (var weeks = 9; weeks >= 0; weeks--) {
            checkInWeeksAgo(weeks);
        }

        // Ten check-ins and the bonuses of ten weeks in a row: 100 + 5 + 10 + 15 + 20 + 25 × 6
        var reputation = service.reputationOf(lucia);
        assertThat(reputation.score()).isEqualTo(300);
        assertThat(reputation.level()).isEqualTo(Level.REGULAR);
        assertThat(reputation.nextLevelAt()).contains(500);
    }
}

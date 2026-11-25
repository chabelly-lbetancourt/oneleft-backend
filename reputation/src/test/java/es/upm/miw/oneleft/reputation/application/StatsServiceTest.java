package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.InMemoryReputation;
import es.upm.miw.oneleft.reputation.domain.model.ActivityPlans;
import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.MonthPlans;
import es.upm.miw.oneleft.reputation.domain.model.MyStats;
import es.upm.miw.oneleft.reputation.domain.model.StatsQuery;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-043: my statistics, worked out from the track record that the attendance events fill. */
class StatsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");
    private static final Instant NOVEMBER = Instant.parse("2026-11-10T18:00:00Z");
    private static final Instant OCTOBER = Instant.parse("2026-10-10T18:00:00Z");
    private final InMemoryReputation store = new InMemoryReputation();
    private final StatsService service = new StatsService(store, Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    private void plan(UUID userId, String activity, boolean organizer, boolean arrived, Instant closedAt) {
        store.record(List.of(new Attendance(userId, UUID.randomUUID(), activity, organizer, arrived, true,
                closedAt)));
    }

    @Test
    void myStatsCountThePlansIArrivedAtByMonthActivityAndWhoOrganizedThem() {
        plan(ana, "PADEL", true, true, NOVEMBER);
        plan(ana, "PADEL", false, true, NOVEMBER.plusSeconds(60));
        plan(ana, "RUNNING", false, true, NOVEMBER.plusSeconds(120));
        plan(ana, "PADEL", false, true, OCTOBER);
        plan(ana, "CHESS", false, true, Instant.parse("2025-01-10T18:00:00Z"));
        // A no-show is not a plan of mine, and the plans of someone else are theirs
        plan(ana, "RUNNING", false, false, NOVEMBER);
        plan(lucia, "RUNNING", true, true, NOVEMBER);

        var stats = service.myStats(ana, new StatsQuery(3, 2));

        assertThat(stats.plansThisMonth()).isEqualTo(3);
        assertThat(stats.plansCompleted()).isEqualTo(5);
        assertThat(stats.plansOrganized()).isEqualTo(1);
        assertThat(stats.plansJoined()).isEqualTo(4);
        assertThat(stats.favoriteActivity()).contains("PADEL");
        // Only the two asked for: among equals, the one done the latest
        assertThat(stats.activities()).containsExactly(new ActivityPlans("PADEL", 3),
                new ActivityPlans("RUNNING", 1));
        assertThat(stats.months()).containsExactly(MonthPlans.empty(YearMonth.of(2026, 9)),
                new MonthPlans(YearMonth.of(2026, 10), 1, 0, 1), new MonthPlans(YearMonth.of(2026, 11), 3, 1, 2));
    }

    @Test
    void withoutPlansEverythingIsZeroAndTheMonthsAreStillThere() {
        var stats = service.myStats(ana, new StatsQuery(StatsQuery.DEFAULT_MONTHS, StatsQuery.DEFAULT_ACTIVITIES));

        assertThat(stats).isEqualTo(new MyStats(0, 0, 0, List.of(), stats.months()));
        assertThat(stats.months()).hasSize(StatsQuery.DEFAULT_MONTHS).allMatch(month -> month.completed() == 0);
        assertThat(stats.favoriteActivity()).isEmpty();
    }

    @Test
    void organizingAPlanThatWasNotHeldDoesNotCount() {
        store.record(List.of(new Attendance(ana, UUID.randomUUID(), "PADEL", true, false, false, NOVEMBER)));

        var stats = service.myStats(ana, new StatsQuery(1, 1));

        assertThat(stats.plansOrganized()).isZero();
        assertThat(stats.months()).containsExactly(MonthPlans.empty(YearMonth.of(2026, 11)));
    }
}

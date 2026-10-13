package es.upm.miw.oneleft.reputation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/** HU-009: the entries each event gives. */
class ReputationRulesTest {

    private static final Instant NOW = Instant.parse("2026-10-13T16:30:00.123Z");
    private final UUID plan = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();

    private AttendanceFact attendance(List<UUID> attendees, List<UUID> absentees, boolean full) {
        return new AttendanceFact(plan, "Padel", ana, attendees, absentees, full, NOW);
    }

    @Test
    void checkingInIsWorthTenPoints() {
        var entry = ReputationRules.attended(new CheckInFact(plan, "Padel", lucia, NOW));

        assertThat(entry).isEqualTo(new LedgerEntry(lucia, Reason.ATTENDED, 10, plan, "Padel",
                Instant.parse("2026-10-13T16:30:00Z")));
    }

    @Test
    void whoDidNotShowUpToAPlanThatWasHeldLosesFifteenAndTheOrganizerOfAFullOneWinsTwenty() {
        var entries = ReputationRules.attendanceClosed(attendance(List.of(lucia), List.of(ana, diego), true));

        assertThat(entries).extracting(LedgerEntry::userId, LedgerEntry::reason, LedgerEntry::points)
                .containsExactly(tuple(ana, Reason.NO_SHOW, -15),
                        tuple(diego, Reason.NO_SHOW, -15),
                        tuple(ana, Reason.ORGANIZED_HELD, 20));
    }

    @Test
    void nobodyIsJudgedByAPlanThatWasNotHeld() {
        assertThat(ReputationRules.attendanceClosed(attendance(List.of(), List.of(ana, lucia), true))).isEmpty();
    }

    @Test
    void organizingNeedsAFullPlanAndAParticipantWhoArrived() {
        assertThat(ReputationRules.attendanceClosed(attendance(List.of(lucia), List.of(), false))).isEmpty();
        // Only the organizer arrived: the plan was not really held with anyone
        assertThat(ReputationRules.attendanceClosed(attendance(List.of(ana), List.of(lucia), true)))
                .extracting(LedgerEntry::reason).containsExactly(Reason.NO_SHOW);
    }

    @Test
    void cancellingAnOwnPlanLessThanAnHourBeforeItsStartCostsTen() {
        var late = new CancellationFact(plan, "Padel", ana, NOW.plus(Duration.ofMinutes(59)), true, NOW);
        var inTime = new CancellationFact(plan, "Padel", ana, NOW.plus(Duration.ofHours(1)), true, NOW);
        var forTheMinimum = new CancellationFact(plan, "Padel", ana, NOW.plus(Duration.ofMinutes(10)), false, NOW);

        assertThat(ReputationRules.cancelled(late)).hasValueSatisfying(entry -> {
            assertThat(entry.userId()).isEqualTo(ana);
            assertThat(entry.points()).isEqualTo(-10);
        });
        assertThat(ReputationRules.cancelled(inTime)).isEmpty();
        assertThat(ReputationRules.cancelled(forTheMinimum)).isEmpty();
    }

    @Test
    void aConfirmedReportCostsFiftyAndHasNoPlan() {
        var report = UUID.randomUUID();

        var entry = ReputationRules.reportConfirmed(new ReportFact(report, diego, NOW));

        assertThat(entry.points()).isEqualTo(-50);
        assertThat(entry.sourceId()).isEqualTo(report);
        assertThat(entry.planTitle()).isNull();
    }

    @Test
    void anEntryNeedsWhoWhyWhatAndWhen() {
        assertThatThrownBy(() -> LedgerEntry.of(null, Reason.ATTENDED, plan, "Padel", NOW))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("ledger.missingData"));
        assertThatThrownBy(() -> new PageQuery(0, 51)).isInstanceOf(ValidationException.class);
    }
}

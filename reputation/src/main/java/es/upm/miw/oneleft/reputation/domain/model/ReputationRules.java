package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The rules of HU-009: which entries each event of the other services gives. Nobody is judged by a plan that was not
 * held: if nobody of the group checked in, there are no no-shows and no reward for organizing it.
 */
public final class ReputationRules {

    /** Cancelling an own plan closer than this to its start costs points. */
    public static final Duration LATE_CANCELLATION = Duration.ofHours(1);

    private ReputationRules() {
    }

    public static LedgerEntry attended(CheckInFact checkIn) {
        return LedgerEntry.of(checkIn.userId(), Reason.ATTENDED, checkIn.planId(), checkIn.title(),
                checkIn.occurredAt());
    }

    public static List<LedgerEntry> attendanceClosed(AttendanceFact attendance) {
        var entries = new ArrayList<LedgerEntry>();
        if (attendance.attendeeIds().isEmpty()) {
            return entries;
        }
        attendance.absenteeIds().forEach(userId -> entries.add(LedgerEntry.of(userId, Reason.NO_SHOW,
                attendance.planId(), attendance.title(), attendance.occurredAt())));
        var participantArrived = attendance.attendeeIds().stream()
                .anyMatch(userId -> !userId.equals(attendance.organizerId()));
        if (attendance.full() && participantArrived) {
            entries.add(LedgerEntry.of(attendance.organizerId(), Reason.ORGANIZED_HELD, attendance.planId(),
                    attendance.title(), attendance.occurredAt()));
        }
        return entries;
    }

    public static Optional<LedgerEntry> cancelled(CancellationFact cancellation) {
        var late = Duration.between(cancellation.occurredAt(), cancellation.startsAt())
                .compareTo(LATE_CANCELLATION) < 0;
        if (!cancellation.byOrganizer() || cancellation.organizerId() == null || !late) {
            return Optional.empty();
        }
        return Optional.of(LedgerEntry.of(cancellation.organizerId(), Reason.LATE_CANCELLATION,
                cancellation.planId(), cancellation.title(), cancellation.occurredAt()));
    }

    public static LedgerEntry reportConfirmed(ReportFact report) {
        return LedgerEntry.of(report.userId(), Reason.CONFIRMED_REPORT, report.reportId(), null,
                report.occurredAt());
    }
}

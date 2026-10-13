package es.upm.miw.oneleft.reputation.infrastructure.messaging;

import es.upm.miw.oneleft.reputation.domain.model.AttendanceFact;
import es.upm.miw.oneleft.reputation.domain.model.CancellationFact;
import es.upm.miw.oneleft.reputation.domain.model.CheckInFact;
import es.upm.miw.oneleft.reputation.domain.model.ReportFact;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The part of each event that the reputation reads: the JSON fields are the contract. */
final class ReputationMessages {

    /** The reason of a cancellation decided by the organizer (the plans service does not send it yet) */
    static final String BY_ORGANIZER = "ORGANIZER";

    private ReputationMessages() {
    }

    /** plan.checked-in */
    record CheckIn(UUID planId, String title, UUID userId, Instant occurredAt) {

        CheckInFact toDomain() {
            return new CheckInFact(planId, title, userId, occurredAt);
        }
    }

    /** plan.attendance-closed */
    record Attendance(UUID planId, String title, UUID organizerId, List<UUID> attendeeIds, List<UUID> absenteeIds,
                      boolean full, Instant occurredAt) {

        AttendanceFact toDomain() {
            return new AttendanceFact(planId, title, organizerId, attendeeIds, absenteeIds, full, occurredAt);
        }
    }

    /** plan.cancelled */
    record Cancellation(UUID planId, UUID organizerId, String title, Instant startsAt, String reason,
                        Instant occurredAt) {

        CancellationFact toDomain() {
            return new CancellationFact(planId, title, organizerId, startsAt, BY_ORGANIZER.equals(reason),
                    occurredAt);
        }
    }

    /** report.confirmed (HU-033) */
    record Report(UUID reportId, UUID userId, Instant occurredAt) {

        ReportFact toDomain() {
            return new ReportFact(reportId, userId, occurredAt);
        }
    }
}

package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * An entry of the ledger of reputation (HU-009): who, why, how many points, what caused it (a plan or a report) and
 * when. Entries are never changed: the score is their sum. The points are kept with the entry, so that changing a rule
 * does not rewrite the past.
 *
 * @param sourceId  the plan or the report that caused it; with the user and the reason, it identifies the entry
 * @param planTitle the title of the plan when it was a plan, to explain the entry
 */
public record LedgerEntry(UUID userId, Reason reason, int points, UUID sourceId, String planTitle,
                          Instant occurredAt) {

    public LedgerEntry {
        if (userId == null || reason == null || sourceId == null || occurredAt == null) {
            throw new ValidationException("ledger.missingData", "Required ledger data is missing");
        }
        occurredAt = occurredAt.truncatedTo(ChronoUnit.SECONDS);
    }

    public static LedgerEntry of(UUID userId, Reason reason, UUID sourceId, String planTitle, Instant occurredAt) {
        return new LedgerEntry(userId, reason, reason.points(), sourceId, planTitle, occurredAt);
    }
}

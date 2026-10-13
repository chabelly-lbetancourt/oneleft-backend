package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A report against someone was confirmed by the moderation (HU-033). */
public record ReportFact(UUID reportId, UUID userId, Instant occurredAt) {
}

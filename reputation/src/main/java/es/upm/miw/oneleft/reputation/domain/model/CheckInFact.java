package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Someone checked in to a plan (HU-025). */
public record CheckInFact(UUID planId, String title, UUID userId, Instant occurredAt) {
}

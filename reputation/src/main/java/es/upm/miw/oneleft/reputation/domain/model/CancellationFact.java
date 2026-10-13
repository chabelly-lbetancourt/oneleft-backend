package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A plan was cancelled, by its organizer or for another reason (the minimum of participants, HU-039). */
public record CancellationFact(UUID planId, String title, UUID organizerId, Instant startsAt, boolean byOrganizer,
                               Instant occurredAt) {
}

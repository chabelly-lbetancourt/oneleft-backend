package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.BlockedPerson;

import java.time.Instant;
import java.util.UUID;

/** A row of my list of blocked people: the person and since when, read in the same query. */
public record BlockedRow(UUID userId, String name, Instant blockedAt) {

    BlockedPerson toDomain() {
        return new BlockedPerson(userId, name, blockedAt);
    }
}

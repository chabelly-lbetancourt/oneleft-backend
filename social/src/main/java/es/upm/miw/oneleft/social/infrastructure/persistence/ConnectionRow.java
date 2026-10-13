package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.Connection;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;

import java.time.Instant;
import java.util.UUID;

/** A row of a list: the person and the follow that puts them there, read in the same query. */
public record ConnectionRow(UUID userId, String name, FollowStatus status, Instant requestedAt, Instant acceptedAt) {

    Connection toDomain() {
        return new Connection(userId, name, status, acceptedAt == null ? requestedAt : acceptedAt);
    }
}

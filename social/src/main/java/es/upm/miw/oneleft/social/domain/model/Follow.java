package es.upm.miw.oneleft.social.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Someone follows someone else (HU-047). Following a public profile is accepted at once; following a private one is a
 * request until they accept it. Times are kept to the second.
 */
public record Follow(UUID followerId, UUID followeeId, FollowStatus status, Instant requestedAt, Instant acceptedAt) {

    public Follow {
        if (followerId == null || followeeId == null || status == null || requestedAt == null) {
            throw new ValidationException("follow.missingData", "Required follow data is missing");
        }
        if (followerId.equals(followeeId)) {
            throw new ValidationException("follow.self", "Nobody follows themselves");
        }
        if ((status == FollowStatus.ACCEPTED) != (acceptedAt != null)) {
            throw new ValidationException("follow.acceptedAt", "Only an accepted follow has an acceptance time");
        }
        requestedAt = requestedAt.truncatedTo(ChronoUnit.SECONDS);
        acceptedAt = acceptedAt == null ? null : acceptedAt.truncatedTo(ChronoUnit.SECONDS);
    }

    /** {@code follower} starts following {@code followee}, or asks to if their profile is private. */
    public static Follow request(Person follower, Person followee, Clock clock) {
        var now = clock.instant();
        return followee.visibility() == Visibility.PUBLIC
                ? new Follow(follower.userId(), followee.userId(), FollowStatus.ACCEPTED, now, now)
                : new Follow(follower.userId(), followee.userId(), FollowStatus.PENDING, now, null);
    }

    public Follow accept(Clock clock) {
        return isPending() ? new Follow(followerId, followeeId, FollowStatus.ACCEPTED, requestedAt, clock.instant())
                : this;
    }

    public boolean isPending() {
        return status == FollowStatus.PENDING;
    }

    /** Since when: the acceptance for a follow, the request for a request. */
    public Instant since() {
        return isPending() ? requestedAt : acceptedAt;
    }
}

package es.upm.miw.oneleft.social.domain.port.out;

import es.upm.miw.oneleft.social.domain.model.Connection;
import es.upm.miw.oneleft.social.domain.model.Follow;
import es.upm.miw.oneleft.social.domain.model.FollowCounts;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Who follows whom, and the requests. The lists come with the names in a single query. */
public interface Follows {

    Optional<Follow> find(UUID followerId, UUID followeeId);

    Follow save(Follow follow);

    void delete(UUID followerId, UUID followeeId);

    /** Accepts every request to {@code followeeId}: they have opened their profile. */
    void acceptAllRequests(UUID followeeId, Instant at);

    /** The people who follow {@code userId} (or ask to), the latest first. */
    PageResult<Connection> followersOf(UUID userId, FollowStatus status, PageQuery page);

    /** The people {@code userId} follows (or asks to follow), the latest first. */
    PageResult<Connection> followedBy(UUID userId, FollowStatus status, PageQuery page);

    FollowCounts counts(UUID userId);

    /** Everyone who follows {@code userId}, accepted: whose feed shows what they do (HU-051) */
    List<UUID> acceptedFollowerIds(UUID userId);
}

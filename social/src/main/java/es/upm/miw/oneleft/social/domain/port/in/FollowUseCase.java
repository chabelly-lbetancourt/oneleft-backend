package es.upm.miw.oneleft.social.domain.port.in;

import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.Connection;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;

import java.util.UUID;

/** Following people, the requests to private profiles and my lists (HU-047). */
public interface FollowUseCase {

    /** I follow someone, or ask to if their profile is private. Doing it again changes nothing. */
    Connection follow(Caller me, UUID userId);

    /** I stop following someone, or take back my request. */
    void unfollow(UUID me, UUID userId);

    /** I accept the request of someone, who becomes one of my followers. */
    Connection accept(UUID me, UUID userId);

    void decline(UUID me, UUID userId);

    /** Someone stops following me. */
    void removeFollower(UUID me, UUID userId);

    PageResult<Connection> followers(UUID me, PageQuery page);

    PageResult<Connection> following(UUID me, PageQuery page);

    /**
     * The followers of someone (HU-048).
     *
     * @throws es.upm.miw.oneleft.social.domain.model.SocialException {@code PRIVATE} if their profile is private and I
     *                                                                  do not follow them
     */
    PageResult<Connection> followersOf(Caller me, UUID userId, PageQuery page);

    /** Who someone follows (HU-048), with the same privacy as {@link #followersOf}. */
    PageResult<Connection> followingOf(Caller me, UUID userId, PageQuery page);

    /** The requests waiting for my answer. */
    PageResult<Connection> requests(UUID me, PageQuery page);

    /** My requests waiting for an answer. */
    PageResult<Connection> sentRequests(UUID me, PageQuery page);
}

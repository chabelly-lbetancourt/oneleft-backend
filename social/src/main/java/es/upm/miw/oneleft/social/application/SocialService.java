package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.Connection;
import es.upm.miw.oneleft.social.domain.model.Follow;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.Person;
import es.upm.miw.oneleft.social.domain.model.PersonView;
import es.upm.miw.oneleft.social.domain.model.Relation;
import es.upm.miw.oneleft.social.domain.model.SocialAccount;
import es.upm.miw.oneleft.social.domain.model.SocialException;
import es.upm.miw.oneleft.social.domain.model.ValidationException;
import es.upm.miw.oneleft.social.domain.model.Visibility;
import es.upm.miw.oneleft.social.domain.port.in.FollowUseCase;
import es.upm.miw.oneleft.social.domain.port.in.SocialAccountUseCase;
import es.upm.miw.oneleft.social.domain.port.out.Directory;
import es.upm.miw.oneleft.social.domain.port.out.FeedCache;
import es.upm.miw.oneleft.social.domain.port.out.Follows;
import es.upm.miw.oneleft.social.domain.port.out.People;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Following people (HU-047). Whoever uses the service is registered with the name of their account; someone new is
 * looked up once in the users service. A profile is private until its owner opens it.
 */
@Service
@Transactional
public class SocialService implements SocialAccountUseCase, FollowUseCase {

    /** Nobody follows a new person without their permission until they open their profile (HU-047, HU-059). */
    static final Visibility DEFAULT_VISIBILITY = Visibility.PRIVATE;

    private final People people;
    private final Follows follows;
    private final Directory directory;
    private final FeedCache feeds;
    private final Clock clock;

    public SocialService(People people, Follows follows, Directory directory, FeedCache feeds, Clock clock) {
        this.people = people;
        this.follows = follows;
        this.directory = directory;
        this.feeds = feeds;
        this.clock = clock;
    }

    @Override
    public SocialAccount myAccount(Caller me) {
        return account(register(me));
    }

    @Override
    public SocialAccount changeVisibility(Caller me, Visibility visibility) {
        if (visibility == null) {
            throw new ValidationException("person.visibility", "Choose a public or a private profile");
        }
        var person = people.save(register(me).withVisibility(visibility));
        if (visibility == Visibility.PUBLIC) {
            follows.acceptAllRequests(me.userId(), clock.instant());
            // Whoever was waiting now follows them: their feeds change (HU-051)
            feeds.forget(follows.acceptedFollowerIds(me.userId()));
        }
        return account(person);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonView person(Caller me, UUID userId) {
        var person = people.find(userId).orElseGet(() -> userId.equals(me.userId())
                ? new Person(me.userId(), me.name(), DEFAULT_VISIBILITY) : stranger(userId));
        var counts = follows.counts(userId);
        var followsMe = follows.find(userId, me.userId()).filter(follow -> !follow.isPending()).isPresent();
        return new PersonView(person, counts.followers(), counts.following(), relation(me.userId(), userId),
                followsMe);
    }

    @Override
    public Connection follow(Caller me, UUID userId) {
        var follower = register(me);
        if (follower.userId().equals(userId)) {
            throw new ValidationException("follow.self", "Nobody follows themselves");
        }
        var followee = people.find(userId).orElseGet(() -> people.save(stranger(userId)));
        var follow = follows.find(me.userId(), userId)
                .orElseGet(() -> follows.save(Follow.request(follower, followee, clock)));
        feeds.forget(List.of(me.userId()));
        return Connection.of(followee, follow);
    }

    @Override
    public void unfollow(UUID me, UUID userId) {
        follows.delete(me, userId);
        feeds.forget(List.of(me));
    }

    @Override
    public Connection accept(UUID me, UUID userId) {
        var request = follows.find(userId, me).filter(Follow::isPending).orElseThrow(() -> new SocialException(
                SocialException.Reason.REQUEST_NOT_FOUND, "There is no request of that person"));
        var follower = people.find(userId).orElseThrow(() -> new SocialException(
                SocialException.Reason.PERSON_NOT_FOUND, "Unknown person"));
        feeds.forget(List.of(userId));
        return Connection.of(follower, follows.save(request.accept(clock)));
    }

    @Override
    public void decline(UUID me, UUID userId) {
        follows.find(userId, me).filter(Follow::isPending).ifPresent(request -> follows.delete(userId, me));
    }

    @Override
    public void removeFollower(UUID me, UUID userId) {
        follows.find(userId, me).filter(follow -> !follow.isPending())
                .ifPresent(follow -> follows.delete(userId, me));
        feeds.forget(List.of(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Connection> followers(UUID me, PageQuery page) {
        return follows.followersOf(me, FollowStatus.ACCEPTED, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Connection> following(UUID me, PageQuery page) {
        return follows.followedBy(me, FollowStatus.ACCEPTED, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Connection> followersOf(Caller me, UUID userId, PageQuery page) {
        requireActivityVisible(me, userId);
        return follows.followersOf(userId, FollowStatus.ACCEPTED, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Connection> followingOf(Caller me, UUID userId, PageQuery page) {
        requireActivityVisible(me, userId);
        return follows.followedBy(userId, FollowStatus.ACCEPTED, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Connection> requests(UUID me, PageQuery page) {
        return follows.followersOf(me, FollowStatus.PENDING, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Connection> sentRequests(UUID me, PageQuery page) {
        return follows.followedBy(me, FollowStatus.PENDING, page);
    }

    private void requireActivityVisible(Caller me, UUID userId) {
        if (!person(me, userId).activityVisible()) {
            throw new SocialException(SocialException.Reason.PRIVATE, "Only their followers see their network");
        }
    }

    /** The caller as a person of the service: created the first time, with the current name of their account. */
    private Person register(Caller me) {
        var known = people.find(me.userId());
        if (known.isEmpty()) {
            return people.save(new Person(me.userId(), me.name(), DEFAULT_VISIBILITY));
        }
        var renamed = known.get().renamed(me.name());
        return renamed == known.get() ? renamed : people.save(renamed);
    }

    /** Someone the service does not know yet: their name from the users service, private until they choose. */
    private Person stranger(UUID userId) {
        return new Person(userId, directory.nameOf(userId), DEFAULT_VISIBILITY);
    }

    private Relation relation(UUID me, UUID userId) {
        if (me.equals(userId)) {
            return Relation.SELF;
        }
        return follows.find(me, userId).map(follow -> follow.isPending() ? Relation.REQUESTED : Relation.FOLLOWING)
                .orElse(Relation.NONE);
    }

    private SocialAccount account(Person person) {
        return new SocialAccount(person, follows.counts(person.userId()));
    }
}

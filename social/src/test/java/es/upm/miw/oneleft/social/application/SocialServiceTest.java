package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.InMemoryFeed;
import es.upm.miw.oneleft.social.InMemorySafety;
import es.upm.miw.oneleft.social.InMemorySocial;
import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.Connection;
import es.upm.miw.oneleft.social.domain.model.FeedAudience;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.Person;
import es.upm.miw.oneleft.social.domain.model.Relation;
import es.upm.miw.oneleft.social.domain.model.SocialException;
import es.upm.miw.oneleft.social.domain.model.ValidationException;
import es.upm.miw.oneleft.social.domain.model.Visibility;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-047: following, the requests to private profiles, the lists and the names of new people. */
class SocialServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T16:00:00Z"), ZoneOffset.UTC);
    private static final PageQuery FIRST = new PageQuery(0, 20);
    private final Caller lucia = new Caller(UUID.randomUUID(), "Lucía");
    private final Caller ana = new Caller(UUID.randomUUID(), "Ana");
    private final UUID diego = UUID.randomUUID();
    private final InMemorySocial social = new InMemorySocial();
    private final InMemoryFeed feed = new InMemoryFeed(social);
    private final InMemorySafety safety = new InMemorySafety(social);
    private final List<UUID> asked = new ArrayList<>();
    private final SocialService service = new SocialService(social, social, safety, userId -> {
        asked.add(userId);
        if (userId.equals(ana.userId())) {
            return "Ana";
        }
        throw new SocialException(SocialException.Reason.PERSON_NOT_FOUND, "Unknown");
    }, feed, CLOCK);

    @Test
    void aNewPersonIsPrivateWithTheNameOfTheirAccount() {
        var account = service.myAccount(lucia);

        // Private by default (HU-047), and the activity only for the followers it accepts (HU-059)
        assertThat(account.person())
                .isEqualTo(new Person(lucia.userId(), "Lucía", Visibility.PRIVATE, FeedAudience.FOLLOWERS));
        assertThat(account.counts().followers()).isZero();
        // The name follows the account, and nothing is saved when it has not changed
        service.myAccount(lucia);
        assertThat(social.saves).isEqualTo(1);
        assertThat(service.myAccount(new Caller(lucia.userId(), "Lucía M.")).person().name()).isEqualTo("Lucía M.");
    }

    @Test
    void followingAPrivateProfileIsARequestThatItsOwnerAccepts() {
        var request = service.follow(lucia, ana.userId());

        assertThat(request).isEqualTo(new Connection(ana.userId(), "Ana", FollowStatus.PENDING,
                CLOCK.instant()));
        assertThat(asked).containsExactly(ana.userId());
        assertThat(service.sentRequests(lucia.userId(), FIRST).items()).extracting(Connection::name)
                .containsExactly("Ana");
        assertThat(service.requests(ana.userId(), FIRST).items()).extracting(Connection::name)
                .containsExactly("Lucía");
        assertThat(service.person(lucia, ana.userId()).relation()).isEqualTo(Relation.REQUESTED);

        var follower = service.accept(ana.userId(), lucia.userId());

        assertThat(follower.status()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(service.followers(ana.userId(), FIRST).items()).extracting(Connection::name)
                .containsExactly("Lucía");
        assertThat(service.following(lucia.userId(), FIRST).total()).isEqualTo(1);
        assertThat(service.requests(ana.userId(), FIRST).items()).isEmpty();
        assertThat(service.person(ana, lucia.userId()).followsMe()).isTrue();
    }

    @Test
    void aPublicProfileIsFollowedAtOnceAndFollowingAgainChangesNothing() {
        service.changeVisibility(ana, Visibility.PUBLIC);

        var follow = service.follow(lucia, ana.userId());
        var again = service.follow(lucia, ana.userId());

        assertThat(follow.status()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(again).isEqualTo(follow);
        assertThat(asked).isEmpty();
        var view = service.person(lucia, ana.userId());
        assertThat(view.relation()).isEqualTo(Relation.FOLLOWING);
        assertThat(view.followers()).isEqualTo(1);
        assertThat(view.followsMe()).isFalse();
    }

    @Test
    void theNetworkOfAPrivateProfileIsOnlyForItsFollowers() {
        var diegoCaller = new Caller(diego, "Diego");
        service.myAccount(diegoCaller);
        service.follow(diegoCaller, ana.userId());
        service.accept(ana.userId(), diego);
        service.follow(lucia, ana.userId());

        assertThat(service.person(lucia, ana.userId()).activityVisible()).isFalse();
        assertThatThrownBy(() -> service.followersOf(lucia, ana.userId(), FIRST))
                .isInstanceOfSatisfying(SocialException.class,
                        error -> assertThat(error.code()).isEqualTo("social.private"));
        assertThatThrownBy(() -> service.followingOf(lucia, ana.userId(), FIRST))
                .isInstanceOf(SocialException.class);

        service.accept(ana.userId(), lucia.userId());

        assertThat(service.person(lucia, ana.userId()).activityVisible()).isTrue();
        assertThat(service.followersOf(lucia, ana.userId(), FIRST).items()).extracting(Connection::name)
                .containsExactlyInAnyOrder("Diego", "Lucía");
        assertThat(service.followingOf(lucia, ana.userId(), FIRST).items()).isEmpty();
        // Diego's profile is private too, and Lucía does not follow him
        assertThatThrownBy(() -> service.followingOf(lucia, diego, FIRST)).isInstanceOf(SocialException.class);
    }

    @Test
    void theNetworkOfAPublicProfileOrMyOwnIsForEveryone() {
        service.myAccount(lucia);
        service.changeVisibility(ana, Visibility.PUBLIC);
        service.follow(ana, lucia.userId());
        service.accept(lucia.userId(), ana.userId());

        assertThat(service.person(new Caller(diego, "Diego"), ana.userId()).activityVisible()).isTrue();
        assertThat(service.followingOf(new Caller(diego, "Diego"), ana.userId(), FIRST).items())
                .extracting(Connection::name).containsExactly("Lucía");
        assertThat(service.followersOf(lucia, lucia.userId(), FIRST).items()).extracting(Connection::name)
                .containsExactly("Ana");
        assertThat(service.person(lucia, lucia.userId()).activityVisible()).isTrue();
    }

    @Test
    void openingAPrivateProfileAcceptsTheRequestsAndClosingItKeepsTheFollowers() {
        service.follow(lucia, ana.userId());

        var open = service.changeVisibility(ana, Visibility.PUBLIC);
        assertThat(open.counts().followers()).isEqualTo(1);
        assertThat(open.counts().requests()).isZero();

        var closed = service.changeVisibility(ana, Visibility.PRIVATE);
        assertThat(closed.person().visibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(closed.counts().followers()).isEqualTo(1);
        assertThatThrownBy(() -> service.changeVisibility(ana, null))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("person.visibility"));
    }

    @Test
    void requestsCanBeTakenBackOrDeclinedAndFollowersRemoved() {
        service.follow(lucia, ana.userId());
        service.unfollow(lucia.userId(), ana.userId());
        assertThat(service.requests(ana.userId(), FIRST).total()).isZero();

        service.follow(lucia, ana.userId());
        service.decline(ana.userId(), lucia.userId());
        assertThat(service.sentRequests(lucia.userId(), FIRST).total()).isZero();
        assertThatThrownBy(() -> service.accept(ana.userId(), lucia.userId()))
                .isInstanceOfSatisfying(SocialException.class,
                        error -> assertThat(error.code()).isEqualTo("follow.requestNotFound"));

        service.follow(lucia, ana.userId());
        // Removing a follower does not touch a request, and declining does not touch a follower
        service.removeFollower(ana.userId(), lucia.userId());
        assertThat(service.requests(ana.userId(), FIRST).total()).isEqualTo(1);
        service.accept(ana.userId(), lucia.userId());
        service.decline(ana.userId(), lucia.userId());
        assertThat(service.followers(ana.userId(), FIRST).total()).isEqualTo(1);
        service.removeFollower(ana.userId(), lucia.userId());
        assertThat(service.followers(ana.userId(), FIRST).total()).isZero();
    }

    @Test
    void nobodyFollowsThemselvesOrSomeoneWithoutAnAccount() {
        assertThatThrownBy(() -> service.follow(lucia, lucia.userId()))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("follow.self"));
        assertThatThrownBy(() -> service.follow(lucia, diego))
                .isInstanceOfSatisfying(SocialException.class,
                        error -> assertThat(error.code()).isEqualTo("person.notFound"));
        assertThat(social.find(diego)).isEmpty();
    }

    @Test
    void myOwnPageNeedsNoQuestionToTheUsersService() {
        var me = service.person(lucia, lucia.userId());

        assertThat(me.relation()).isEqualTo(Relation.SELF);
        assertThat(me.person().name()).isEqualTo("Lucía");
        assertThat(asked).isEmpty();
        // Someone unknown is looked up but not saved just for being seen
        assertThat(service.person(lucia, ana.userId()).relation()).isEqualTo(Relation.NONE);
        assertThat(social.find(ana.userId())).isEmpty();
    }

    @Test
    void whoeverStartsOrStopsFollowingSomeoneHasTheirFeedComputedAgain() {
        social.save(new Person(ana.userId(), "Ana", Visibility.PRIVATE, FeedAudience.FOLLOWERS));
        service.follow(lucia, ana.userId());
        assertThat(feed.forgotten).containsExactly(lucia.userId());

        service.accept(ana.userId(), lucia.userId());
        service.unfollow(lucia.userId(), ana.userId());
        assertThat(feed.forgotten).containsExactly(lucia.userId(), lucia.userId(), lucia.userId());

        // A private profile that becomes public lets in whoever was waiting: their feeds change too
        service.follow(lucia, ana.userId());
        feed.forgotten.clear();
        service.changeVisibility(ana, Visibility.PUBLIC);
        assertThat(feed.forgotten).containsExactly(lucia.userId());

        service.removeFollower(ana.userId(), lucia.userId());
        assertThat(feed.forgotten).containsExactly(lucia.userId(), lucia.userId());
    }

    @Test
    void theActivityIsSharedWithTheFollowersByDefaultAndCanBeSharedWithNobody() {
        assertThat(service.myAccount(lucia).person().feedAudience()).isEqualTo(FeedAudience.FOLLOWERS);
        social.save(new Person(ana.userId(), "Ana", Visibility.PRIVATE, FeedAudience.FOLLOWERS));
        service.follow(ana, lucia.userId());
        service.accept(lucia.userId(), ana.userId());
        feed.forgotten.clear();

        var account = service.changeFeedAudience(lucia, FeedAudience.NOBODY);

        assertThat(account.person().feedAudience()).isEqualTo(FeedAudience.NOBODY);
        assertThat(social.people.get(lucia.userId()).sharesActivity()).isFalse();
        // Her followers see the change at once
        assertThat(feed.forgotten).containsExactly(ana.userId());
        assertThatThrownBy(() -> service.changeFeedAudience(lucia, null))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("code", "person.feedAudience");
    }

    @Test
    void forSomeoneIBlockedMyProfileDoesNotExistAndTheyCannotFollowMe() {
        service.myAccount(lucia);
        social.save(new Person(ana.userId(), "Ana", Visibility.PUBLIC, FeedAudience.FOLLOWERS));
        safety.block(lucia.userId(), ana.userId(), CLOCK.instant());

        // Ana is not told (HU-033): for her, Lucía has no account
        assertThatThrownBy(() -> service.person(ana, lucia.userId()))
                .isInstanceOf(SocialException.class)
                .hasFieldOrPropertyWithValue("reason", SocialException.Reason.PERSON_NOT_FOUND);
        assertThatThrownBy(() -> service.follow(ana, lucia.userId()))
                .isInstanceOf(SocialException.class)
                .hasFieldOrPropertyWithValue("reason", SocialException.Reason.PERSON_NOT_FOUND);
        assertThatThrownBy(() -> service.followersOf(ana, lucia.userId(), FIRST))
                .hasFieldOrPropertyWithValue("reason", SocialException.Reason.PERSON_NOT_FOUND);
        assertThat(social.follows).isEmpty();
        // Lucía still sees Ana, as blocked, and not her plans although her profile is public
        var view = service.person(lucia, ana.userId());
        assertThat(view.relation()).isEqualTo(Relation.BLOCKED);
        assertThat(view.activityVisible()).isFalse();
        // She unblocks her before following her again
        assertThatThrownBy(() -> service.follow(lucia, ana.userId()))
                .isInstanceOf(SocialException.class)
                .hasFieldOrPropertyWithValue("reason", SocialException.Reason.BLOCKED);
        safety.unblock(lucia.userId(), ana.userId());
        assertThat(service.follow(lucia, ana.userId()).status()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(service.person(ana, lucia.userId()).followsMe()).isTrue();
    }
}

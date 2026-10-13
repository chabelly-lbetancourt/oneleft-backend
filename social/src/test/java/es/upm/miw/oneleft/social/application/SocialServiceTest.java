package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.InMemorySocial;
import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.Connection;
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
    private final List<UUID> asked = new ArrayList<>();
    private final SocialService service = new SocialService(social, social, userId -> {
        asked.add(userId);
        if (userId.equals(ana.userId())) {
            return "Ana";
        }
        throw new SocialException(SocialException.Reason.PERSON_NOT_FOUND, "Unknown");
    }, CLOCK);

    @Test
    void aNewPersonIsPrivateWithTheNameOfTheirAccount() {
        var account = service.myAccount(lucia);

        assertThat(account.person()).isEqualTo(new Person(lucia.userId(), "Lucía", Visibility.PRIVATE));
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
}

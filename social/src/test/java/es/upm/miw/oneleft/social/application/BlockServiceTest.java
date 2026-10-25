package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.InMemoryFeed;
import es.upm.miw.oneleft.social.InMemorySafety;
import es.upm.miw.oneleft.social.InMemorySocial;
import es.upm.miw.oneleft.social.domain.model.BlockedPerson;
import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.FeedAudience;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.Person;
import es.upm.miw.oneleft.social.domain.model.SocialException;
import es.upm.miw.oneleft.social.domain.model.ValidationException;
import es.upm.miw.oneleft.social.domain.model.Visibility;
import es.upm.miw.oneleft.social.domain.port.out.Directory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-033: blocking ends the follows in both directions, privately, and is announced to the other services. */
class BlockServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T18:00:00Z"), ZoneOffset.UTC);
    private static final PageQuery FIRST = new PageQuery(0, 20);
    private final Caller lucia = new Caller(UUID.randomUUID(), "Lucía");
    private final Caller ana = new Caller(UUID.randomUUID(), "Ana");
    private final UUID nobody = UUID.randomUUID();
    private final InMemorySocial social = new InMemorySocial();
    private final InMemoryFeed feed = new InMemoryFeed(social);
    private final InMemorySafety safety = new InMemorySafety(social);
    private final Directory directory = userId -> {
        if (userId.equals(ana.userId())) {
            return "Ana";
        }
        throw new SocialException(SocialException.Reason.PERSON_NOT_FOUND, "Unknown");
    };
    private final SocialService follows = new SocialService(social, social, safety, directory, feed, CLOCK);
    private final BlockService service = new BlockService(social, directory, social, safety, feed, safety, CLOCK);

    @Test
    void blockingEndsTheFollowsAndTheRequestsInBothDirections() {
        social.save(new Person(ana.userId(), "Ana", Visibility.PUBLIC, FeedAudience.FOLLOWERS));
        assertThat(follows.follow(lucia, ana.userId()).status()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(follows.follow(ana, lucia.userId()).status()).isEqualTo(FollowStatus.PENDING);
        feed.forgotten.clear();

        service.block(lucia, ana.userId());

        assertThat(social.follows).isEmpty();
        // Neither feed shows the other one any more (HU-051)
        assertThat(feed.forgotten).containsExactlyInAnyOrder(lucia.userId(), ana.userId());
        assertThat(safety.events).containsExactly("user.blocked " + lucia.userId() + ">" + ana.userId());
        assertThat(service.blocked(lucia.userId(), FIRST).items())
                .containsExactly(new BlockedPerson(ana.userId(), "Ana", CLOCK.instant()));
        // Blocking again changes nothing and is not announced again
        service.block(lucia, ana.userId());
        assertThat(safety.events).hasSize(1);
        assertThat(service.blocked(ana.userId(), FIRST).total()).isZero();
    }

    @Test
    void someoneNewIsLookedUpOnceAndOnlyPeopleWithAnAccountAreBlocked() {
        service.block(lucia, ana.userId());

        // Ana was not known: her name comes from the users service, and Lucía is registered too
        assertThat(social.people.get(ana.userId()).name()).isEqualTo("Ana");
        assertThat(social.people).containsKey(lucia.userId());
        assertThatThrownBy(() -> service.block(lucia, nobody))
                .isInstanceOf(SocialException.class)
                .hasFieldOrPropertyWithValue("reason", SocialException.Reason.PERSON_NOT_FOUND);
        assertThatThrownBy(() -> service.block(lucia, lucia.userId()))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("code", "block.self");
        assertThat(safety.blocks).hasSize(1);
    }

    @Test
    void unblockingIsAnnouncedOnce() {
        service.block(lucia, ana.userId());
        safety.events.clear();

        service.unblock(lucia.userId(), ana.userId());
        service.unblock(lucia.userId(), ana.userId());

        assertThat(safety.events).containsExactly("user.unblocked " + lucia.userId() + ">" + ana.userId());
        assertThat(service.blocked(lucia.userId(), FIRST).items()).isEmpty();
    }
}

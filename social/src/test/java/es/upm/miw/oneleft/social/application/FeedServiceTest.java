package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.InMemoryFeed;
import es.upm.miw.oneleft.social.InMemorySocial;
import es.upm.miw.oneleft.social.domain.model.Activity;
import es.upm.miw.oneleft.social.domain.model.ActivityType;
import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.Follow;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.Person;
import es.upm.miw.oneleft.social.domain.model.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** HU-051: the feed of each person, computed when it is read, with its cache and its real-time notices. */
class FeedServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");
    private static final PageQuery FIRST = new PageQuery(0, PageQuery.DEFAULT_SIZE);
    private final UUID lucia = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();
    private final UUID plan = UUID.randomUUID();
    private final InMemorySocial social = new InMemorySocial();
    private final InMemoryFeed feed = new InMemoryFeed(social);
    private final FeedService service = new FeedService(feed, social, social, feed, feed);

    private static Activity activity(UUID actor, ActivityType type, UUID plan, int minutesAgo) {
        return new Activity(actor, type, plan, "Padel at the club", "PADEL", NOW.plusSeconds(3600),
                NOW.minusSeconds(60L * minutesAgo));
    }

    private void follow(UUID follower, UUID followee, FollowStatus status) {
        social.save(new Follow(follower, followee, status, NOW, status == FollowStatus.ACCEPTED ? NOW : null));
    }

    @BeforeEach
    void people() {
        social.save(new Person(lucia, "Lucía", Visibility.PRIVATE));
        social.save(new Person(ana, "Ana", Visibility.PRIVATE));
        social.save(new Person(diego, "Diego", Visibility.PUBLIC));
    }

    @Test
    void theFeedShowsWhatThePeopleFollowedDidTheLatestFirst() {
        follow(lucia, ana, FollowStatus.ACCEPTED);
        follow(lucia, diego, FollowStatus.ACCEPTED);
        service.record(activity(ana, ActivityType.PUBLISHED, plan, 30));
        service.record(activity(diego, ActivityType.JOINED, plan, 10));
        service.record(activity(lucia, ActivityType.JOINED, plan, 5));

        var page = service.feed(lucia, FIRST);

        assertThat(page.items()).extracting(FeedItem::actorName, FeedItem::type)
                .containsExactly(tuple("Diego", ActivityType.JOINED),
                        tuple("Ana", ActivityType.PUBLISHED));
        assertThat(page.total()).isEqualTo(2);
    }

    @Test
    void aPrivateProfileShowsUpOnlyOnceItAcceptsTheFollower() {
        follow(lucia, ana, FollowStatus.PENDING);
        service.record(activity(ana, ActivityType.PUBLISHED, plan, 30));

        assertThat(service.feed(lucia, FIRST).items()).isEmpty();
        assertThat(feed.notices).isEmpty();

        follow(lucia, ana, FollowStatus.ACCEPTED);
        feed.cached.clear();
        assertThat(service.feed(lucia, FIRST).items()).extracting(FeedItem::actorName).containsExactly("Ana");
    }

    @Test
    void newActivityIsToldToEachAcceptedFollowerOnce() {
        follow(lucia, ana, FollowStatus.ACCEPTED);
        follow(diego, ana, FollowStatus.ACCEPTED);
        var published = activity(ana, ActivityType.PUBLISHED, plan, 0);

        service.record(published);
        // The same event again changes nothing and is not told twice
        service.record(published);

        assertThat(feed.activities).hasSize(1);
        assertThat(feed.notices).extracting(InMemoryFeed.Notice::follower).containsExactlyInAnyOrder(lucia, diego);
        assertThat(feed.notices).extracting(notice -> notice.item())
                .containsOnly(FeedItem.of(published, "Ana"));
    }

    @Test
    void theActivityOfSomeoneUnknownIsKeptForWhoeverFollowsThemLater() {
        var stranger = UUID.randomUUID();

        service.record(activity(stranger, ActivityType.JOINED, plan, 0));

        assertThat(feed.activities).hasSize(1);
        assertThat(feed.notices).isEmpty();
    }

    @Test
    void theFirstPageIsKeptUntilWhatItShowsChanges() {
        follow(lucia, ana, FollowStatus.ACCEPTED);
        service.record(activity(ana, ActivityType.PUBLISHED, plan, 30));

        var first = service.feed(lucia, FIRST);
        assertThat(feed.cached).containsEntry(lucia, first);
        // Other pages and sizes are read every time
        service.feed(lucia, new PageQuery(1, PageQuery.DEFAULT_SIZE));
        service.feed(diego, new PageQuery(0, 5));
        assertThat(feed.cached).containsOnlyKeys(lucia);

        service.record(activity(ana, ActivityType.COMPLETED, plan, 0));

        assertThat(feed.cached).isEmpty();
        assertThat(service.feed(lucia, FIRST).items()).extracting(FeedItem::type)
                .containsExactly(ActivityType.COMPLETED, ActivityType.PUBLISHED);
    }

    @Test
    void whoeverLeavesAPlanIsNoLongerShownInIt() {
        follow(lucia, ana, FollowStatus.ACCEPTED);
        service.record(activity(ana, ActivityType.JOINED, plan, 10));
        service.feed(lucia, FIRST);
        feed.forgotten.clear();

        service.left(plan, ana);
        // Leaving a plan one had not joined changes nothing
        service.left(UUID.randomUUID(), ana);

        assertThat(feed.cached).isEmpty();
        assertThat(feed.forgotten).containsExactly(lucia);
        assertThat(service.feed(lucia, FIRST).items()).isEmpty();
    }

    @Test
    void aCancelledPlanLeavesEveryFeed() {
        var other = UUID.randomUUID();
        follow(lucia, ana, FollowStatus.ACCEPTED);
        follow(lucia, diego, FollowStatus.ACCEPTED);
        service.record(activity(ana, ActivityType.PUBLISHED, plan, 30));
        service.record(activity(diego, ActivityType.JOINED, plan, 20));
        service.record(activity(diego, ActivityType.JOINED, other, 10));
        service.feed(lucia, FIRST);

        service.cancelled(plan);

        assertThat(feed.cached).isEmpty();
        assertThat(service.feed(lucia, FIRST).items()).extracting(FeedItem::planId).containsExactly(other);
    }
}

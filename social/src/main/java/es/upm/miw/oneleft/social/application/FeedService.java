package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.domain.model.Activity;
import es.upm.miw.oneleft.social.domain.model.ActivityType;
import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.port.in.FeedUseCase;
import es.upm.miw.oneleft.social.domain.port.in.RecordActivityUseCase;
import es.upm.miw.oneleft.social.domain.port.out.Activities;
import es.upm.miw.oneleft.social.domain.port.out.FeedCache;
import es.upm.miw.oneleft.social.domain.port.out.FeedNotices;
import es.upm.miw.oneleft.social.domain.port.out.Follows;
import es.upm.miw.oneleft.social.domain.port.out.People;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The activity feed (HU-051) with fan-out on read: each activity is kept once, for whoever did it, and a feed is the
 * activity of the people followed, computed when it is read. Following or unfollowing someone shows at once, and an
 * activity costs one write whatever the number of followers. The first page of each feed is cached and forgotten when
 * it changes; the followers are told in real time.
 */
@Service
@Transactional
public class FeedService implements FeedUseCase, RecordActivityUseCase {

    private final Activities activities;
    private final Follows follows;
    private final People people;
    private final FeedCache cache;
    private final FeedNotices notices;

    public FeedService(Activities activities, Follows follows, People people, FeedCache cache, FeedNotices notices) {
        this.activities = activities;
        this.follows = follows;
        this.people = people;
        this.cache = cache;
        this.notices = notices;
    }

    @Override
    public void record(Activity activity) {
        if (!activities.save(activity)) {
            return;
        }
        var followers = follows.acceptedFollowerIds(activity.actorId());
        cache.forget(followers);
        // Someone the social service does not know has no followers yet
        people.find(activity.actorId())
                .ifPresent(actor -> notices.newActivity(followers, FeedItem.of(activity, actor.name())));
    }

    @Override
    public void left(UUID planId, UUID userId) {
        if (activities.delete(userId, ActivityType.JOINED, planId)) {
            cache.forget(follows.acceptedFollowerIds(userId));
        }
    }

    @Override
    public void cancelled(UUID planId) {
        activities.deletePlan(planId).forEach(actor -> cache.forget(follows.acceptedFollowerIds(actor)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FeedItem> feed(UUID me, PageQuery page) {
        if (page.page() != 0 || page.size() != PageQuery.DEFAULT_SIZE) {
            return activities.feedOf(me, page);
        }
        return cache.firstPage(me).orElseGet(() -> {
            var first = activities.feedOf(me, page);
            cache.keepFirstPage(me, first);
            return first;
        });
    }
}

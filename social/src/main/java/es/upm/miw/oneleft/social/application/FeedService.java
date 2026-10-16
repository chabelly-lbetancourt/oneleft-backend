package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.domain.model.Activity;
import es.upm.miw.oneleft.social.domain.model.ActivityType;
import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.port.in.FeedUseCase;
import es.upm.miw.oneleft.social.domain.port.in.HiddenPlansUseCase;
import es.upm.miw.oneleft.social.domain.port.in.RecordActivityUseCase;
import es.upm.miw.oneleft.social.domain.port.out.Activities;
import es.upm.miw.oneleft.social.domain.port.out.FeedCache;
import es.upm.miw.oneleft.social.domain.port.out.FeedNotices;
import es.upm.miw.oneleft.social.domain.port.out.Follows;
import es.upm.miw.oneleft.social.domain.port.out.HiddenPlans;
import es.upm.miw.oneleft.social.domain.port.out.People;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * The activity feed (HU-051) with fan-out on read: each activity is kept once, for whoever did it, and a feed is the
 * activity of the people followed, computed when it is read. Following or unfollowing someone shows at once, and an
 * activity costs one write whatever the number of followers. The first page of each feed is cached and forgotten when
 * it changes; the followers are told in real time. Privacy (HU-059): whoever shares their activity with nobody, or
 * hides a plan, keeps it out of the feeds when they are read, and nothing new of it is kept.
 */
@Service
@Transactional
public class FeedService implements FeedUseCase, RecordActivityUseCase, HiddenPlansUseCase {

    private final Activities activities;
    private final Follows follows;
    private final People people;
    private final HiddenPlans hiddenPlans;
    private final FeedCache cache;
    private final FeedNotices notices;
    private final Clock clock;

    public FeedService(Activities activities, Follows follows, People people, HiddenPlans hiddenPlans,
                       FeedCache cache, FeedNotices notices, Clock clock) {
        this.activities = activities;
        this.follows = follows;
        this.people = people;
        this.hiddenPlans = hiddenPlans;
        this.cache = cache;
        this.notices = notices;
        this.clock = clock;
    }

    @Override
    public void record(Activity activity) {
        var actor = people.find(activity.actorId());
        // What they share with nobody, or do in a plan they hid, is not even kept (HU-059)
        if (actor.filter(person -> !person.sharesActivity()).isPresent()
                || hiddenPlans.isHidden(activity.actorId(), activity.planId()) || !activities.save(activity)) {
            return;
        }
        var followers = follows.acceptedFollowerIds(activity.actorId());
        cache.forget(followers);
        // Someone the social service does not know has no followers yet
        actor.ifPresent(person -> notices.newActivity(followers, FeedItem.of(activity, person.name())));
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
    public void hidePlan(UUID me, UUID planId) {
        if (hiddenPlans.hide(me, planId, clock.instant())) {
            cache.forget(follows.acceptedFollowerIds(me));
        }
    }

    @Override
    public void showPlan(UUID me, UUID planId) {
        if (hiddenPlans.show(me, planId)) {
            cache.forget(follows.acceptedFollowerIds(me));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isHidden(UUID me, UUID planId) {
        return hiddenPlans.isHidden(me, planId);
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

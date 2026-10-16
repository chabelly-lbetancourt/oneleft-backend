package es.upm.miw.oneleft.social;

import es.upm.miw.oneleft.social.domain.model.Activity;
import es.upm.miw.oneleft.social.domain.model.ActivityType;
import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.port.out.Activities;
import es.upm.miw.oneleft.social.domain.port.out.FeedCache;
import es.upm.miw.oneleft.social.domain.port.out.FeedNotices;
import es.upm.miw.oneleft.social.domain.port.out.HiddenPlans;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The activity, the hidden plans, the cache of the feeds and their notices in memory, with the same rules as the SQL
 * query (tested in FeedIntegrationTest): only the people followed and accepted who share their activity, without the
 * plans they hid, the latest first.
 */
public class InMemoryFeed implements Activities, HiddenPlans, FeedCache, FeedNotices {

    public final List<Activity> activities = new ArrayList<>();
    public final Map<UUID, PageResult<FeedItem>> cached = new HashMap<>();
    public final List<UUID> forgotten = new ArrayList<>();
    public final List<Notice> notices = new ArrayList<>();
    public final Set<String> hidden = new HashSet<>();
    private final InMemorySocial social;

    public InMemoryFeed(InMemorySocial social) {
        this.social = social;
    }

    private static boolean same(Activity activity, UUID actorId, ActivityType type, UUID planId) {
        return activity.actorId().equals(actorId) && activity.type() == type && activity.planId().equals(planId);
    }

    @Override
    public boolean save(Activity activity) {
        if (activities.stream().anyMatch(a -> same(a, activity.actorId(), activity.type(), activity.planId()))) {
            return false;
        }
        return activities.add(activity);
    }

    @Override
    public boolean delete(UUID actorId, ActivityType type, UUID planId) {
        return activities.removeIf(activity -> same(activity, actorId, type, planId));
    }

    @Override
    public List<UUID> deletePlan(UUID planId) {
        var actors = activities.stream().filter(a -> a.planId().equals(planId)).map(Activity::actorId).distinct()
                .toList();
        activities.removeIf(activity -> activity.planId().equals(planId));
        return actors;
    }

    @Override
    public PageResult<FeedItem> feedOf(UUID viewer, PageQuery page) {
        var followed = social.follows.values().stream()
                .filter(follow -> follow.followerId().equals(viewer) && !follow.isPending())
                .map(follow -> follow.followeeId()).toList();
        List<FeedItem> all = activities.stream()
                .filter(activity -> followed.contains(activity.actorId())
                        && social.people.containsKey(activity.actorId())
                        && social.people.get(activity.actorId()).sharesActivity()
                        && !isHidden(activity.actorId(), activity.planId()))
                .sorted(Comparator.comparing(Activity::occurredAt).reversed())
                .map(activity -> FeedItem.of(activity, social.people.get(activity.actorId()).name()))
                .toList();
        var from = Math.min(all.size(), page.page() * page.size());
        return new PageResult<>(all.subList(from, Math.min(all.size(), from + page.size())), page.page(), page.size(),
                all.size());
    }

    @Override
    public boolean hide(UUID userId, UUID planId, Instant at) {
        return hidden.add(userId + ">" + planId);
    }

    @Override
    public boolean show(UUID userId, UUID planId) {
        return hidden.remove(userId + ">" + planId);
    }

    @Override
    public boolean isHidden(UUID userId, UUID planId) {
        return hidden.contains(userId + ">" + planId);
    }

    @Override
    public Optional<PageResult<FeedItem>> firstPage(UUID viewer) {
        return Optional.ofNullable(cached.get(viewer));
    }

    @Override
    public void keepFirstPage(UUID viewer, PageResult<FeedItem> page) {
        cached.put(viewer, page);
    }

    @Override
    public void forget(Collection<UUID> viewers) {
        forgotten.addAll(viewers);
        viewers.forEach(cached::remove);
    }

    @Override
    public void newActivity(Collection<UUID> followers, FeedItem item) {
        followers.forEach(follower -> notices.add(new Notice(follower, item)));
    }

    public record Notice(UUID follower, FeedItem item) {
    }
}

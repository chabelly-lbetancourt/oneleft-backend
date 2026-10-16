package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.Activity;
import es.upm.miw.oneleft.social.domain.model.ActivityType;
import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.port.out.Activities;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Output adapter: the activity of each person in PostgreSQL (HU-051). */
@Repository
public class JpaActivityRepository implements Activities {

    private final SpringDataActivityRepository activities;

    public JpaActivityRepository(SpringDataActivityRepository activities) {
        this.activities = activities;
    }

    @Override
    public boolean save(Activity activity) {
        return activities.insert(activity.actorId(), activity.type().name(), activity.planId(), activity.planTitle(),
                activity.planActivity(), activity.startsAt(), activity.occurredAt()) == 1;
    }

    @Override
    public boolean delete(UUID actorId, ActivityType type, UUID planId) {
        return activities.delete(actorId, type, planId) > 0;
    }

    @Override
    public List<UUID> deletePlan(UUID planId) {
        var actors = activities.actorsOf(planId);
        activities.deletePlan(planId);
        return actors;
    }

    @Override
    public PageResult<FeedItem> feedOf(UUID viewer, PageQuery page) {
        var rows = activities.feedOf(viewer, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(rows.map(FeedRow::toDomain).getContent(), page.page(), page.size(),
                rows.getTotalElements());
    }
}

package es.upm.miw.oneleft.social.domain.port.out;

import es.upm.miw.oneleft.social.domain.model.Activity;
import es.upm.miw.oneleft.social.domain.model.ActivityType;
import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;

import java.util.List;
import java.util.UUID;

/** What each person did in the plans (HU-051). */
public interface Activities {

    /** @return whether it is new: once per person, kind and plan */
    boolean save(Activity activity);

    /** @return whether there was such an activity */
    boolean delete(UUID actorId, ActivityType type, UUID planId);

    /** @return who had activity in the plan */
    List<UUID> deletePlan(UUID planId);

    /**
     * The feed of {@code viewer} computed when it is read (fan-out on read): the activity of the people they follow,
     * accepted, with their names, the latest first, in one query.
     */
    PageResult<FeedItem> feedOf(UUID viewer, PageQuery page);
}

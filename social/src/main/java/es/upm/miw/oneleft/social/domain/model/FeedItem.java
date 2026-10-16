package es.upm.miw.oneleft.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/** An activity in a feed, with the name of whoever did it. */
public record FeedItem(UUID actorId, String actorName, ActivityType type, UUID planId, String planTitle,
                       String planActivity, Instant startsAt, Instant occurredAt) {

    public static FeedItem of(Activity activity, String actorName) {
        return new FeedItem(activity.actorId(), actorName, activity.type(), activity.planId(), activity.planTitle(),
                activity.planActivity(), activity.startsAt(), activity.occurredAt());
    }
}

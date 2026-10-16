package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.ActivityType;
import es.upm.miw.oneleft.social.domain.model.FeedItem;

import java.time.Instant;
import java.util.UUID;

/** A row of a feed: the activity and the name of whoever did it, read in the same query. */
public record FeedRow(UUID actorId, String actorName, ActivityType type, UUID planId, String planTitle,
                      String planActivity, Instant startsAt, Instant occurredAt) {

    FeedItem toDomain() {
        return new FeedItem(actorId, actorName, type, planId, planTitle, planActivity, startsAt, occurredAt);
    }
}

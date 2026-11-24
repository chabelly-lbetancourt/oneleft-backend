package es.upm.miw.oneleft.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * What the level of someone is worth in this service (HU-063), as the reputation service says: how much more their
 * notices weigh and how many alerts they may keep. The numbers come from there; here they are only applied.
 *
 * @param level      the name of the level, only to explain the copy
 * @param occurredAt when the reputation service said it
 */
public record MemberPerks(UUID userId, String level, double noticeWeight, int maxSavedAlerts, Instant occurredAt) {

    /** The weight of someone whose perks are not known: the one of a new person */
    public static final double DEFAULT_WEIGHT = 1.0;
}

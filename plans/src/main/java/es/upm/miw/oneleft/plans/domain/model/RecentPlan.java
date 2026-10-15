package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A plan someone took part in, for their social profile (HU-048): what and when, never where.
 *
 * @param organizer whether they organized it; otherwise they arrived at it (check-in)
 */
public record RecentPlan(UUID planId, String title, Activity activity, Instant startsAt, boolean organizer) {
}

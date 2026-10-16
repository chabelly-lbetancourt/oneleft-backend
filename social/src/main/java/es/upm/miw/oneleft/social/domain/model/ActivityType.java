package es.upm.miw.oneleft.social.domain.model;

/** What someone did in a plan, for the feed of their followers (HU-051). */
public enum ActivityType {
    /** Published a plan: an invitation to join it */
    PUBLISHED,
    /** Took a spot in a plan */
    JOINED,
    /** Arrived at a plan that was held (check-in) */
    COMPLETED
}

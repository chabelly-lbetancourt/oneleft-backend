package es.upm.miw.oneleft.social.domain.model;

/** How many people follow someone and how many they follow, and the requests waiting on each side. */
public record FollowCounts(long followers, long following, long requests, long sentRequests) {

    public static final FollowCounts NONE = new FollowCounts(0, 0, 0, 0);
}

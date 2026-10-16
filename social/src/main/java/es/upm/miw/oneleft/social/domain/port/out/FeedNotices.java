package es.upm.miw.oneleft.social.domain.port.out;

import es.upm.miw.oneleft.social.domain.model.FeedItem;

import java.util.Collection;
import java.util.UUID;

/** Tells each follower in real time that there is something new in their feed (HU-051), once it is saved. */
public interface FeedNotices {

    void newActivity(Collection<UUID> followers, FeedItem item);
}

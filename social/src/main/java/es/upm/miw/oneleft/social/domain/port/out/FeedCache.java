package es.upm.miw.oneleft.social.domain.port.out;

import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.PageResult;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * The first page of each feed, the one opened most, kept for a while (HU-051). It is thrown away when what it shows
 * changes; if the cache does not answer, the feed is computed as if it were empty.
 */
public interface FeedCache {

    Optional<PageResult<FeedItem>> firstPage(UUID viewer);

    void keepFirstPage(UUID viewer, PageResult<FeedItem> page);

    /** Once the change is saved, the feeds of these people are computed again */
    void forget(Collection<UUID> viewers);
}

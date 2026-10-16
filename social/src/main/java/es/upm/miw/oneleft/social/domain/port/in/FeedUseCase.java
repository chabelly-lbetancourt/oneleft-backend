package es.upm.miw.oneleft.social.domain.port.in;

import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;

import java.util.UUID;

/** My feed (HU-051): what the people I follow did, the latest first. */
public interface FeedUseCase {

    PageResult<FeedItem> feed(UUID me, PageQuery page);
}

package es.upm.miw.oneleft.social.domain.port.in;

import es.upm.miw.oneleft.social.domain.model.BlockedPerson;
import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;

import java.util.UUID;

/**
 * Blocking someone (HU-033): we stop following each other, neither of us sees the plans of the other one, and they
 * cannot join mine. It is private: they are never told, and for them my profile does not exist.
 */
public interface BlockUseCase {

    void block(Caller me, UUID userId);

    void unblock(UUID me, UUID userId);

    PageResult<BlockedPerson> blocked(UUID me, PageQuery page);
}

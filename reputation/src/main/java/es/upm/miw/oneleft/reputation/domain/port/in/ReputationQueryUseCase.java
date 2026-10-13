package es.upm.miw.oneleft.reputation.domain.port.in;

import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;
import es.upm.miw.oneleft.reputation.domain.model.Reputation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** The reputation of people and the entries that explain it. */
public interface ReputationQueryUseCase {

    /** At most this many people in one question */
    int MAX_PEOPLE = 50;

    Reputation reputationOf(UUID userId);

    /** The reputation of several people at once (lists of plans), in the order asked. */
    List<Reputation> reputationsOf(Collection<UUID> userIds);

    /** Why the reputation of someone went up or down, the latest first. */
    PageResult<LedgerEntry> ledgerOf(UUID userId, PageQuery page);
}

package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;

import java.util.Optional;
import java.util.UUID;

/** The immutable ledger of reputation: entries are only added. */
public interface Ledger {

    /** @return false if the same entry (person, reason and source) was already there */
    boolean append(LedgerEntry entry);

    PageResult<LedgerEntry> entriesOf(UUID userId, PageQuery page);

    /** The title of a plan, as its entries keep it */
    Optional<String> planTitle(UUID planId);
}

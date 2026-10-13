package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;

import java.util.UUID;

/** The immutable ledger of reputation: entries are only added. */
public interface Ledger {

    /** @return false if the same entry (person, reason and source) was already there */
    boolean append(LedgerEntry entry);

    PageResult<LedgerEntry> entriesOf(UUID userId, PageQuery page);
}

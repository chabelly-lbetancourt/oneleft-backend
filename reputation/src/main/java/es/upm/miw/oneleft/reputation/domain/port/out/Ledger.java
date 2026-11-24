package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;
import es.upm.miw.oneleft.reputation.domain.model.Reason;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The immutable ledger of reputation: entries are only added. */
public interface Ledger {

    /** @return false if the same entry (person, reason and source) was already there */
    boolean append(LedgerEntry entry);

    PageResult<LedgerEntry> entriesOf(UUID userId, PageQuery page);

    /** The entries of a person and reason from an instant (included) to another (not included), for the caps */
    int count(UUID userId, Reason reason, Instant from, Instant to);
    /** When the entries of a person and reason happened, from an instant on: the weeks of a streak (HU-062) */
    List<Instant> occurrencesOf(UUID userId, Reason reason, Instant since);

    /** The title of a plan, as its entries keep it */
    Optional<String> planTitle(UUID planId);
}

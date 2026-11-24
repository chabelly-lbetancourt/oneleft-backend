package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.model.AntiCheatRules;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.port.out.Ledger;

/**
 * The daily cap of each reason (HU-066), with the entries the ledger already has that day. Only entries that add
 * points and whose reason has a cap ask the ledger.
 */
final class DailyCaps {

    private final Ledger ledger;

    DailyCaps(Ledger ledger) {
        this.ledger = ledger;
    }

    LedgerEntry apply(LedgerEntry entry) {
        if (!AntiCheatRules.capped(entry)) {
            return entry;
        }
        var earlier = ledger.count(entry.userId(), entry.reason(), AntiCheatRules.startOfDay(entry.occurredAt()),
                AntiCheatRules.endOfDay(entry.occurredAt()));
        return AntiCheatRules.dailyCapped(entry, earlier);
    }
}

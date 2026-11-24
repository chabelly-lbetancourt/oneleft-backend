package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.Perks;

import java.time.Instant;
import java.util.UUID;

/** Tells the other services what the level of someone is worth now (HU-063), so they keep their copy. */
public interface PerkNotices {

    /** Tells nobody: where there is nobody to tell */
    PerkNotices NONE = (userId, perks, at) -> {
    };

    void changed(UUID userId, Perks perks, Instant at);
}

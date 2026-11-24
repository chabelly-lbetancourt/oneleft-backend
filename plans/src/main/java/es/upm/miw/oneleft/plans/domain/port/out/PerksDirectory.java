package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.ReputationPerks;

import java.util.UUID;

/** What the reputation of a person is worth, from the reputation service (HU-063). */
public interface PerksDirectory {

    /** Nobody has perks: where there is no reputation service */
    PerksDirectory NONE = userId -> ReputationPerks.BASE;

    /** @return the perks of the person, or {@link ReputationPerks#BASE} if they cannot be known now; never an error */
    ReputationPerks perksOf(UUID userId);
}

package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.TrustedLink;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The links for trusted contacts (HU-034): whose each one is and the hash of its secret. */
public interface TrustedLinkRepository {

    /**
     * Makes whoever creates a link for this plan wait for their turn until the end of the transaction, so that two
     * requests at once cannot both take the last one of the limit.
     */
    void lockPlan(UUID planId);

    TrustedLink save(TrustedLink link);

    Optional<TrustedLink> findByTokenHash(String tokenHash);

    /** The links of a person for a plan that have not expired, oldest first. */
    List<TrustedLink> findActive(UUID planId, UUID userId, Instant now);

    /** Deletes a link only if that person created it for that plan; nothing happens otherwise. */
    void delete(UUID linkId, UUID planId, UUID userId);

    /** Deletes the links that expired and returns how many there were. */
    int deleteExpired(Instant now);
}

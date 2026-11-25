package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A link someone of a plan gives to a person they trust (HU-034), so that they know where, when and with whom they
 * meet. It is read-only, needs no account and stops working when the plan is over.
 *
 * @param userId    who created it: only they see it in their list and revoke it
 * @param tokenHash the SHA-256 of its secret ({@link TrustedLinkToken})
 * @param expiresAt when the plan is over
 */
public record TrustedLink(UUID id, UUID planId, UUID userId, String tokenHash, Instant createdAt, Instant expiresAt) {

    /** Links a person can have at once for a plan: one for each person they want to tell. */
    public static final int MAX_PER_PERSON = 5;

    public TrustedLink {
        if (id == null || planId == null || userId == null || tokenHash == null || createdAt == null
                || expiresAt == null) {
            throw new ValidationException("trustedLink.missingData", "Required data of the link is missing");
        }
    }

    /** A new link for a plan, which expires when the plan is over. */
    public static TrustedLink of(Plan plan, UUID userId, String token, Instant now) {
        return new TrustedLink(UUID.randomUUID(), plan.id(), userId, TrustedLinkToken.hash(token), now, plan.endsAt());
    }

    public boolean isActive(Instant now) {
        return now.isBefore(expiresAt);
    }
}

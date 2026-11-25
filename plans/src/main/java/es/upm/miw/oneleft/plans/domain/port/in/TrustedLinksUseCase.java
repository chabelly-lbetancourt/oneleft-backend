package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.IssuedTrustedLink;
import es.upm.miw.oneleft.plans.domain.model.TrustedLink;
import es.upm.miw.oneleft.plans.domain.model.TrustedPlanView;

import java.util.List;
import java.util.UUID;

/**
 * Sharing a plan with a trusted contact (HU-034): whoever goes to a plan creates a link and sends it, on their own,
 * to someone they trust. The contact sees where, when and with whom they meet, without an account, until the plan is
 * over or the link is revoked.
 */
public interface TrustedLinksUseCase {

    /**
     * Creates a link for a plan the person goes to. Its secret is only known now.
     *
     * @throws es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException    if there is no such plan
     * @throws es.upm.miw.oneleft.plans.domain.model.MemoryForbiddenException if the person is neither its organizer
     *                                                                        nor a participant
     * @throws es.upm.miw.oneleft.plans.domain.model.JoinRejectedException    if the plan is over or cancelled, or the
     *                                                                        person already has as many links as
     *                                                                        allowed
     */
    IssuedTrustedLink create(UUID planId, UUID userId);

    /** The links of the person for a plan that still work, oldest first; without their secrets. */
    List<TrustedLink> mine(UUID planId, UUID userId);

    /** Revokes a link of the person: it stops working at once. Revoking it again, or one of somebody else, does nothing. */
    void revoke(UUID planId, UUID userId, UUID linkId);

    /**
     * The plan as the contact sees it.
     *
     * @throws es.upm.miw.oneleft.plans.domain.model.TrustedLinkNotFoundException if the link does not exist, expired,
     *                                                                            was revoked or its creator is no
     *                                                                            longer in the plan: always the same
     */
    TrustedPlanView open(String token);

    /** Removes the links that expired and returns how many there were. */
    int purgeExpired();
}

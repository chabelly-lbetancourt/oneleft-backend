package es.upm.miw.oneleft.plans.domain.port.in;

import java.util.UUID;

/**
 * The organizer cancels their own plan before it starts. Everyone who had joined or was waiting is told, and the plan
 * no longer counts among the plans the organizer has at once (HU-063).
 *
 * @see es.upm.miw.oneleft.plans.domain.model.NotOrganizerException when someone else tries
 * @see es.upm.miw.oneleft.plans.domain.model.JoinRejectedException {@code plan.started} once the plan has started
 */
public interface CancelPlanUseCase {

    /** Cancelling a plan that is already cancelled does nothing. */
    void cancel(UUID planId, UUID organizerId);

    /**
     * The organizer deleted their account (HU-045): every plan of theirs that has not started is cancelled and
     * everyone in it is told, with the reason {@code ACCOUNT_DELETED}. Repeating it cancels and tells nothing more.
     */
    void cancelPlansOfDeletedAccount(UUID organizerId);
}

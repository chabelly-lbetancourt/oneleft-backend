package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.util.Optional;
import java.util.UUID;

/** Whether a person fits the audience of a plan before they join it, join its waiting list or ask for a spot (HU-070). */
public interface AudienceCheck {

    /** Checks nothing: for the plans of everyone, and where there is no users service. */
    AudienceCheck NONE = (plan, userId) -> {
    };

    /** @throws es.upm.miw.oneleft.plans.domain.model.JoinRejectedException if the person does not fit */
    void check(Plan plan, UUID userId);

    /** The age band of the person for the organizer to see, if known; never an error: it is only shown (HU-070). */
    default Optional<String> ageBandOf(UUID userId) {
        return Optional.empty();
    }
}

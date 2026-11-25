package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Compatibility;
import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.util.Optional;
import java.util.UUID;

/** How well a plan fits a person (HU-031), from the hobbies of their profile. */
public interface CompatibilityUseCase {

    /**
     * Empty for the organizer of the plan, and if the profile of the person cannot be known now (they have not set
     * their date of birth and gender, or the users service does not answer): a compatibility never makes anything fail.
     */
    Optional<Compatibility> compatibility(Plan plan, UUID userId);
}

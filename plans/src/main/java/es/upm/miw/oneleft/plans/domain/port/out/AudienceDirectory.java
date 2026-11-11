package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.AudienceMember;

import java.util.Optional;
import java.util.UUID;

/** Gender and age of a person, from the users service (HU-070). */
public interface AudienceDirectory {

    /**
     * @return the person, or empty if they have not set their date of birth and gender
     * @throws es.upm.miw.oneleft.plans.domain.model.AudienceUnavailableException if it cannot be known now
     */
    Optional<AudienceMember> memberOf(UUID userId);
}

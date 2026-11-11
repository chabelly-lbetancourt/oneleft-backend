package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.AudienceMember;

import java.util.Optional;
import java.util.UUID;

/** Gender and age of the person who searches, to leave out the plans that are not for them (HU-070). */
public interface AudienceQueryUseCase {

    /** Empty if the person has not set them or the users service cannot say. */
    Optional<AudienceMember> viewer(UUID userId);
}

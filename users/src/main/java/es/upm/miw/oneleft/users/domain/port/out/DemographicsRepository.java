package es.upm.miw.oneleft.users.domain.port.out;

import es.upm.miw.oneleft.users.domain.model.Demographics;

import java.util.Optional;
import java.util.UUID;

/** Date of birth and gender (HU-070). */
public interface DemographicsRepository {

    Optional<Demographics> findByUser(UUID userId);

    Demographics save(Demographics demographics);
}

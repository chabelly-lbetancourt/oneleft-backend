package es.upm.miw.oneleft.users.domain.port.out;

import es.upm.miw.oneleft.users.domain.model.Demographics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Date of birth and gender (HU-070). */
public interface DemographicsRepository {

    Optional<Demographics> findByUser(UUID userId);

    Demographics save(Demographics demographics);

    /** Who has their birthday on that day; those born on 29 February turn the age on 1 March in common years. */
    List<Demographics> bornOn(LocalDate day);
}

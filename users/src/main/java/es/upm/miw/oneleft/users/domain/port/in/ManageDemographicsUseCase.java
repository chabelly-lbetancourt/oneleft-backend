package es.upm.miw.oneleft.users.domain.port.in;

import es.upm.miw.oneleft.users.domain.model.Demographics;
import es.upm.miw.oneleft.users.domain.model.Gender;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Date of birth and gender of the signed-in person (HU-070). */
public interface ManageDemographicsUseCase {

    Optional<Demographics> mine(UUID userId);

    /** Sets them the first time or changes them; the date of birth can only change a few times. */
    Demographics save(UUID userId, LocalDate birthDate, Gender gender);
}

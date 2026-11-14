package es.upm.miw.oneleft.users.domain.port.out;

import es.upm.miw.oneleft.users.domain.model.Gender;

import java.time.Instant;
import java.util.UUID;

/**
 * What the other services learn of the demographics of a person (HU-070): their gender and their age in years, never
 * the date of birth. The notices of nearby plans use it to leave out who the plan is not for.
 */
public interface DemographicsEvents {

    void changed(UUID userId, Gender gender, int age, Instant at);
}

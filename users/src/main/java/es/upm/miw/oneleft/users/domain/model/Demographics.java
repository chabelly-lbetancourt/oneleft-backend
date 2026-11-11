package es.upm.miw.oneleft.users.domain.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Date of birth and gender of a person (HU-070), needed to publish or join a plan: you must be of age and a plan can be
 * for a gender or an age range. The date is only used to work out the age and the band; it is never shown. Once set it
 * can only be changed {@link #MAX_BIRTH_DATE_CHANGES} times, to avoid abuse.
 *
 * @param changes how many times the date was changed after it was first set
 */
public record Demographics(UUID userId, LocalDate birthDate, Gender gender, int changes) {

    public static final int MIN_AGE = 18;
    public static final int MAX_BIRTH_DATE_CHANGES = 2;
    private static final int MAX_AGE = 120;

    public Demographics {
        if (userId == null || birthDate == null || gender == null) {
            throw new ValidationException("demographics.missing", "The date of birth and the gender are required");
        }
    }

    /** Demographics for the first time; the person must be of age and born at most 120 years ago. */
    public static Demographics first(UUID userId, LocalDate birthDate, Gender gender, Clock clock) {
        checkAge(birthDate, clock);
        return new Demographics(userId, birthDate, gender, 0);
    }

    /** The same person with new data; a different date counts as a change and they are limited. */
    public Demographics update(LocalDate newBirthDate, Gender newGender, Clock clock) {
        checkAge(newBirthDate, clock);
        if (newBirthDate == null || newBirthDate.equals(birthDate)) {
            return new Demographics(userId, birthDate, newGender, changes);
        }
        if (changes >= MAX_BIRTH_DATE_CHANGES) {
            throw new ValidationException("demographics.birthDateLocked",
                    "The date of birth can only be changed " + MAX_BIRTH_DATE_CHANGES + " times");
        }
        return new Demographics(userId, newBirthDate, newGender, changes + 1);
    }

    private static void checkAge(LocalDate birthDate, Clock clock) {
        if (birthDate == null) {
            throw new ValidationException("demographics.missing", "The date of birth and the gender are required");
        }
        var age = Period.between(birthDate, LocalDate.now(clock)).getYears();
        if (birthDate.isAfter(LocalDate.now(clock)) || age > MAX_AGE) {
            throw new ValidationException("demographics.birthDate", "The date of birth is not valid");
        }
        if (age < MIN_AGE) {
            throw new ValidationException("demographics.minor", "Only people of age can use OneLeft");
        }
    }

    public int age(Clock clock) {
        return Period.between(birthDate, LocalDate.now(clock)).getYears();
    }

    public AgeBand ageBand(Clock clock) {
        return AgeBand.of(age(clock));
    }
}
